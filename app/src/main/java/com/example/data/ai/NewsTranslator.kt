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

    private val translationSchema = Schema.obj(
        mapOf(
            "title" to Schema.string("The faithfully translated news headline in the target language, without adding any facts."),
            "summary" to Schema.string("The faithfully translated concise news summary (maximum 60 words) in the target language, without adding any facts.")
        )
    )

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = translationSchema
                temperature = 0.1f
            },
            systemInstruction = content {
                text(
                    "You are an accurate news translator for an international news app. " +
                        "Translate and concisely summarize ONLY the given headline and summary into the requested language. " +
                        "NEVER invent, extrapolate, or add any facts, names, numbers, or context not present in the source text. " +
                        "Keep the summary under 60 words."
                )
            }
        )
    }

    override suspend fun translateArticle(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent? = withContext(Dispatchers.IO) {
        if (targetLanguage == AppLanguage.ENGLISH) {
            return@withContext LocalizedContent(title = title, summary = summary)
        }

        val now = System.currentTimeMillis()
        if (now >= firebaseCooldownUntilMs.get()) {
            val geminiResult = tryTranslateWithGemini(title, summary, targetLanguage)
            if (geminiResult != null) {
                return@withContext geminiResult
            }
            // Put Firebase AI on a short 5-minute cooldown if it failed (e.g., App Check or quota)
            // so remaining visible cards translate instantaneously via the cloud fallback
            firebaseCooldownUntilMs.set(now + FIREBASE_COOLDOWN_MS)
        }

        // Fast, reliable HTTPS translation fallback (works without API keys on any device/APK)
        tryTranslateViaCloudEndpoint(title, summary, targetLanguage)
    }

    private suspend fun tryTranslateWithGemini(
        title: String,
        summary: String,
        targetLanguage: AppLanguage
    ): LocalizedContent? {
        val targetLangName = when (targetLanguage) {
            AppLanguage.URDU -> "Urdu (اردو script)"
            AppLanguage.ARABIC -> "Arabic (العربية script)"
            AppLanguage.TURKISH -> "Turkish (Türkçe)"
            AppLanguage.HINDI -> "Hindi (हिन्दी Devanagari script)"
            AppLanguage.ENGLISH -> "English"
        }

        val prompt = buildString {
            appendLine("Translate the following news headline and summary strictly into $targetLangName.")
            appendLine("Rules:")
            appendLine("1. Translate ONLY the provided text. Do NOT add any outside facts or assumptions.")
            appendLine("2. Keep the summary concise (maximum 60 words).")
            appendLine("3. Return JSON with keys \"title\" and \"summary\".")
            appendLine()
            appendLine("Headline: $title")
            appendLine("Summary: $summary")
        }

        return try {
            val response = withTimeout(18_000L) {
                generativeModel.generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) return null

            val jsonObj = JSONObject(rawJson)
            val translatedTitle = jsonObj.optString("title").trim()
            val translatedSummary = jsonObj.optString("summary").trim()

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
            Log.w(TAG, "Gemini AI translation unavailable, switching to instant cloud fallback: ${e.message}")
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

            val translatedTitle = titleDeferred.await()?.trim().orEmpty()
            if (translatedTitle.isBlank()) return@coroutineScope null

            val translatedSummary = summaryDeferred.await()?.trim()?.takeIf { it.isNotBlank() }
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
        private const val FIREBASE_COOLDOWN_MS = 5L * 60L * 1000L // 5 minutes

        fun defaultTranslationHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
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
