package com.example.data.ai

import android.util.Log
import com.example.model.AppLanguage
import com.example.model.LocalizedContent
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

interface NewsTranslator {
    suspend fun translateArticle(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent?
}

class GeminiNewsTranslator(
    private val httpClient: OkHttpClient = defaultTranslationHttpClient()
) : NewsTranslator {

    private val firebaseCooldownUntilMs = AtomicLong(0L)
    private val consecutiveGeminiFailures = AtomicInteger(0)

    private val translationSchema = Schema.obj(
        mapOf(
            "title" to Schema.string("Idiomatic, publication-grade news headline in the target language using native journalistic phrasing."),
            "summary" to Schema.string("Rich, fluent, publication-grade news summary (up to 110 words) faithfully preserving every fact, attribution, quote, or statement without inventing any outside facts.")
        )
    )

    @Volatile
    private var cachedTranslationModelPair: Pair<String, com.google.firebase.ai.GenerativeModel>? = null

    private fun getGenerativeModel(): com.google.firebase.ai.GenerativeModel {
        val targetModelName = com.example.data.remote.RemoteConfigManager.config.value.aiModelName
        val current = cachedTranslationModelPair
        if (current != null && current.first == targetModelName) return current.second
        val created = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = targetModelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
                temperature = 0.15f
            },
            systemInstruction = content {
                text(
                    "You are a senior international news desk editor and translator. " +
                        "Translate ONLY the provided headline and story summary accurately and eloquently into the requested language. " +
                        "NEVER invent facts, names, dates, or quotes not present in the source text.\n" +
                        "Language quality rules:\n" +
                        "- For URDU (اردو): Write in fluent, authoritative Pakistani & international journalistic Urdu (شستہ، بامحاورہ صحافتی اردو، BBC Urdu / Dawn Urdu معیار). Use natural Urdu sentence structure, accurate transliteration of proper nouns, proper Urdu punctuation (۔ ، ؟), and idiomatic expressions instead of literal word-for-word translation.\n" +
                        "- For ARABIC (العربية): Write in eloquent, crisp Modern Standard Journalistic Arabic (العربية الفصحى الإعلامية الرصينة بأسلوب كبرى الوكالات الإخبارية), with natural collocations and clean Arabic punctuation (، ؛ ؟).\n" +
                        "- For TURKISH (Türkçe) and HINDI (हिन्दी): Write in natural broadcast-grade journalistic prose.\n" +
                        "Keep summaries thorough yet concise (up to 110 words), preserving speaker attributions, official statements, X/social quotes, and scholarly citations."
                )
            }
        )
        cachedTranslationModelPair = targetModelName to created
        return created
    }

    override suspend fun translateArticle(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent? = withContext(Dispatchers.IO) {
        if (targetLanguage == AppLanguage.ENGLISH) {
            return@withContext LocalizedContent(title = title, summary = summary)
        }

        val remoteCfg = com.example.data.remote.RemoteConfigManager.config.value
        val now = System.currentTimeMillis()
        if (remoteCfg.aiTranslationEnabled && now >= firebaseCooldownUntilMs.get()) {
            val geminiResult = tryTranslateWithGemini(title, summary, targetLanguage)
            if (geminiResult != null) {
                consecutiveGeminiFailures.set(0)
                return@withContext geminiResult
            }
            if (consecutiveGeminiFailures.incrementAndGet() >= 2) {
                firebaseCooldownUntilMs.set(now + FIREBASE_COOLDOWN_MS)
            }
        }

        // Emergency cloud endpoint fallback if Firebase AI is temporarily unreachable
        tryTranslateViaCloudEndpoint(title, summary, targetLanguage)
    }

    private suspend fun tryTranslateWithGemini(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent? {
        val targetGuidance = when (targetLanguage) {
            AppLanguage.URDU -> "Urdu (اردو script — شستہ، بامحاورہ صحافتی اردو معیار، درست املاء اور اردو علاماتِ اوقاف ۔ ، کے ساتھ)"
            AppLanguage.ARABIC -> "Arabic (العربية الفصحى الإعلامية الرصينة والواضحة بأسلوب الصحافة الاحترافية)"
            AppLanguage.TURKISH -> "Turkish (Türkçe — akıcı gazetecilik üslubu)"
            AppLanguage.HINDI -> "Hindi (हिन्दी — स्पष्ट और मानक पत्रकारिता शैली)"
            AppLanguage.ENGLISH -> "English"
        }

        val prompt = buildString {
            appendLine("Translate the following news report into $targetGuidance.")
            appendLine("Strict requirements:")
            appendLine("1. Translate ONLY the facts present below; do NOT add external claims.")
            appendLine("2. Use authentic journalistic syntax and natural idioms of $targetGuidance — never literal robot phrasing.")
            appendLine("3. Preserve all quoted statements, social/X remarks, analyst titles, or scholarly attributions faithfully.")
            appendLine("4. Return JSON with keys \"title\" and \"summary\" (up to 110 words).")
            appendLine()
            appendLine("Headline: $title")
            appendLine("Report: $summary")
        }

        return try {
            val response = withTimeout(40_000L) {
                getGenerativeModel().generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) return null

            val jsonObj = JSONObject(rawJson)
            val translatedTitle = postProcessLanguageText(jsonObj.optString("title").trim(), targetLanguage)
            val translatedSummary = postProcessLanguageText(jsonObj.optString("summary").trim(), targetLanguage)

            if (translatedTitle.isBlank() || translatedSummary.isBlank()) {
                null
            } else {
                LocalizedContent(
                    title = translatedTitle,
                    summary = translatedSummary
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI translation failed for ${targetLanguage.code}: ${e.message}", e)
            null
        }
    }

    private suspend fun tryTranslateViaCloudEndpoint(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent? = coroutineScope {
        try {
            val targetIso = when (targetLanguage) {
                AppLanguage.URDU -> "ur"
                AppLanguage.ARABIC -> "ar"
                AppLanguage.TURKISH -> "tr"
                AppLanguage.HINDI -> "hi"
                AppLanguage.ENGLISH -> "en"
            }

            val titleDeferred = async { fetchSingleCloudTranslation(title, targetIso) }
            val summaryDeferred = async {
                if (summary.isBlank() || summary == title) {
                    null
                } else {
                    fetchSingleCloudTranslation(summary, targetIso)
                }
            }

            val translatedTitle = postProcessLanguageText(titleDeferred.await()?.trim().orEmpty(), targetLanguage)
            if (translatedTitle.isBlank()) return@coroutineScope null

            val translatedSummary = summaryDeferred.await()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { postProcessLanguageText(it, targetLanguage) }
                ?: translatedTitle

            LocalizedContent(
                title = translatedTitle,
                summary = translatedSummary
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Cloud translation fallback failed", e)
            null
        }
    }

    private fun fetchSingleCloudTranslation(text: String, targetIso: String): String? {
        if (text.isBlank()) return null
        val url = CLOUD_TRANSLATE_BASE_URL.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("client", "gtx")
            ?.addQueryParameter("sl", "auto")
            ?.addQueryParameter("tl", targetIso)
            ?.addQueryParameter("dt", "t")
            ?.addQueryParameter("q", text)
            ?.build()
            ?: return null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            return parseCloudTranslationJson(body)
        }
    }

    companion object {
        private const val TAG = "NewsTranslator"
        private const val CLOUD_TRANSLATE_BASE_URL = "https://translate.googleapis.com/translate_a/single"
        private const val FIREBASE_COOLDOWN_MS = 60L * 1000L // 60 seconds retry window

        private val SHARED_TRANSLATION_CLIENT: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        fun defaultTranslationHttpClient(): OkHttpClient = SHARED_TRANSLATION_CLIENT

        fun postProcessLanguageText(text: String, lang: AppLanguage): String {
            if (text.isBlank()) return text
            return when (lang) {
                AppLanguage.URDU -> text
                    .replace(" ,", "،")
                    .replace(", ", "، ")
                    .replace("?", "؟")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                AppLanguage.ARABIC -> text
                    .replace(" ,", "،")
                    .replace(", ", "، ")
                    .replace("?", "؟")
                    .replace(";", "؛")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                else -> text.replace(Regex("\\s+"), " ").trim()
            }
        }

        fun parseCloudTranslationJson(jsonBody: String): String? {
            if (jsonBody.isBlank()) return null
            return try {
                val rootArray = JSONArray(jsonBody)
                val sentencesArray = rootArray.optJSONArray(0) ?: return null
                val sb = StringBuilder()
                for (i in 0 until sentencesArray.length()) {
                    val segment = sentencesArray.optJSONArray(i)
                    val piece = segment?.optString(0).orEmpty()
                    if (piece.isNotEmpty() && piece != "null") {
                        sb.append(piece)
                    }
                }
                sb.toString().trim().takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
