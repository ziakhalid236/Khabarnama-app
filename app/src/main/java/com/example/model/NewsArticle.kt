package com.example.model

enum class AppLanguage(
    val displayName: String,
    val code: String,
    val isRtl: Boolean,
    val flagEmoji: String,
    val nativeName: String,
    val regionLabel: String
) {
    ENGLISH("English", "EN", false, "🇬🇧", "English", "Global / UK"),
    URDU("اردو (Urdu)", "UR", true, "🇵🇰", "اردو", "Pakistan"),
    ARABIC("العربية (Arabic)", "AR", true, "🇸🇦", "العربية", "Arab World"),
    TURKISH("Türkçe", "TR", false, "🇹🇷", "Türkçe", "Türkiye"),
    HINDI("हिन्दी (Hindi)", "HI", false, "🇮🇳", "हिन्दी", "India");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: ENGLISH
        }
    }
}

enum class NewsCategory(
    val englishLabel: String,
    val urduLabel: String,
    val arabicLabel: String,
    val turkishLabel: String,
    val hindiLabel: String
) {
    ALL("All", "تمام", "الكل", "Tümü", "सभी"),
    ISLAMIC("Islamic", "اسلامی", "إسلامي", "İslami", "इस्लामी"),
    PAKISTAN("Pakistan", "پاکستان", "باكستان", "Pakistan", "पाकिस्तान"),
    WORLD("World", "دنیا", "العالم", "Dünya", "विश्व"),
    BUSINESS("Business", "کاروبار", "الأعمال", "Ekonomi", "व्यापार"),
    SPORTS("Sports", "کھیل", "الرياضة", "Spor", "खेल"),
    TECHNOLOGY("Technology", "ٹیکنالوجی", "التكنولوجيا", "Teknoloji", "तकनीक");

    fun getLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> englishLabel
        AppLanguage.URDU -> urduLabel
        AppLanguage.ARABIC -> arabicLabel
        AppLanguage.TURKISH -> turkishLabel
        AppLanguage.HINDI -> hindiLabel
    }

    companion object {
        fun fromString(value: String): NewsCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}

enum class IslamicSection(
    val englishLabel: String,
    val urduLabel: String,
    val arabicLabel: String,
    val turkishLabel: String,
    val hindiLabel: String
) {
    ALL("All Islamic", "تمام اسلامی", "كل الإسلاميات", "Tüm İslami", "सभी इस्लामी"),
    MUSLIM_WORLD("Muslim World", "مسلم امہ", "العالم الإسلامي", "İslam Dünyası", "मुस्लिम जगत"),
    HAJJ_UMRAH("Hajj & Umrah", "حج و عمرہ", "الحج والعمرة", "Hac ve Umre", "हज और उमरा"),
    ISLAMIC_EVENTS("Islamic Events", "اسلامی تقریبات", "المناسبات الإسلامية", "İslami Etkinlikler", "इस्लामी आयोजन"),
    FATWA_SCHOLARS("Fatwa & Scholars", "بیانات و فتاویٰ", "الفتاوى والعلماء", "Fetva ve Alimler", "फतवा और विद्वान"),
    RAMADAN_EID("Ramadan & Eid", "رمضان و عید", "رمضان والعيد", "Ramazan ve Bayram", "रमजान और ईद");

    fun getLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> englishLabel
        AppLanguage.URDU -> urduLabel
        AppLanguage.ARABIC -> arabicLabel
        AppLanguage.TURKISH -> turkishLabel
        AppLanguage.HINDI -> hindiLabel
    }

    companion object {
        fun fromString(value: String?): IslamicSection? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

data class NewsSourceConfig(
    val name: String,
    val feedUrl: String,
    val category: NewsCategory,
    val language: String,
    val isIslamicSource: Boolean = false,
    val isScholarlySource: Boolean = false
)

data class LocalizedContent(
    val title: String,
    val summary: String
)

data class NewsArticle(
    val id: String,
    val translations: Map<AppLanguage, LocalizedContent>,
    val category: NewsCategory,
    val islamicSection: IslamicSection? = null,
    val imageUrl: String = "",
    val sourceName: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val isFeaturedIslamic: Boolean = false,
    val isIslamicSource: Boolean = false,
    val isScholarlySource: Boolean = false,
    val isBreaking: Boolean = false,
    val readTimeMinutes: Int = 3,
    val isTranslating: Boolean = false,
    val translationFailed: Boolean = false
) {
    val hasValidImage: Boolean
        get() = imageUrl.isNotBlank() &&
            (imageUrl.startsWith("https://", ignoreCase = true) || imageUrl.startsWith("http://", ignoreCase = true))

    fun hasTranslation(lang: AppLanguage): Boolean {
        return lang == AppLanguage.ENGLISH || translations.containsKey(lang)
    }

    fun shouldShowOriginalLanguageLabel(lang: AppLanguage): Boolean {
        return lang != AppLanguage.ENGLISH && !isTranslating && (translationFailed || !translations.containsKey(lang))
    }

    // Convenience helper to get title in user's selected language, falling back to English
    fun getTitle(lang: AppLanguage): String {
        return translations[lang]?.title?.takeIf { it.isNotBlank() }
            ?: translations[AppLanguage.ENGLISH]?.title
            ?: ""
    }

    // Convenience helper to get summary in user's selected language, falling back to English
    fun getSummary(lang: AppLanguage): String {
        return translations[lang]?.summary?.takeIf { it.isNotBlank() }
            ?: translations[AppLanguage.ENGLISH]?.summary
            ?: ""
    }

    // Fallback getters for original English text
    val title: String get() = getTitle(AppLanguage.ENGLISH)
    val summary: String get() = getSummary(AppLanguage.ENGLISH)
}
