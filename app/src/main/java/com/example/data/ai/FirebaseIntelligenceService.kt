package com.example.data.ai

import android.util.Log
import com.example.model.AppLanguage
import com.example.model.ArticleBriefing
import com.example.model.NewsArticle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class SituationBrief(
    val headline: String,
    val bullets: List<String>,
    val securityFocusNote: String,
    val generatedAtMs: Long = System.currentTimeMillis()
)

class FirebaseIntelligenceService {

    private val articleBriefingSchema = Schema.obj(
        mapOf(
            "editorialHeadline" to Schema.string("Clear, authoritative journalistic headline in the requested language."),
            "detailedSummary" to Schema.string("In-depth 100 to 150 word editorial report grounded strictly in the provided story details."),
            "keyTakeaways" to Schema.array(
                items = Schema.string("Concise single-sentence key takeaway."),
                description = "Exactly 3 or 4 crisp key points covering the core developments."
            ),
            "strategicContext" to Schema.string("1 to 2 sentences explaining the broader security, geopolitical, regional, or scholarly significance based only on the provided report."),
            "notableVoices" to Schema.string("Official statements, journalist/correspondent observations, X/social media posts, or diverse scholar viewpoints cited in the report.")
        )
    )

    private val situationBriefSchema = Schema.obj(
        mapOf(
            "headline" to Schema.string("Concise Intelligence & Global Situation Brief title in the requested language."),
            "bullets" to Schema.array(
                items = Schema.string("Crisp 1-sentence development summary."),
                description = "4 key developments prioritizing security, conflict tensions, Palestine, Muslim world, and Pakistan."
            ),
            "securityFocusNote" to Schema.string("1-sentence assessment of current regional tensions and verified alerts.")
        )
    )

    @Volatile
    private var cachedArticleModelPair: Pair<String, com.google.firebase.ai.GenerativeModel>? = null

    @Volatile
    private var cachedSituationModelPair: Pair<String, com.google.firebase.ai.GenerativeModel>? = null

