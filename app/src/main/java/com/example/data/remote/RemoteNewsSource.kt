package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ArticleEntity
import com.example.model.IslamicSection
import com.example.model.NewsCategory
import com.example.model.NewsSourceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

interface RemoteNewsSource {
    suspend fun fetchLatestArticles(): List<ArticleEntity>
    fun loadWhitelistedSources(): List<NewsSourceConfig>
}

class RssRemoteNewsSource(
    private val context: Context,
    private val httpClient: OkHttpClient = defaultOkHttpClient(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : RemoteNewsSource {

    private val ogImageFetchSemaphore = Semaphore(6)
    private val feedHealthPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FEED_HEALTH, Context.MODE_PRIVATE)
    }

    override fun loadWhitelistedSources(): List<NewsSourceConfig> {
        return try {
            val jsonString = context.assets.open("news_sources.json").bufferedReader().use { it.readText() }
            parseSourcesJson(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchLatestArticles(): List<ArticleEntity> = withContext(Dispatchers.IO) {
        val sources = loadWhitelistedSources()
        if (sources.isEmpty()) return@withContext emptyList()

        val feedSemaphore = Semaphore(16)
        val allFetched = coroutineScope {
            sources.map { source ->
                async {
                    feedSemaphore.withPermit {
                        fetchFeedArticlesWithBackoff(source)
                    }
                }
            }.awaitAll().flatten()
        }

        deduplicateArticles(allFetched)
    }

    internal fun shouldSkipFeed(feedUrl: String, nowMs: Long = currentTimeProvider()): Boolean {
        val cooldownUntil = feedHealthPrefs.getLong(cooldownKey(feedUrl), 0L)
        return cooldownUntil > nowMs
    }

    internal fun recordFeedOutcome(feedUrl: String, succeeded: Boolean, nowMs: Long = currentTimeProvider()) {
        val fKey = failCountKey(feedUrl)
        val cKey = cooldownKey(feedUrl)
        synchronized(this) {
            if (succeeded) {
                feedHealthPrefs.edit()
                    .putInt(fKey, 0)
                    .putLong(cKey, 0L)
                    .apply()
            } else {
                val fails = feedHealthPrefs.getInt(fKey, 0) + 1
                val editor = feedHealthPrefs.edit().putInt(fKey, fails)
                if (fails >= MAX_CONSECUTIVE_FAILURES) {
                    editor.putLong(cKey, nowMs + BACKOFF_DURATION_MS)
                }
                editor.apply()
            }
        }
    }

    private suspend fun fetchFeedArticlesWithBackoff(source: NewsSourceConfig): List<ArticleEntity> {
        val now = currentTimeProvider()
        if (shouldSkipFeed(source.feedUrl, now)) {
            return emptyList()
        }

        val xmlContent = try {
            val request = Request.Builder()
                .url(source.feedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    recordFeedOutcome(source.feedUrl, succeeded = false, nowMs = now)
                    return emptyList()
                }
                response.body?.string() ?: run {
                    recordFeedOutcome(source.feedUrl, succeeded = false, nowMs = now)
                    return emptyList()
                }
            }
        } catch (e: Exception) {
            recordFeedOutcome(source.feedUrl, succeeded = false, nowMs = now)
            return emptyList()
        }

        val rawItems = parseRssXml(xmlContent, source)
        if (rawItems.isEmpty()) {
            recordFeedOutcome(source.feedUrl, succeeded = false, nowMs = now)
            return emptyList()
        }

        recordFeedOutcome(source.feedUrl, succeeded = true, nowMs = now)
        return rawItems
    }

    internal fun fetchOgImage(articleUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(articleUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStream = response.body?.charStream() ?: return null
                val htmlHead = readHtmlHeadUpToLimit(bodyStream, MAX_OG_HTML_BYTES)
                if (htmlHead.isEmpty()) return null
                extractOgImageFromHtml(htmlHead, articleUrl)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun failCountKey(feedUrl: String): String = "fail_count_${sha256Hex(feedUrl).take(16)}"
    private fun cooldownKey(feedUrl: String): String = "cooldown_until_${sha256Hex(feedUrl).take(16)}"

    companion object {
        const val USER_AGENT = "KhabarnamaApp/1.0 (contact: ziaullahkhalid793@gmail.com)"
        private const val PREFS_FEED_HEALTH = "khabarnama_feed_health"
        const val MAX_CONSECUTIVE_FAILURES = 3
        const val BACKOFF_DURATION_MS = 24L * 3600L * 1000L
        const val MAX_OG_IMAGE_FETCHES_PER_FEED = 5
        const val MAX_OG_HTML_BYTES = 128 * 1024 // 128 KB

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .callTimeout(6, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        /**
         * Reads from the HTML stream in chunks until `</head>` (case-insensitive) is encountered
         * or [maxChars] (128 KB) have been read.
         */
        fun readHtmlHeadUpToLimit(reader: Reader, maxChars: Int = MAX_OG_HTML_BYTES): String {
            val sb = StringBuilder()
            val buffer = CharArray(4096)
            while (sb.length < maxChars) {
                val toRead = minOf(buffer.size, maxChars - sb.length)
                val readCount = reader.read(buffer, 0, toRead)
                if (readCount == -1) break
                sb.append(buffer, 0, readCount)
                if (sb.indexOf("</head>", ignoreCase = true) != -1) {
                    break
                }
            }
            return sb.toString()
        }

        fun parseSourcesJson(jsonString: String): List<NewsSourceConfig> {
            val array = JSONArray(jsonString)
            val result = mutableListOf<NewsSourceConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name").trim()
                val feedUrl = obj.optString("feedUrl").trim()
                val categoryStr = obj.optString("category", "WORLD").trim()
                val language = obj.optString("language", "EN").trim()
                val isIslamicSource = obj.optBoolean("isIslamicSource", false)
                val isScholarlySource = obj.optBoolean("isScholarlySource", false)
                if (name.isNotEmpty() && feedUrl.startsWith("http")) {
                    result.add(
                        NewsSourceConfig(
                            name = name,
                            feedUrl = feedUrl,
                            category = NewsCategory.fromString(categoryStr),
                            language = language,
                            isIslamicSource = isIslamicSource,
                            isScholarlySource = isScholarlySource
                        )
                    )
                }
            }
            return result
        }

        fun parseRssXml(xmlContent: String, source: NewsSourceConfig): List<ArticleEntity> {
            val items = mutableListOf<ArticleEntity>()
            try {
                val sanitizedXml = xmlContent.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                val factory = XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = false
                }
                val parser = factory.newPullParser()
                parser.setInput(StringReader(sanitizedXml))

                var eventType = parser.eventType
                var inItem = false
                var inItemImage = false

                var title = ""
                var link = ""
                var pubDateRaw = ""
                var description = ""
                var contentEncoded = ""
                var imageUrl = ""
                val itemCategories = mutableListOf<String>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val rawName = parser.name ?: ""
                            val localName = rawName.substringAfterLast(':').lowercase(Locale.ROOT)
                            val lowerRaw = rawName.lowercase(Locale.ROOT)

                            if (localName == "item" || localName == "entry") {
                                inItem = true
                                inItemImage = false
                                title = ""
                                link = ""
                                pubDateRaw = ""
                                description = ""
                                contentEncoded = ""
                                imageUrl = ""
                                itemCategories.clear()
                            } else if (inItem) {
                                when {
                                    localName == "image" -> {
                                        inItemImage = true
                                        val attrUrl = parser.getAttributeValue(null, "url")
                                            ?: parser.getAttributeValue(null, "src")
                                            ?: parser.getAttributeValue(null, "href")
                                        if (!attrUrl.isNullOrBlank() && imageUrl.isBlank()) {
                                            imageUrl = attrUrl.trim()
                                        }
                                    }
                                    localName == "img" -> {
                                        val srcAttr = parser.getAttributeValue(null, "src")
                                            ?: parser.getAttributeValue(null, "url")
                                        if (!srcAttr.isNullOrBlank() && imageUrl.isBlank()) {
                                            imageUrl = srcAttr.trim()
                                        }
                                    }
                                    inItemImage && localName == "url" -> {
                                        val urlText = safeNextText(parser).trim()
                                        if (urlText.startsWith("http") && imageUrl.isBlank()) {
                                            imageUrl = urlText
                                        }
                                    }
                                    !inItemImage && localName == "title" -> {
                                        title = safeNextText(parser)
                                    }
                                    !inItemImage && localName == "link" -> {
                                        val href = parser.getAttributeValue(null, "href")
                                        val rel = parser.getAttributeValue(null, "rel")
                                        val textLink = safeNextText(parser).trim()
                                        if (textLink.startsWith("http")) {
                                            link = textLink
                                        } else if (!href.isNullOrBlank() && (rel == null || rel == "alternate") && link.isBlank()) {
                                            link = href.trim()
                                        }
                                    }
                                    localName == "pubdate" || localName == "published" || localName == "updated" || lowerRaw == "dc:date" -> {
                                        if (pubDateRaw.isBlank()) {
                                            pubDateRaw = safeNextText(parser).trim()
                                        }
                                    }
                                    localName == "description" || localName == "summary" -> {
                                        description = safeNextText(parser)
                                    }
                                    lowerRaw == "content:encoded" || localName == "encoded" || localName == "content" && lowerRaw != "media:content" -> {
                                        val attrUrl = parser.getAttributeValue(null, "url")
                                        if (!attrUrl.isNullOrBlank()) {
                                            if (imageUrl.isBlank() && isLikelyImageUrl(attrUrl, parser.getAttributeValue(null, "type"))) {
                                                imageUrl = attrUrl.trim()
                                            }
                                        } else {
                                            contentEncoded = safeNextText(parser)
                                        }
                                    }
                                    lowerRaw == "media:content" || lowerRaw == "media:thumbnail" || localName == "thumbnail" -> {
                                        val attrUrl = parser.getAttributeValue(null, "url")
                                        val medium = parser.getAttributeValue(null, "medium")
                                        val type = parser.getAttributeValue(null, "type")
                                        if (!attrUrl.isNullOrBlank() && imageUrl.isBlank()) {
                                            if (medium == null || medium.equals("image", ignoreCase = true) || isLikelyImageUrl(attrUrl, type)) {
                                                imageUrl = attrUrl.trim()
                                            }
                                        }
                                    }
                                    localName == "enclosure" -> {
                                        val attrUrl = parser.getAttributeValue(null, "url")
                                        val type = parser.getAttributeValue(null, "type")
                                        if (!attrUrl.isNullOrBlank() && imageUrl.isBlank() && isLikelyImageUrl(attrUrl, type)) {
                                            imageUrl = attrUrl.trim()
                                        }
                                    }
                                    !inItemImage && localName == "category" -> {
                                        val term = parser.getAttributeValue(null, "term")
                                        val catText = safeNextText(parser).trim()
                                        val catVal = if (catText.isNotEmpty()) catText else (term ?: "")
                                        if (catVal.isNotEmpty()) {
                                            itemCategories.add(catVal)
                                        }
                                    }
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inItem && inItemImage && imageUrl.isBlank()) {
                                val text = parser.text?.trim().orEmpty()
                                if (text.startsWith("http")) {
                                    imageUrl = text
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val rawName = parser.name ?: ""
                            val localName = rawName.substringAfterLast(':').lowercase(Locale.ROOT)
                            if (localName == "image") {
                                inItemImage = false
                            } else if (localName == "item" || localName == "entry") {
                                inItem = false
                                inItemImage = false
                                buildArticleEntity(
                                    rawTitle = title,
                                    rawLink = link,
                                    rawPubDate = pubDateRaw,
                                    rawDescription = description,
                                    rawContentEncoded = contentEncoded,
                                    rawImageUrl = imageUrl,
                                    itemCategories = itemCategories,
                                    source = source
                                )?.let { items.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (_: Exception) {
                // Return any items successfully parsed before malformed XML tail
            }
            return items
        }

        private fun safeNextText(parser: XmlPullParser): String {
            return try {
                parser.nextText() ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        private fun buildArticleEntity(
            rawTitle: String,
            rawLink: String,
            rawPubDate: String,
            rawDescription: String,
            rawContentEncoded: String,
            rawImageUrl: String,
            itemCategories: List<String>,
            source: NewsSourceConfig
        ): ArticleEntity? {
            val cleanTitle = cleanHtmlText(rawTitle)
            val cleanLink = rawLink.trim()
            if (cleanTitle.isBlank() || !cleanLink.startsWith("http")) return null

            var resolvedImage = rawImageUrl.trim()
            if (resolvedImage.isBlank()) {
                resolvedImage = extractFirstImgFromHtml(rawDescription, cleanLink)
                    ?: extractFirstImgFromHtml(rawContentEncoded, cleanLink)
                    ?: ""
            } else {
                resolvedImage = resolveUrl(cleanLink, resolvedImage)
            }
            if (!isValidHttpImageUrl(resolvedImage)) {
                resolvedImage = ""
            }

            val rawBody = if (rawDescription.isNotBlank()) rawDescription else rawContentEncoded
            val cleanedSummary = formatConciseSummary(rawBody, cleanTitle, maxWords = 60)

            val publishedAt = parsePubDateMillis(rawPubDate)
            val (category, islamicSection) = classifyArticle(
                title = cleanTitle,
                summary = cleanedSummary,
                itemCategories = itemCategories,
                source = source
            )

            val normalizedUrl = normalizeUrl(cleanLink)
            val normalizedTitle = normalizeTitle(cleanTitle)
            val id = sha256Hex(normalizedUrl).take(24)

            return ArticleEntity(
                id = id,
                normalizedUrl = normalizedUrl,
                normalizedTitle = normalizedTitle,
                title = cleanTitle,
                summary = cleanedSummary,
                category = category.name,
                islamicSection = islamicSection?.name,
                imageUrl = resolvedImage,
                sourceName = source.name,
                sourceUrl = cleanLink,
                publishedAt = publishedAt,
                isIslamicSource = source.isIslamicSource,
                isScholarlySource = source.isScholarlySource
            )
        }

        /**
         * Word-boundary keyword & multi-word phrase matching.
         * Every single word or multi-word phrase is matched with `\b...\b` boundaries so substrings
         * like "important" -> "import", "criminal" -> "mina", or "individual" -> "dua" never match.
         */
        fun matchesAnyWordOrPhrase(text: String, keywordsOrPhrases: List<String>): Boolean {
            if (text.isBlank() || keywordsOrPhrases.isEmpty()) return false
            val normalizedText = text.replace(Regex("\\s+"), " ")
            return keywordsOrPhrases.any { rawPhrase ->
                val phrase = rawPhrase.trim().replace(Regex("\\s+"), " ")
                if (phrase.isEmpty()) return@any false
                val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
                regex.containsMatchIn(normalizedText)
            }
        }

        fun classifyArticle(
            title: String,
            summary: String,
            itemCategories: List<String>,
            source: NewsSourceConfig
        ): Pair<NewsCategory, IslamicSection?> {
            val combined = buildString {
                append(title)
                append(' ')
                append(summary)
                append(' ')
                itemCategories.forEach {
                    append(it)
                    append(' ')
                }
            }

            // Detect political, military, war, or militant stories so they are never placed into ISLAMIC
            val isPoliticalOrConflict = matchesAnyWordOrPhrase(
                combined,
                listOf(
                    "islamic state", "isis", "isil", "daesh", "islamic republic",
                    "islamic jihad", "al-qaeda", "taliban", "militant", "militants",
                    "airstrike", "airstrikes", "missile", "missiles", "cruise missile",
                    "ballistic", "houthi", "houthis", "hezbollah", "warplane", "warplanes",
                    "artillery", "ceasefire", "military", "troops", "battalion",
                    "terrorist", "terrorism", "sanctions", "parliament", "general election",
                    "presidential election", "diplomatic", "foreign minister", "defense minister"
                )
            )

            // Specific religious/worship/pilgrimage keywords (excluding generic "islamic" or "muslim world")
            val isReligiousStoryByKeyword = !isPoliticalOrConflict && matchesAnyWordOrPhrase(
                combined,
                listOf(
                    "hajj", "umrah", "makkah", "mecca", "madinah", "medina", "haramain",
                    "masjid al-haram", "grand mosque", "prophet's mosque", "al-masjid an-nabawi",
                    "two holy mosques", "kaaba", "tawaf", "zamzam", "mount arafat", "muzdalifah",
                    "ramadan", "eid al-fitr", "eid al-adha", "taraweeh", "itikaf",
                    "zakat", "fatwa", "shariah", "fiqh", "quran", "quranic", "hadith",
                    "tafsir", "mufti", "shaykh"
                )
            )

            val isIslamicEligible = when {
                isPoliticalOrConflict -> false
                source.isScholarlySource -> true
                source.isIslamicSource -> true
                source.category != NewsCategory.PAKISTAN && isReligiousStoryByKeyword -> true
                else -> false
            }

            val category: NewsCategory = when {
                isIslamicEligible -> NewsCategory.ISLAMIC

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "cricket", "football", "soccer", "tennis", "olympic", "olympics",
                        "world cup", "psl", "fifa", "champions league", "la liga",
                        "premier league", "test series", "t20", "odi", "grand slam",
                        "tournament", "athlete", "athletes"
                    )
                ) -> NewsCategory.SPORTS

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "artificial intelligence", "generative ai", "openai", "software",
                        "smartphone", "cybersecurity", "semiconductor", "semiconductors",
                        "microchip", "robotics", "spacecraft", "nasa", "5g",
                        "quantum computing", "tech startup", "tech startups"
                    )
                ) -> NewsCategory.TECHNOLOGY

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "stock exchange", "kse-100", "inflation", "central bank",
                        "state bank", "imf", "gdp", "it exports", "trade deficit",
                        "trade surplus", "interest rate", "interest rates", "fiscal policy",
                        "monetary policy", "foreign exchange", "oil prices", "economy", "economic"
                    )
                ) -> NewsCategory.BUSINESS

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "pakistan", "islamabad", "lahore", "karachi", "peshawar",
                        "quetta", "rawalpindi", "sindh", "punjab", "balochistan", "khyber"
                    )
                ) -> NewsCategory.PAKISTAN

                source.category == NewsCategory.ISLAMIC -> NewsCategory.WORLD
                else -> source.category
            }

            if (category != NewsCategory.ISLAMIC) {
                return category to null
            }

            // Classify IslamicSection.
            // CRITICAL RULE: FATWA_SCHOLARS stories must come ONLY from sources with isScholarlySource = true.
            val islamicSection: IslamicSection = when {
                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "hajj", "umrah", "pilgrim", "pilgrims", "pilgrimage", "makkah", "mecca",
                        "kaaba", "tawaf", "haramain", "nusuk", "mount arafat",
                        "muzdalifah", "zamzam", "masjid al-haram", "two holy mosques"
                    )
                ) -> IslamicSection.HAJJ_UMRAH

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "ramadan", "eid al-fitr", "eid al-adha", "eid prayer", "eid prayers",
                        "iftar", "suhoor", "sehri", "taraweeh", "itikaf", "shawwal",
                        "zakat al-fitr", "fasting month"
                    )
                ) -> IslamicSection.RAMADAN_EID

                source.isScholarlySource && matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "fatwa", "scholar", "scholars", "fiqh", "shariah", "ruling", "rulings",
                        "mufti", "shaykh", "sheikh", "hadith", "tafsir", "quran", "quranic",
                        "jurisprudence", "halal", "answered by", "supplication"
                    )
                ) -> IslamicSection.FATWA_SCHOLARS

                matchesAnyWordOrPhrase(
                    combined,
                    listOf(
                        "islamic conference", "oic summit", "quran competition", "hajj symposium",
                        "mosque expansion", "prophet's mosque", "al-masjid an-nabawi",
                        "islamic manuscript", "islamic manuscripts", "al-azhar"
                    )
                ) -> IslamicSection.ISLAMIC_EVENTS

                else -> IslamicSection.MUSLIM_WORLD
            }

            return NewsCategory.ISLAMIC to islamicSection
        }

        fun cleanHtmlText(raw: String): String {
            if (raw.isBlank()) return ""
            var text = raw
                .replace(Regex("(?is)<(script|style|figure|figcaption)[^>]*>.*?</\\1>"), " ")
                .replace(Regex("(?is)<[^>]+>"), " ")
            text = decodeHtmlEntities(text)
            text = text.replace(Regex("(?i)The post\\s+.+?\\s+appeared first on\\s+.+?\\.?$"), "")
            return text.replace(Regex("\\s+"), " ").trim()
        }

        fun formatConciseSummary(rawHtmlOrText: String, headline: String, maxWords: Int = 60): String {
            val cleaned = cleanHtmlText(rawHtmlOrText)
            if (cleaned.isBlank()) {
                return headline
            }
            val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size <= maxWords) {
                return words.joinToString(" ")
            }
            return words.take(maxWords).joinToString(" ").trimEnd(',', ';', ':', '-') + "…"
        }

        private fun decodeHtmlEntities(input: String): String {
            var s = input
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&lsquo;", "‘")
                .replace("&rsquo;", "’")
                .replace("&#8216;", "‘")
                .replace("&#8217;", "’")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&#8220;", "“")
                .replace("&#8221;", "”")
                .replace("&ndash;", "–")
                .replace("&mdash;", "—")
                .replace("&#8211;", "–")
                .replace("&#8212;", "—")
                .replace("&hellip;", "…")
                .replace("&#8230;", "…")
                .replace("&lt;", "<")
                .replace("&gt;", ">")

            s = Regex("&#(\\d+);").replace(s) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull()
                if (code != null && code in 32..0x10FFFF) String(Character.toChars(code)) else ""
            }
            s = Regex("&#x([0-9a-fA-F]+);").replace(s) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull(16)
                if (code != null && code in 32..0x10FFFF) String(Character.toChars(code)) else ""
            }
            return s
        }

        fun extractFirstImgFromHtml(html: String, baseUrl: String): String? {
            if (html.isBlank()) return null
            val attrPatterns = listOf(
                Regex("""<img[^>]+(?:data-lazy-src|data-src|data-original|src)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<source[^>]+srcset\s*=\s*["']([^"'\s,]+)""", RegexOption.IGNORE_CASE)
            )
            for (pattern in attrPatterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val rawUrl = decodeHtmlEntities(match.groupValues[1].trim()).substringBefore(' ')
                    val resolved = resolveUrl(baseUrl, rawUrl)
                    if (isValidHttpImageUrl(resolved)) {
                        return resolved
                    }
                }
            }
            return null
        }

        fun extractOgImageFromHtml(html: String, baseUrl: String): String? {
            if (html.isBlank()) return null
            val patterns = listOf(
                Regex("""<meta[^>]+property\s*=\s*["']og:image(?::secure_url|:url)?["'][^>]+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+property\s*=\s*["']og:image(?::secure_url|:url)?["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+name\s*=\s*["']twitter:image(?::src)?["'][^>]+content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+name\s*=\s*["']twitter:image(?::src)?["']""", RegexOption.IGNORE_CASE)
            )
            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null) {
                    val candidate = resolveUrl(baseUrl, decodeHtmlEntities(match.groupValues[1].trim()))
                    if (isValidHttpImageUrl(candidate)) {
                        return candidate
                    }
                }
            }
            return null
        }

        private fun resolveUrl(baseUrl: String, candidate: String): String {
            if (candidate.isBlank()) return ""
            val trimmed = candidate.trim()
            if (trimmed.startsWith("https://", ignoreCase = true)) {
                return trimmed
            }
            if (trimmed.startsWith("http://", ignoreCase = true)) {
                return "https://" + trimmed.substring(7)
            }
            if (trimmed.startsWith("//")) {
                return "https:$trimmed"
            }
            return try {
                val resolved = URI(baseUrl).resolve(trimmed).toString()
                if (resolved.startsWith("http://", ignoreCase = true)) {
                    "https://" + resolved.substring(7)
                } else {
                    resolved
                }
            } catch (_: Exception) {
                trimmed
            }
        }

        private fun isLikelyImageUrl(url: String, mimeType: String?): Boolean {
            if (mimeType != null && mimeType.startsWith("image/", ignoreCase = true)) return true
            val lower = url.lowercase(Locale.ROOT)
            return lower.startsWith("http") && !lower.endsWith(".mp3") && !lower.endsWith(".mp4") && !lower.endsWith(".pdf")
        }

        private fun isValidHttpImageUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val lower = url.lowercase(Locale.ROOT)
            if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
            if (lower.contains("1x1") || lower.contains("pixel.") || lower.contains("spacer.gif") || lower.startsWith("data:")) {
                return false
            }
            return true
        }

        fun parsePubDateMillis(rawDate: String): Long {
            if (rawDate.isBlank()) return System.currentTimeMillis()
            val normalized = rawDate.trim()
                .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
                .replace(Regex("^([A-Za-z]{3},\\s+\\d{1,2}\\s+[A-Za-z]{3}\\s+)(\\d{2})(\\s+\\d{2}:\\d{2})"), "${'$'}120${'$'}2${'$'}3")

            val patterns = listOf(
                "EEE, dd MMM yyyy HH:mm:ss Z",
                "EEE, dd MMM yyyy HH:mm Z",
                "dd MMM yyyy HH:mm:ss Z",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsed = sdf.parse(normalized)
                    if (parsed != null) {
                        return parsed.time
                    }
                } catch (_: Exception) {
                    // Try next format
                }
            }
            return System.currentTimeMillis()
        }

        fun normalizeUrl(url: String): String {
            val trimmed = url.trim()
            return try {
                val uri = URI(trimmed)
                val host = (uri.host ?: "").lowercase(Locale.ROOT).removePrefix("www.")
                val path = (uri.path ?: "").trimEnd('/')
                "$host$path"
            } catch (_: Exception) {
                trimmed.lowercase(Locale.ROOT)
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .substringBefore('?')
                    .substringBefore('#')
                    .trimEnd('/')
            }
        }

        fun normalizeTitle(title: String): String {
            return title.lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun deduplicateArticles(articles: List<ArticleEntity>): List<ArticleEntity> {
            val seenUrls = HashSet<String>()
            val seenTitles = HashSet<String>()
            val unique = mutableListOf<ArticleEntity>()

            for (article in articles.sortedByDescending { it.publishedAt }) {
                if (article.normalizedUrl.isBlank() || article.normalizedTitle.isBlank()) continue
                if (seenUrls.add(article.normalizedUrl) && seenTitles.add(article.normalizedTitle)) {
                    unique.add(article)
                }
            }
            return unique
        }

        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
