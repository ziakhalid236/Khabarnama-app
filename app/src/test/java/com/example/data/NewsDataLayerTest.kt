package com.example.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.NewsTranslator
import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.local.ArticleTranslationEntity
import com.example.data.remote.RemoteNewsSource
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsCategory
import com.example.model.NewsSourceConfig
import com.example.ui.NewsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.StringReader
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class NewsDataLayerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun whitelistedSourcesJson_loadsValidTrustedOutletsAndScholarlyFlags() {
        val rssSource = RssRemoteNewsSource(context)
        val sources = rssSource.loadWhitelistedSources()

        assertTrue("Expected non-empty whitelisted sources in assets/news_sources.json", sources.isNotEmpty())
        assertTrue(sources.any { it.name == "Dawn" })
        assertTrue(sources.any { it.name == "The Express Tribune" })
        assertTrue(sources.any { it.name == "Al Jazeera" })
        assertTrue(sources.any { it.name == "Saudi Gazette" && !it.isIslamicSource && !it.isScholarlySource })
        assertTrue(sources.any { it.name == "Anadolu Agency" && !it.isIslamicSource && !it.isScholarlySource })
        assertTrue(sources.any { it.name == "SeekersGuidance" && it.isScholarlySource })
        assertTrue(sources.any { it.name == "AboutIslam" && it.isScholarlySource })
        assertTrue(sources.any { it.name == "Islam21c" && it.isScholarlySource })
        assertTrue(sources.any { it.name == "5Pillars" && it.isIslamicSource && !it.isScholarlySource })
    }

    @Test
    fun rssParser_extractsTitleLinkPubDateDescriptionAndImagesFromAllFormats() {
        val source = NewsSourceConfig(
            name = "The Express Tribune",
            feedUrl = "https://tribune.com.pk/feed/pakistan",
            category = NewsCategory.PAKISTAN,
            language = "EN",
            isIslamicSource = false,
            isScholarlySource = false
        )

        val sampleXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>The Express Tribune</title>
                <item>
                  <title>State Bank announces new monetary policy &amp; inflation outlook</title>
                  <link>https://tribune.com.pk/story/2500001/sbp-policy</link>
                  <pubDate>Mon, 10 Nov 2026 08:30:00 +0500</pubDate>
                  <description><![CDATA[<p>The central bank maintained the policy rate amid easing inflation across Pakistan.</p>]]></description>
                  <media:content url="https://i.tribune.com.pk/media/images/sbp-cover.jpg" medium="image" />
                </item>
                <item>
                  <title>Lahore hosts international cricket tournament opener</title>
                  <link>https://tribune.com.pk/story/2500002/lahore-cricket</link>
                  <pubDate>2026-11-10T06:00:00Z</pubDate>
                  <description>Packed stadium welcomes visiting cricket squad in Lahore.</description>
                  <image><img src="https://i.tribune.com.pk/media/images/cricket-featured.jpg" class="featured_image"/></image>
                </item>
                <item>
                  <title>Item without link should be ignored</title>
                  <description>Missing link</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val parsed = RssRemoteNewsSource.parseRssXml(sampleXml, source)
        assertEquals(2, parsed.size)

        val first = parsed[0]
        assertEquals("State Bank announces new monetary policy & inflation outlook", first.title)
        assertEquals("The central bank maintained the policy rate amid easing inflation across Pakistan.", first.summary)
        assertEquals("https://i.tribune.com.pk/media/images/sbp-cover.jpg", first.imageUrl)
        assertEquals(NewsCategory.BUSINESS.name, first.category)
        assertEquals("The Express Tribune", first.sourceName)

        val second = parsed[1]
        assertEquals("Lahore hosts international cricket tournament opener", second.title)
        assertEquals("https://i.tribune.com.pk/media/images/cricket-featured.jpg", second.imageUrl)
        assertEquals(NewsCategory.SPORTS.name, second.category)
    }

    @Test
    fun item1_wordBoundaryKeywordMatching_rejectsSubstringFalsePositivesAndClassifiesAllCategories() {
        val worldSource = NewsSourceConfig(
            name = "Al Jazeera",
            feedUrl = "https://www.aljazeera.com/xml/rss/all.xml",
            category = NewsCategory.WORLD,
            language = "EN",
            isIslamicSource = false,
            isScholarlySource = false
        )

        // 1. "important" must NOT match "import" (BUSINESS)
        val (cat1, sec1) = RssRemoteNewsSource.classifyArticle(
            title = "The most important talks on regional climate cooperation begin in Geneva",
            summary = "Envoys say this is an important step toward a treaty.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.WORLD, cat1)
        assertNull(sec1)

        // 2. "Criminal" / "determination" must NOT match "mina" (HAJJ_UMRAH / ISLAMIC)
        val (cat2, sec2) = RssRemoteNewsSource.classifyArticle(
            title = "Criminal case opened after determination of high court",
            summary = "Prosecutors cited clear determination to pursue the criminal investigation.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.WORLD, cat2)
        assertNull(sec2)

        // 3. "Individual" / "dual" must NOT match "dua" (FATWA_SCHOLARS / ISLAMIC)
        val (cat3, sec3) = RssRemoteNewsSource.classifyArticle(
            title = "Individual rights and dual citizenship law debated by senate",
            summary = "Every individual with dual residency can apply online.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.WORLD, cat3)
        assertNull(sec3)

        // 4. "match" in non-sports context must NOT trigger SPORTS
        val (cat4, _) = RssRemoteNewsSource.classifyArticle(
            title = "Rescue crews match efforts to contain wildfire across coastal hills",
            summary = "Volunteers worked overnight to match emergency supplies with shelters.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.WORLD, cat4)

        // 5. Positive category rules for TECHNOLOGY, PAKISTAN, and ISLAMIC (HAJJ_UMRAH, RAMADAN_EID)
        val (techCat, _) = RssRemoteNewsSource.classifyArticle(
            title = "New semiconductor and quantum computing lab opens",
            summary = "Researchers unveiled next-generation microchip architecture.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.TECHNOLOGY, techCat)

        val (pakCat, _) = RssRemoteNewsSource.classifyArticle(
            title = "Infrastructure project approved in Islamabad and Rawalpindi",
            summary = "Commuters across Punjab will benefit from the new transit line.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.PAKISTAN, pakCat)

        val (hajjCat, hajjSec) = RssRemoteNewsSource.classifyArticle(
            title = "Masjid al-Haram prepares cooling stations for Umrah pilgrims in Makkah",
            summary = "Haramain authorities expanded Zamzam water distribution.",
            itemCategories = emptyList(),
            source = worldSource
        )
        assertEquals(NewsCategory.ISLAMIC, hajjCat)
        assertEquals(IslamicSection.HAJJ_UMRAH, hajjSec)
    }

    @Test
    fun item2_islamicSourceAndPoliticalConflictFiltering_preventsWarsAndNonScholarlyFatwas() {
        // 1. Political / conflict stories from Saudi Gazette or Anadolu Middle East must NOT become ISLAMIC
        val (warCat, warSec) = RssRemoteNewsSource.classifyArticle(
            title = "Air defenses intercept ballistic missile launched by Houthi militia toward Riyadh",
            summary = "Military forces destroyed the cruise missile before impact.",
            itemCategories = emptyList(),
            source = FakeNewsTestData.middleEastWorldSource
        )
        assertEquals(NewsCategory.WORLD, warCat)
        assertNull(warSec)

        // 2. "Islamic State" / "Islamic Republic" political headlines must NEVER become ISLAMIC
        val (isisCat, _) = RssRemoteNewsSource.classifyArticle(
            title = "Coalition forces target Islamic State militant hideout in northern province",
            summary = "Military commanders confirmed the operation.",
            itemCategories = emptyList(),
            source = FakeNewsTestData.middleEastWorldSource
        )
        assertEquals(NewsCategory.WORLD, isisCat)

        val (iranCat, _) = RssRemoteNewsSource.classifyArticle(
            title = "Islamic Republic parliament debates foreign sanctions and diplomatic talks",
            summary = "Foreign minister addressed the session on sanctions.",
            itemCategories = emptyList(),
            source = FakeNewsTestData.middleEastWorldSource
        )
        assertEquals(NewsCategory.WORLD, iranCat)

        // 3. FATWA_SCHOLARS only from isScholarlySource = true
        val fatwaTitle = "Islamic Fiqh Council Issues Fatwa and Shariah Ruling on Digital Contracts"
        val fatwaSummary = "Mufti and scholars explained the Shariah jurisprudence and halal conditions."

        val (catScholarly, secScholarly) = RssRemoteNewsSource.classifyArticle(
            title = fatwaTitle,
            summary = fatwaSummary,
            itemCategories = emptyList(),
            source = FakeNewsTestData.scholarlyIslamicSource
        )
        assertEquals(NewsCategory.ISLAMIC, catScholarly)
        assertEquals(IslamicSection.FATWA_SCHOLARS, secScholarly)

        // From a community Islamic source (isIslamicSource = true, isScholarlySource = false) -> NOT FATWA_SCHOLARS
        val (catCommunity, secCommunity) = RssRemoteNewsSource.classifyArticle(
            title = fatwaTitle,
            summary = fatwaSummary,
            itemCategories = emptyList(),
            source = FakeNewsTestData.communityIslamicSource
        )
        assertEquals(NewsCategory.ISLAMIC, catCommunity)
        assertNotEquals(IslamicSection.FATWA_SCHOLARS, secCommunity)

        // From a general source (isScholarlySource = false) -> NOT FATWA_SCHOLARS
        val (_, secGeneral) = RssRemoteNewsSource.classifyArticle(
            title = fatwaTitle,
            summary = fatwaSummary,
            itemCategories = emptyList(),
            source = FakeNewsTestData.generalSource
        )
        assertNotEquals(IslamicSection.FATWA_SCHOLARS, secGeneral)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun item3_viewModel_debouncesSearchAndDoesNotCancelRunningTranslationsOnLoadMore() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val baseTime = 1_760_000_000_000L
            val allArticles = (1..12).map { idx ->
                com.example.model.NewsArticle(
                    id = "art_$idx",
                    translations = mapOf(
                        AppLanguage.ENGLISH to LocalizedContent(
                            title = "Pakistan Economy Update Number $idx",
                            summary = "Summary for dispatch number $idx."
                        )
                    ),
                    category = NewsCategory.PAKISTAN,
                    sourceName = "The Express Tribune",
                    sourceUrl = "https://tribune.com.pk/story/$idx",
                    publishedAt = baseTime - idx * 1000L
                )
            }

            val searchQueriesObserved = mutableListOf<String>()
            val cancelledTranslations = AtomicInteger(0)
            val completedTranslations = AtomicInteger(0)

            val testRepo = object : NewsRepository {
                override suspend fun syncFromRemote(): SyncStatus = SyncStatus.SUCCESS
                override suspend fun getCachedArticleCount(): Int = allArticles.size
                override suspend fun getFeaturedIslamicArticles(limit: Int): List<com.example.model.NewsArticle> = emptyList()

                override suspend fun getArticlesPage(
                    category: NewsCategory,
                    islamicSection: IslamicSection?,
                    searchQuery: String,
                    language: AppLanguage,
                    excludeIds: Set<String>,
                    beforePublishedAt: Long,
                    afterId: String,
                    pageSize: Int
                ): PaginatedArticlesResult {
                    if (searchQuery.isNotBlank()) {
                        synchronized(searchQueriesObserved) {
                            searchQueriesObserved.add(searchQuery)
                        }
                    }
                    val filtered = allArticles.filter {
                        it.publishedAt < beforePublishedAt &&
                            (searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true))
                    }
                    val page = filtered.take(pageSize)
                    val hasMore = filtered.size > pageSize
                    return PaginatedArticlesResult(
                        articles = page,
                        hasMore = hasMore,
                        nextBeforePublishedAt = page.lastOrNull()?.publishedAt ?: 0L,
                        nextAfterId = page.lastOrNull()?.id ?: ""
                    )
                }

                override suspend fun getOrTranslateArticle(
                    article: com.example.model.NewsArticle,
                    targetLanguage: AppLanguage
                ): LocalizedContent? {
                    return try {
                        delay(150)
                        completedTranslations.incrementAndGet()
                        LocalizedContent("[UR] ${article.title}", "[UR] ${article.summary}")
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        cancelledTranslations.incrementAndGet()
                        throw e
                    }
                }
            }

            val app = ApplicationProvider.getApplicationContext<Application>()
            val vm = NewsViewModel(app, testRepo)
            testScheduler.advanceUntilIdle()
            assertEquals(8, vm.articles.value.size)
            assertTrue(vm.hasMore.value)

            // 1. Test 300ms search debounce: rapid keystrokes only trigger final query after 300ms
            vm.setSearchQuery("P")
            testScheduler.advanceTimeBy(100)
            vm.setSearchQuery("Pa")
            testScheduler.advanceTimeBy(100)
            vm.setSearchQuery("Pakistan")
            testScheduler.advanceTimeBy(299)
            assertTrue("Search should not fire before 300ms debounce expires", searchQueriesObserved.isEmpty())
            testScheduler.advanceTimeBy(5)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Pakistan"), searchQueriesObserved)

            // Clear search
            vm.toggleSearchOpen(false)
            testScheduler.advanceUntilIdle()

            // 2. Switch to Urdu to start translations on page 1, then call loadMore() mid-translation
            vm.setLanguage(AppLanguage.URDU)
            testScheduler.advanceTimeBy(50)
            vm.loadMore()
            testScheduler.advanceUntilIdle()

            assertEquals("loadMore must not cancel in-flight translations", 0, cancelledTranslations.get())
            assertEquals(12, vm.articles.value.size)
            assertEquals(12, completedTranslations.get())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun item4And5_searchInCachedTranslations_andCleanupOldArticlesAndOrphanTranslations() = runTest {
        val now = 1_760_000_000_000L
        val oldTime = now - 16L * 24L * 3600L * 1000L // 16 days old (> 14 days)

        val oldEntity = ArticleEntity(
            id = "expired_1",
            normalizedUrl = "tribune.com.pk/story/expired",
            normalizedTitle = "expired story older than fourteen days",
            title = "Expired Story Older Than Fourteen Days",
            summary = "Should be deleted on sync.",
            category = NewsCategory.PAKISTAN.name,
            islamicSection = null,
            imageUrl = "",
            sourceName = "The Express Tribune",
            sourceUrl = "https://tribune.com.pk/story/expired",
            publishedAt = oldTime,
            isIslamicSource = false,
            isScholarlySource = false
        )

        val dao = database.articleDao()
        dao.insertArticles(listOf(oldEntity))
        dao.insertTranslation(
            ArticleTranslationEntity(
                articleId = "expired_1",
                languageCode = "UR",
                translatedTitle = "پرانی خبر",
                translatedSummary = "یہ خبر پرانی ہے"
            )
        )

        val freshEntities = FakeNewsTestData.createSampleEntities(baseTime = now)
        val fakeRemote = object : RemoteNewsSource {
            override suspend fun fetchLatestArticles(): List<ArticleEntity> = freshEntities
            override fun loadWhitelistedSources(): List<NewsSourceConfig> = listOf(FakeNewsTestData.scholarlyIslamicSource)
        }

        val repo = DefaultNewsRepository(
            remoteSource = fakeRemote,
            articleDao = dao,
            translator = object : NewsTranslator {
                override suspend fun translateArticle(
                    title: String,
                    summary: String,
                    targetLanguage: AppLanguage
                ): LocalizedContent = LocalizedContent("حج اسمارٹ شٹل سروس", "مکہ مکرمہ میں عازمین کے لیے شٹل سروس")
            },
            currentTimeProvider = { now }
        )

        // Sync inserts fresh entities, deletes >14d old articles, and deletes orphan translations
        val syncStatus = repo.syncFromRemote()
        assertEquals(SyncStatus.SUCCESS, syncStatus)
        assertNull("Orphan translation for expired_1 should be deleted", dao.getTranslation("expired_1", "UR"))

        // Translate isl_1 into Urdu so its Urdu text is cached in article_translations
        val page = repo.getArticlesPage(category = NewsCategory.ALL, pageSize = 10)
        val isl1 = page.articles.first { it.id == "isl_1" }
        repo.getOrTranslateArticle(isl1, AppLanguage.URDU)

        // Search using Urdu keyword "اسمارٹ شٹل" while language is URDU
        val urduSearchResult = repo.getArticlesPage(
            category = NewsCategory.ALL,
            searchQuery = "اسمارٹ شٹل",
            language = AppLanguage.URDU,
            pageSize = 10
        )
        assertEquals(1, urduSearchResult.articles.size)
        assertEquals("isl_1", urduSearchResult.articles.first().id)
    }

    @Test
    fun item6_syncEfficiency_headStreamStopsAtEndOfHeadAndThreeStrikeBackoffWorks() {
        // 1. Verify readHtmlHeadUpToLimit stops immediately once </head> is seen
        val largeHtml = "<html><head><meta property=\"og:image\" content=\"https://example.com/pic.jpg\"/></head>" +
            "<body>" + "A".repeat(200_000) + "</body></html>"
        val readHead = RssRemoteNewsSource.readHtmlHeadUpToLimit(StringReader(largeHtml))
        assertTrue(readHead.contains("</head>", ignoreCase = true))
        assertTrue("Stream reader must stop near </head> instead of reading 200KB", readHead.length <= 8192)

        // 2. Verify 3 consecutive failures trigger 24-hour feed skip
        var now = 1_760_000_000_000L
        val rssSource = RssRemoteNewsSource(context, currentTimeProvider = { now })
        val testUrl = "https://example.com/failing-feed.xml"

        assertFalse(rssSource.shouldSkipFeed(testUrl, now))
        rssSource.recordFeedOutcome(testUrl, succeeded = false, nowMs = now)
        assertFalse(rssSource.shouldSkipFeed(testUrl, now))
        rssSource.recordFeedOutcome(testUrl, succeeded = false, nowMs = now)
        assertFalse(rssSource.shouldSkipFeed(testUrl, now))
        rssSource.recordFeedOutcome(testUrl, succeeded = false, nowMs = now)

        // After 3rd consecutive failure, feed is skipped for 24 hours
        assertTrue(rssSource.shouldSkipFeed(testUrl, now + 3600_000L))
        // After 24 hours + 1 second, feed is retried
        assertFalse(rssSource.shouldSkipFeed(testUrl, now + RssRemoteNewsSource.BACKOFF_DURATION_MS + 1000L))
    }

    @Test
    fun item7_offlineFirstLaunchVsCachedOfflineBehavior() = runTest {
        val dao = database.articleDao()
        val offlineRemote = object : RemoteNewsSource {
            override suspend fun fetchLatestArticles(): List<ArticleEntity> = emptyList()
            override fun loadWhitelistedSources(): List<NewsSourceConfig> = emptyList()
        }
        val repo = DefaultNewsRepository(remoteSource = offlineRemote, articleDao = dao)

        // 1. Very first launch without internet (cache empty) -> ERROR_EMPTY_CACHE
        assertEquals(0, repo.getCachedArticleCount())
        assertEquals(SyncStatus.ERROR_EMPTY_CACHE, repo.syncFromRemote())

        // 2. After a successful load populates cache, subsequent offline open -> OFFLINE_WITH_CACHE
        dao.insertArticles(FakeNewsTestData.createSampleEntities())
        assertTrue(repo.getCachedArticleCount() > 0)
        assertEquals(SyncStatus.OFFLINE_WITH_CACHE, repo.syncFromRemote())
    }

    @Test
    fun item8_cloudTranslationParserAndExpandedSources_verified() {
        val sampleJson = """[[["پاکستان کی معیشت میں بہتری۔ ","Pakistan economy improves. ",null,null,10],["برآمدات میں اضافہ ہوا ہے۔","Exports have increased.",null,null,10]],null,"en"]"""
        val parsed = com.example.data.ai.GeminiNewsTranslator.parseCloudTranslationJson(sampleJson)
        assertEquals("پاکستان کی معیشت میں بہتری۔ برآمدات میں اضافہ ہوا ہے۔", parsed)

        val sources = RssRemoteNewsSource(context).loadWhitelistedSources()
        assertTrue("Expected at least 30 active RSS feeds across categories", sources.size >= 30)
        assertTrue(sources.any { it.name == "BBC News" })
        assertTrue(sources.any { it.name == "Arab News" })
        assertTrue(sources.any { it.name == "Dawn" })
        assertTrue(sources.any { it.name == "Bellingcat" && it.category == NewsCategory.SECURITY })
        assertTrue(sources.any { it.name == "War on the Rocks" && it.category == NewsCategory.SECURITY })
        assertTrue(sources.any { it.name == "Yaqeen Institute" && it.isScholarlySource })
    }

    @Test
    fun item9_priorityRanking_fullBodySocialQuotes_andBriefingCache() = runTest {
        val now = 1_760_000_000_000L
        val dao = database.articleDao()

        val securityArticle = ArticleEntity(
            id = "sec_1",
            normalizedUrl = "bellingcat.com/report/1",
            normalizedTitle = "osint analysis of border ceasefire violations",
            title = "OSINT Analysis of Border Ceasefire Violations",
            summary = "Satellite imagery confirms troop movements.",
            fullBody = "Detailed satellite imagery and journalist reports posted on X show heavy artillery deployments.",
            category = NewsCategory.SECURITY.name,
            islamicSection = null,
            imageUrl = "",
            sourceName = "Bellingcat",
            sourceUrl = "https://bellingcat.com/report/1",
            publishedAt = now - 60_000L,
            isIslamicSource = false,
            isScholarlySource = false,
            hasSocialQuote = true
        )
        val sportsArticle = ArticleEntity(
            id = "spt_1",
            normalizedUrl = "espncricinfo.com/story/1",
            normalizedTitle = "cricket squad begins training camp",
            title = "Cricket Squad Begins Training Camp",
            summary = "Players arrived for test series.",
            fullBody = "Players arrived for test series.",
            category = NewsCategory.SPORTS.name,
            islamicSection = null,
            imageUrl = "",
            sourceName = "ESPNcricinfo",
            sourceUrl = "https://espncricinfo.com/story/1",
            publishedAt = now, // newer timestamp, but SPORTS is de-prioritized
            isIslamicSource = false,
            isScholarlySource = false,
            hasSocialQuote = false
        )
        dao.insertArticles(listOf(sportsArticle, securityArticle))

        val repo = DefaultNewsRepository(
            remoteSource = object : RemoteNewsSource {
                override suspend fun fetchLatestArticles(): List<ArticleEntity> = emptyList()
                override fun loadWhitelistedSources(): List<NewsSourceConfig> = emptyList()
            },
            articleDao = dao,
            currentTimeProvider = { now }
        )

        val page = repo.getArticlesPage(category = NewsCategory.ALL, pageSize = 10)
        assertEquals(2, page.articles.size)
        assertEquals("sec_1", page.articles.first().id)
        assertTrue(page.articles.first().hasSocialQuote)

        // Verify direct article lookup by ID (used on notification click)
        val fetchedById = repo.getArticleById("sec_1")
        assertEquals("sec_1", fetchedById?.id)
        assertTrue((fetchedById?.fullBody ?: "").contains("Satellite imagery", ignoreCase = true))

        // Verify Room briefing cache round-trip
        dao.insertBriefing(
            com.example.data.local.ArticleBriefingEntity(
                articleId = "sec_1",
                languageCode = "UR",
                editorialHeadline = "سرحدی صورتحال پر تحقیقی رپورٹ",
                detailedSummary = "سیٹلائٹ تصاویر اور صحافیوں کی رپورٹس کے مطابق سرحدی علاقے میں نقل و حرکت دیکھی گئی ہے۔",
                keyTakeawaysJson = """["نکتہ اول","نکتہ دوم","نکتہ سوم"]""",
                strategicContext = "علاقائی کشیدگی میں اضافہ ہو سکتا ہے۔",
                notableVoices = "ایکس (X) پر صحافیوں کے بیانات"
            )
        )
        val cachedBriefing = dao.getBriefing("sec_1", "UR")?.toDomainModel()
        assertEquals("سرحدی صورتحال پر تحقیقی رپورٹ", cachedBriefing?.editorialHeadline)
        assertEquals(3, cachedBriefing?.keyTakeaways?.size)

        // Verify RSS parser flags hasSocialQuote on X / Twitter / correspondent statements
        val quoteXml = """
            <rss version="2.0"><channel><item>
              <title>Correspondent reports ceasefire statement posted on X</title>
              <link>https://www.aljazeera.com/news/quote-story</link>
              <pubDate>Mon, 10 Nov 2026 10:00:00 GMT</pubDate>
              <description>According to a statement on X, the envoy tweeted urgent diplomatic warnings after defense talks.</description>
            </item></channel></rss>
        """.trimIndent()
        val parsedQuote = RssRemoteNewsSource.parseRssXml(quoteXml, FakeNewsTestData.middleEastWorldSource)
        assertEquals(1, parsedQuote.size)
        assertTrue(parsedQuote.first().hasSocialQuote)
    }

    @Test
    fun item10_deduplicationSkipsExistingUrls_andRemoteConfigParsersValidateInputs() = runTest {
        val now = 1_760_000_000_000L
        val dao = database.articleDao()

        val initialEntity = ArticleEntity(
            id = "orig_1",
            normalizedUrl = "aljazeera.com/news/gaza-aid-1",
            normalizedTitle = "humanitarian convoy reaches northern gaza",
            title = "Humanitarian convoy reaches northern Gaza",
            summary = "Original cached summary",
            category = NewsCategory.PALESTINE.name,
            islamicSection = null,
            imageUrl = "",
            sourceName = "Al Jazeera",
            sourceUrl = "https://www.aljazeera.com/news/gaza-aid-1",
            publishedAt = now - 60_000L,
            isIslamicSource = false,
            isScholarlySource = false
        )
        dao.insertArticles(listOf(initialEntity))

        // Remote returns the same URL with a different id + one genuinely new article
        val duplicateWithDifferentId = initialEntity.copy(
            id = "dup_1",
            summary = "Modified summary that must NOT overwrite cached row"
        )
        val newEntity = initialEntity.copy(
            id = "new_2",
            normalizedUrl = "aljazeera.com/news/security-talks-2",
            normalizedTitle = "security talks resume after border alert",
            title = "Security talks resume after border alert",
            category = NewsCategory.SECURITY.name,
            sourceUrl = "https://www.aljazeera.com/news/security-talks-2",
            publishedAt = now
        )

        val repo = DefaultNewsRepository(
            remoteSource = object : RemoteNewsSource {
                override suspend fun fetchLatestArticles(): List<ArticleEntity> = listOf(duplicateWithDifferentId, newEntity)
                override fun loadWhitelistedSources(): List<NewsSourceConfig> = emptyList()
            },
            articleDao = dao,
            currentTimeProvider = { now }
        )

        assertEquals(SyncStatus.SUCCESS, repo.syncFromRemote())
        assertEquals(2, dao.getTotalCount())
        assertNull("Duplicate normalized URL must not insert a second row", dao.getArticleById("dup_1"))
        assertEquals("Original cached summary", dao.getArticleById("orig_1")?.summary)

        // Verify Firebase Remote Config OTA JSON parsers
        val parsedSources = com.example.data.remote.RemoteConfigManager.parseRemoteNewsSourcesJson(
            """[{"name":"TRT Arabi","feedUrl":"https://www.trtarabi.com/rss","category":"WORLD","language":"EN","isIslamicSource":false},{"name":"Insecure","feedUrl":"http://insecure.example/rss","category":"WORLD"}]"""
        )
        assertEquals(1, parsedSources.size)
        assertEquals("TRT Arabi", parsedSources.first().name)

        val parsedChannels = com.example.data.remote.RemoteConfigManager.parseRemoteVideoChannelsJson(
            """[{"channelId":"UC_SAMPLE_123","channelName":"Al Jazeera Arabic","category":"WORLD","countryFlag":"🇶🇦","watchLiveUrl":"https://www.youtube.com/@AlJazeera/live"}]"""
        )
        assertEquals(1, parsedChannels.size)
        assertEquals("https://www.youtube.com/feeds/videos.xml?channel_id=UC_SAMPLE_123", parsedChannels.first().feedUrl)
    }

    @Test
    fun item11_youtubeAtomFeedParser_andVideoDaoCachingAndCategoryFilter() = runTest {
        val now = 1_760_000_000_000L
        val channel = com.example.model.VideoChannelConfig(
            channelId = "UCNye-wNBqNL5ZzHSJj3l8Bg",
            channelName = "Al Jazeera English",
            category = NewsCategory.WORLD,
            countryFlag = "🇶🇦"
        )
        val atomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <yt:videoId>dQw4w9WgXcQ</yt:videoId>
                <title>Live: Emergency UN Security Council session on Gaza ceasefire</title>
                <link rel="alternate" href="https://www.youtube.com/watch?v=dQw4w9WgXcQ"/>
                <author><name>Al Jazeera English</name></author>
                <published>2026-11-10T10:00:00+00:00</published>
                <media:group>
                  <media:thumbnail url="https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"/>
                  <media:description>Diplomats convene for urgent Gaza humanitarian briefing.</media:description>
                </media:group>
              </entry>
            </feed>
        """.trimIndent()

        val clips = com.example.data.remote.YouTubeAtomVideoSource.parseYouTubeAtomFeed(atomXml, channel)
        assertEquals(1, clips.size)
        val first = clips.first()
        assertEquals("dQw4w9WgXcQ", first.videoId)
        assertEquals("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?rel=0&playsinline=1&autoplay=1", first.embedUrl)
        assertEquals(NewsCategory.PALESTINE.name, first.category)
        assertTrue(first.isLiveBadge)

        val videoDao = database.videoDao()
        videoDao.insertVideos(clips)
        assertEquals(1, videoDao.getTotalCount())
        assertEquals(1, videoDao.getLatestVideos(category = NewsCategory.PALESTINE.name, limit = 10).size)
        assertEquals(0, videoDao.getLatestVideos(category = NewsCategory.SPORTS.name, limit = 10).size)

        // Verify YouTube link extractor from standard HTML/RSS strings
        assertEquals(
            "abcDEF12345",
            com.example.data.remote.YouTubeAtomVideoSource.extractYouTubeVideoId("Check report https://youtu.be/abcDEF12345?si=test")
        )
        assertEquals(
            "xyz98765432",
            com.example.data.remote.YouTubeAtomVideoSource.extractYouTubeVideoId("https://www.youtube.com/watch?v=xyz98765432&feature=share")
        )
    }

    @Test
    fun item12_strictLiveBadgeFiltering_andYouTubeIframeConfigPreventsError153() {
        val channel = com.example.model.VideoChannelConfig(
            channelId = "UCR0fZh5SBxxMNYdg0VzRFkg",
            channelName = "Middle East Eye",
            category = NewsCategory.PALESTINE,
            countryFlag = "🇵🇸"
        )
        val nonLiveAtomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns:yt="http://www.youtube.com/xml/schemas/2015" xmlns:media="http://search.yahoo.com/mrss/" xmlns="http://www.w3.org/2005/Atom">
              <entry>
                <yt:videoId>AbCdEf12345</yt:videoId>
                <title>US lawmaker calls for ending weapons aid to save lives</title>
                <link rel="alternate" href="https://www.youtube.com/watch?v=AbCdEf12345"/>
                <author><name>Middle East Eye</name></author>
                <published>2026-11-10T10:00:00+00:00</published>
              </entry>
            </feed>
        """.trimIndent()

        val clips = com.example.data.remote.YouTubeAtomVideoSource.parseYouTubeAtomFeed(nonLiveAtomXml, channel)
        assertEquals(1, clips.size)
        assertFalse("Word 'lives' must not falsely trigger LIVE badge", clips.first().isLiveBadge)

        val mutedHtml = com.example.ui.components.buildYouTubeIframeHtml("AbCdEf12345<script>", muted = true)
        assertTrue(mutedHtml.contains("https://www.youtube.com/embed/AbCdEf12345script?"))
        assertTrue(mutedHtml.contains("mute=1"))
        assertTrue(mutedHtml.contains("autoplay=1"))
        assertTrue(mutedHtml.contains("origin=https://www.youtube.com"))
        assertTrue(mutedHtml.contains("referrerpolicy=\"strict-origin-when-cross-origin\""))
    }

    @Test
    fun item13_installBaselineSuppressesStaleNotifications_andAllowsNewPostInstallAlerts() {
        val prefs = context.getSharedPreferences("khabarnama_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val oldTime = System.currentTimeMillis() - 60_000L
        val preInstallArticle = com.example.model.NewsArticle(
            id = "pre_install_1",
            translations = mapOf(
                com.example.model.AppLanguage.ENGLISH to com.example.model.LocalizedContent(
                    title = "Pre-install headline",
                    summary = "Pre-install summary"
                )
            ),
            category = NewsCategory.PALESTINE,
            sourceName = "Arab News",
            sourceUrl = "https://arabnews.com/1",
            publishedAt = oldTime
        )

        assertFalse(com.example.util.NewsNotificationHelper.isBaselineEstablished(context))
        val newlyEstablished = com.example.util.NewsNotificationHelper.establishInstallBaselineIfNeeded(
            context,
            listOf(preInstallArticle)
        )
        assertTrue(newlyEstablished)
        assertTrue(com.example.util.NewsNotificationHelper.isBaselineEstablished(context))
        assertTrue("pre_install_1" in com.example.util.NewsNotificationHelper.getBaselineArticleIds(context))

        // Second call must be a no-op
        val secondCall = com.example.util.NewsNotificationHelper.establishInstallBaselineIfNeeded(
            context,
            listOf(preInstallArticle.copy(id = "later_article"))
        )
        assertFalse(secondCall)
    }

    @Test
    fun item14_firebaseInitHelperRegistersContextSafely() {
        val result = com.example.util.FirebaseInitHelper.ensureInitialized(context)
        // Whether google-services values are present or mocked in Robolectric, must never crash
        assertTrue(result || com.google.firebase.FirebaseApp.getApps(context).isEmpty())
    }

    @Test
    fun item15_typographyHasUnspecifiedColor_andVideoSectionDisabledByDefault() {
        assertEquals(
            androidx.compose.ui.graphics.Color.Unspecified,
            com.example.ui.theme.AppTypography.labelLarge.color
        )
        assertEquals(
            androidx.compose.ui.graphics.Color.Unspecified,
            com.example.ui.theme.AppTypography.bodyMedium.color
        )
        assertFalse(com.example.data.remote.AppRemoteConfig().videoSectionEnabled)
    }

    @Test
    fun item16_recaptchaSiteKeyResolvesForAppCheck() {
        val siteKey = com.example.util.resolveRecaptchaSiteKey(context)
        assertTrue("Provisioned recaptcha_site_key must be present and non-blank for App Check", !siteKey.isNullOrBlank())
    }
}