    private fun getArticleBriefingModel(): com.google.firebase.ai.GenerativeModel {
        val targetModelName = com.example.data.remote.RemoteConfigManager.config.value.aiModelName
        val current = cachedArticleModelPair
        if (current != null && current.first == targetModelName) return current.second
        val created = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = targetModelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = articleBriefingSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are the Chief Editorial & Intelligence Analyst for Khabarnama, a serious multilingual global, security, Palestine, Pakistan, and Islamic affairs dispatch platform. " +
                        "Produce deep, clear, strictly grounded editorial briefings ONLY from the provided article details. NEVER fabricate dates, numbers, quotes, or external facts.\n" +
                        "Editorial standards:\n" +
                        "- URDU (اردو): Write in high-standard, eloquent journalistic Urdu (شستہ و بامحاورہ صحافتی اردو — بی بی سی اردو اور ڈان معیار), using proper Urdu punctuation (۔ ، ؟) and natural syntax.\n" +
                        "- ARABIC (العربية): Write in eloquent Modern Standard Journalistic Arabic (العربية الفصحى الإعلامية الرصينة).\n" +
                        "- Keep scholarly reporting balanced across global Arab and non-Arab (عرب و عجم) scholars without promoting any single figure exclusively.\n" +
                        "- Highlight verified journalist dispatches, official statements, and X/social remarks when present."
                )
            }
        )
        cachedArticleModelPair = targetModelName to created
        return created
    }

    private fun getSituationBriefModel(): com.google.firebase.ai.GenerativeModel {
        val targetModelName = com.example.data.remote.RemoteConfigManager.config.value.aiModelName
        val current = cachedSituationModelPair
        if (current != null && current.first == targetModelName) return current.second
        val created = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = targetModelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = situationBriefSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You synthesize live verified headlines into a concise executive intelligence briefing. " +
                        "Prioritize security developments, conflict escalations, Palestine, Muslim world affairs, and Pakistan. " +
                        "Never invent facts not present in the provided headlines. Write in fluent journalistic Urdu, Arabic, Turkish, Hindi, or English as requested."
                )
            }
        )
        cachedSituationModelPair = targetModelName to created
        return created
    }

    suspend fun generateArticleBriefing(
        article: NewsArticle,
        targetLanguage: AppLanguage
    ): Result<ArticleBriefing> = withContext(Dispatchers.IO) {
        if (!com.example.data.remote.RemoteConfigManager.config.value.aiBriefingEnabled) {
            return@withContext Result.failure(IllegalStateException("AI editorial briefings are temporarily disabled by remote configuration."))
        }
        try {
            val langLabel = languagePromptName(targetLanguage)
            val sourceText = article.fullBody.ifBlank { article.summary }
            val prompt = buildString {
                appendLine("Produce a structured deep editorial briefing strictly in $langLabel.")
                appendLine("Source Outlet: ${article.sourceName} (Category: ${article.category.englishLabel})")
                appendLine("Original Headline: ${article.title}")
                appendLine("Original Dispatch Text: $sourceText")
                appendLine("Source URL: ${article.sourceUrl}")
                appendLine()
                appendLine("Ensure 'detailedSummary' is rich and readable (100-150 words) in authentic journalistic $langLabel, and provide 3-4 clear 'keyTakeaways', 'strategicContext', and 'notableVoices'.")
            }

            val response = withTimeout(45_000L) {
                getArticleBriefingModel().generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Empty AI briefing response."))
            }

            val json = JSONObject(rawJson)
            val headline = GeminiNewsTranslator.postProcessLanguageText(json.optString("editorialHeadline").trim(), targetLanguage)
            val summary = GeminiNewsTranslator.postProcessLanguageText(json.optString("detailedSummary").trim(), targetLanguage)
            val takeawaysArray = json.optJSONArray("keyTakeaways")
            val takeaways = buildList {
                if (takeawaysArray != null) {
                    for (i in 0 until takeawaysArray.length()) {
                        val item = GeminiNewsTranslator.postProcessLanguageText(takeawaysArray.optString(i).trim(), targetLanguage)
                        if (item.isNotBlank()) add(item)
                    }
                }
            }
            val contextText = GeminiNewsTranslator.postProcessLanguageText(json.optString("strategicContext").trim(), targetLanguage)
            val voicesText = GeminiNewsTranslator.postProcessLanguageText(json.optString("notableVoices").trim(), targetLanguage)

            if (summary.isBlank()) {
                return@withContext Result.failure(IllegalStateException("AI briefing summary was blank."))
            }

            Result.success(
                ArticleBriefing(
                    editorialHeadline = headline.ifBlank { article.getTitle(targetLanguage) },
                    detailedSummary = summary,
                    keyTakeaways = takeaways,
                    strategicContext = contextText,
                    notableVoices = voicesText
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI article briefing failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateSituationBrief(
        articles: List<NewsArticle>,
        targetLanguage: AppLanguage
    ): Result<SituationBrief> = withContext(Dispatchers.IO) {
        if (!com.example.data.remote.RemoteConfigManager.config.value.situationBriefEnabled) {
            return@withContext Result.failure(IllegalStateException("Situation digests are temporarily paused by remote configuration."))
        }
        if (articles.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("No live dispatches available to summarize."))
        }
        try {
            val langLabel = languagePromptName(targetLanguage)
            val topFeed = articles.take(8)
            val prompt = buildString {
                appendLine("Synthesize these latest verified dispatches into a crisp Situation Brief strictly in $langLabel.")
                appendLine("Focus on war/conflict status, security alerts, Palestine, regional tensions, and key statements:")
                topFeed.forEachIndexed { idx, item ->
                    appendLine("${idx + 1}. [${item.category.englishLabel} | ${item.sourceName}] ${item.title} — ${item.summary}")
                }
            }

            val response = withTimeout(45_000L) {
                getSituationBriefModel().generateContent(prompt)
            }
            val rawJson = response.text?.trim().orEmpty()
            if (rawJson.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Empty situation brief response."))
            }

            val json = JSONObject(rawJson)
            val headline = GeminiNewsTranslator.postProcessLanguageText(json.optString("headline").trim(), targetLanguage)
            val bulletsArray = json.optJSONArray("bullets")
            val bullets = buildList {
                if (bulletsArray != null) {
                    for (i in 0 until bulletsArray.length()) {
                        val b = GeminiNewsTranslator.postProcessLanguageText(bulletsArray.optString(i).trim(), targetLanguage)
                        if (b.isNotBlank()) add(b)
                    }
                }
            }
            val note = GeminiNewsTranslator.postProcessLanguageText(json.optString("securityFocusNote").trim(), targetLanguage)

            if (bullets.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No situation bullets returned."))
            }

            Result.success(
                SituationBrief(
                    headline = headline.ifBlank { "Live Intelligence & Situation Brief" },
                    bullets = bullets,
                    securityFocusNote = note
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI situation brief failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun languagePromptName(lang: AppLanguage): String = when (lang) {
        AppLanguage.URDU -> "Urdu (اردو — شستہ، بامحاورہ صحافتی اردو)"
        AppLanguage.ARABIC -> "Arabic (العربية الفصحى الإعلامية الرصينة)"
        AppLanguage.TURKISH -> "Turkish (Türkçe)"
        AppLanguage.HINDI -> "Hindi (हिन्दी)"
        AppLanguage.ENGLISH -> "English"
    }

    companion object {
        private const val TAG = "FirebaseIntelService"
    }
}
