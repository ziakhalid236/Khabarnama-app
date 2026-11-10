package com.example.data

import com.example.data.local.ArticleEntity
import com.example.model.IslamicSection
import com.example.model.NewsCategory
import com.example.model.NewsSourceConfig

object FakeNewsTestData {

    val scholarlyIslamicSource = NewsSourceConfig(
        name = "SeekersGuidance",
        feedUrl = "https://seekersguidance.org/feed/",
        category = NewsCategory.ISLAMIC,
        language = "EN",
        isIslamicSource = true,
        isScholarlySource = true
    )

    val communityIslamicSource = NewsSourceConfig(
        name = "5Pillars",
        feedUrl = "https://5pillarsuk.com/feed/",
        category = NewsCategory.ISLAMIC,
        language = "EN",
        isIslamicSource = true,
        isScholarlySource = false
    )

    val middleEastWorldSource = NewsSourceConfig(
        name = "Saudi Gazette",
        feedUrl = "https://saudigazette.com.sa/rssFeed/74",
        category = NewsCategory.WORLD,
        language = "EN",
        isIslamicSource = false,
        isScholarlySource = false
    )

    val generalSource = NewsSourceConfig(
        name = "The Express Tribune",
        feedUrl = "https://tribune.com.pk/feed/pakistan",
        category = NewsCategory.PAKISTAN,
        language = "EN",
        isIslamicSource = false,
        isScholarlySource = false
    )

    fun createSampleEntities(baseTime: Long = 1_760_000_000_000L): List<ArticleEntity> {
        return listOf(
            ArticleEntity(
                id = "isl_1",
                normalizedUrl = "saudigazette.com.sa/article/101",
                normalizedTitle = "hajj smart shuttle services expand across makkah",
                title = "Hajj Smart Shuttle Services Expand Across Makkah",
                summary = "The Ministry of Hajj and Umrah inaugurated electric shuttle routes for pilgrims around Masjid al-Haram.",
                category = NewsCategory.ISLAMIC.name,
                islamicSection = IslamicSection.HAJJ_UMRAH.name,
                imageUrl = "https://saudigazette.com.sa/uploads/hajj.webp",
                sourceName = "Saudi Gazette",
                sourceUrl = "https://saudigazette.com.sa/article/101",
                publishedAt = baseTime - 1_000L,
                isIslamicSource = false,
                isScholarlySource = false
            ),
            ArticleEntity(
                id = "isl_2",
                normalizedUrl = "seekersguidance.org/answers/fiqh-ruling-zakat",
                normalizedTitle = "fiqh ruling on calculating annual zakat on savings",
                title = "Fiqh Ruling on Calculating Annual Zakat on Savings",
                summary = "Answered by Shaykh Faraz Rabbani regarding the nisab threshold and lunar year calculation for Zakat.",
                category = NewsCategory.ISLAMIC.name,
                islamicSection = IslamicSection.FATWA_SCHOLARS.name,
                imageUrl = "",
                sourceName = "SeekersGuidance",
                sourceUrl = "https://seekersguidance.org/answers/fiqh-ruling-zakat",
                publishedAt = baseTime - 2_000L,
                isIslamicSource = true,
                isScholarlySource = true
            ),
            ArticleEntity(
                id = "isl_3",
                normalizedUrl = "aa.com.tr/en/oic-summit-jeddah",
                normalizedTitle = "oic summit in jeddah calls for immediate humanitarian aid",
                title = "OIC Summit in Jeddah Calls for Immediate Humanitarian Aid",
                summary = "Delegates gathered in Jeddah to coordinate relief corridors.",
                category = NewsCategory.ISLAMIC.name,
                islamicSection = IslamicSection.ISLAMIC_EVENTS.name,
                imageUrl = "https://www.aa.com.tr/uploads/oic.jpg",
                sourceName = "Anadolu Agency",
                sourceUrl = "https://www.aa.com.tr/en/oic-summit-jeddah",
                publishedAt = baseTime - 3_000L,
                isIslamicSource = false,
                isScholarlySource = false
            ),
            ArticleEntity(
                id = "isl_4",
                normalizedUrl = "aboutislam.net/ramadan-preparations-guide",
                normalizedTitle = "community mosques prepare for ramadan taraweeh prayers",
                title = "Community Mosques Prepare for Ramadan Taraweeh Prayers",
                summary = "Mosques across major cities have announced schedules for Ramadan community iftars and Taraweeh.",
                category = NewsCategory.ISLAMIC.name,
                islamicSection = IslamicSection.RAMADAN_EID.name,
                imageUrl = "https://aboutislam.net/uploads/ramadan.jpg",
                sourceName = "AboutIslam",
                sourceUrl = "https://aboutislam.net/ramadan-preparations-guide",
                publishedAt = baseTime - 4_000L,
                isIslamicSource = true,
                isScholarlySource = true
            ),
            ArticleEntity(
                id = "isl_5",
                normalizedUrl = "5pillarsuk.com/muslim-world-charity-drive",
                normalizedTitle = "global muslim relief agencies launch winter appeal",
                title = "Global Muslim Relief Agencies Launch Winter Appeal",
                summary = "Charities have coordinated emergency winter supplies for displaced families.",
                category = NewsCategory.ISLAMIC.name,
                islamicSection = IslamicSection.MUSLIM_WORLD.name,
                imageUrl = "",
                sourceName = "5Pillars",
                sourceUrl = "https://5pillarsuk.com/muslim-world-charity-drive",
                publishedAt = baseTime - 5_000L,
                isIslamicSource = true,
                isScholarlySource = false
            ),
            ArticleEntity(
                id = "pak_1",
                normalizedUrl = "tribune.com.pk/story/psx-rally",
                normalizedTitle = "pakistan stock exchange posts strong weekly gains",
                title = "Pakistan Stock Exchange Posts Strong Weekly Gains",
                summary = "KSE-100 index climbed amid strong institutional participation in Karachi.",
                category = NewsCategory.PAKISTAN.name,
                islamicSection = null,
                imageUrl = "https://tribune.com.pk/uploads/psx.jpg",
                sourceName = "The Express Tribune",
                sourceUrl = "https://tribune.com.pk/story/psx-rally",
                publishedAt = baseTime - 6_000L,
                isIslamicSource = false,
                isScholarlySource = false
            )
        )
    }
}
