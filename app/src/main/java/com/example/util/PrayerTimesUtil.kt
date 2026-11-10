package com.example.util

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.model.AppLanguage
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Date
import java.util.Locale

data class CityPrayerInfo(
    val id: String,
    val nameEn: String,
    val nameUr: String,
    val nameAr: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
    val isPakistan: Boolean
) {
    fun getName(lang: AppLanguage): String = when (lang) {
        AppLanguage.URDU -> nameUr
        AppLanguage.ARABIC -> nameAr
        else -> nameEn
    }

    val name: String get() = nameEn
}

data class DailyPrayerSchedule(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fajrTime: ZonedDateTime,
    val sunriseTime: ZonedDateTime,
    val dhuhrTime: ZonedDateTime,
    val asrTime: ZonedDateTime,
    val maghribTime: ZonedDateTime,
    val ishaTime: ZonedDateTime
)

data class NextPrayer(
    val prayerName: String,
    val timeFormatted: String,
    val minutesRemaining: Long
)

object PrayerTimesUtil {

    val hijriAdjustments = listOf(-2, -1, 0, 1, 2)

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    private val urduArabicDigits = mapOf(
        '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
        '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
    )

    fun toEasternDigits(number: Any): String {
        return number.toString().map { urduArabicDigits[it] ?: it }.joinToString("")
    }

    private val islamicMonths = mapOf(
        AppLanguage.ENGLISH to listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
        ),
        AppLanguage.URDU to listOf(
            "محرم الحرام", "صفر المظفر", "ربیع الاول", "ربیع الثانی",
            "جمادی الاول", "جمادی الثانی", "رجب المرجب", "شعبان المعظم",
            "رمضان المبارک", "شوال المکرم", "ذوالقعدۃ الحرام", "ذوالحجۃ الحرام"
        ),
        AppLanguage.ARABIC to listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        ),
        AppLanguage.TURKISH to listOf(
            "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir",
            "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban",
            "Ramazan", "Şevval", "Zilkade", "Zilhicce"
        ),
        AppLanguage.HINDI to listOf(
            "मुहर्रम", "सफ़र", "रबीउल अव्वल", "रबीउस सानी",
            "जुमादा अल-अव्वल", "जुमादा अल-सानी", "रजब", "शाबान",
            "रमजान", "शव्वाल", "ज़ुल-क़ादा", "ज़ुल-हिज्जा"
        )
    )

    fun getTodayHijriDate(
        lang: AppLanguage = AppLanguage.ENGLISH,
        adjustmentDays: Int = 0,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): String {
        return try {
            val localDate = nowInstant.atZone(zoneId).toLocalDate()
            formatHijriDateForLocalDate(localDate, adjustmentDays, lang)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatHijriDateForLocalDate(
        localDate: LocalDate,
        adjustmentDays: Int = 0,
        lang: AppLanguage = AppLanguage.ENGLISH
    ): String {
        return try {
            val adjustedDate = localDate.plusDays(adjustmentDays.toLong())
            val hijrahDate = HijrahDate.from(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)

            val monthNames = islamicMonths[lang] ?: islamicMonths[AppLanguage.ENGLISH]!!
            val monthName = monthNames.getOrNull(month - 1) ?: return ""

            when (lang) {
                AppLanguage.URDU -> "${toEasternDigits(day)} $monthName ${toEasternDigits(year)}ھ"
                AppLanguage.ARABIC -> "${toEasternDigits(day)} $monthName ${toEasternDigits(year)} هـ"
                else -> "$day $monthName $year AH"
            }
        } catch (_: Exception) {
            ""
        }
    }

    private val karachiZone: ZoneId = ZoneId.of("Asia/Karachi")
    private val riyadhZone: ZoneId = ZoneId.of("Asia/Riyadh")

    val supportedCities = listOf(
        CityPrayerInfo(
            id = "ISB",
            nameEn = "Islamabad",
            nameUr = "اسلام آباد",
            nameAr = "إسلام آباد",
            latitude = 33.6844,
            longitude = 73.0479,
            zoneId = karachiZone,
            isPakistan = true
        ),
        CityPrayerInfo(
            id = "LHE",
            nameEn = "Lahore",
            nameUr = "لاہور",
            nameAr = "لاهور",
            latitude = 31.5204,
            longitude = 74.3587,
            zoneId = karachiZone,
            isPakistan = true
        ),
        CityPrayerInfo(
            id = "KHI",
            nameEn = "Karachi",
            nameUr = "کراچی",
            nameAr = "كراتشي",
            latitude = 24.8607,
            longitude = 67.0011,
            zoneId = karachiZone,
            isPakistan = true
        ),
        CityPrayerInfo(
            id = "PEW",
            nameEn = "Peshawar",
            nameUr = "پشاور",
            nameAr = "بيشاور",
            latitude = 34.0151,
            longitude = 71.5249,
            zoneId = karachiZone,
            isPakistan = true
        ),
        CityPrayerInfo(
            id = "QTA",
            nameEn = "Quetta",
            nameUr = "کوئٹہ",
            nameAr = "كويتا",
            latitude = 30.1798,
            longitude = 66.9750,
            zoneId = karachiZone,
            isPakistan = true
        ),
        CityPrayerInfo(
            id = "MAK",
            nameEn = "Makkah",
            nameUr = "مکہ مکرمہ",
            nameAr = "مكة المكرمة",
            latitude = 21.4225,
            longitude = 39.8262,
            zoneId = riyadhZone,
            isPakistan = false
        ),
        CityPrayerInfo(
            id = "MED",
            nameEn = "Madinah",
            nameUr = "مدینہ منورہ",
            nameAr = "المدينة المنورة",
            latitude = 24.4672,
            longitude = 39.6112,
            zoneId = riyadhZone,
            isPakistan = false
        )
    )

    fun findCityById(id: String?): CityPrayerInfo {
        return supportedCities.firstOrNull { it.id == id } ?: supportedCities[0]
    }

    private fun buildCalculationParameters(city: CityPrayerInfo): CalculationParameters {
        return if (city.isPakistan) {
            CalculationMethod.KARACHI.parameters.apply {
                madhab = Madhab.HANAFI
            }
        } else {
            CalculationMethod.UMM_AL_QURA.parameters
        }
    }

    private fun Date.toCityZonedDateTime(zoneId: ZoneId): ZonedDateTime {
        return Instant.ofEpochMilli(this.time).atZone(zoneId)
    }

    fun getDailySchedule(
        city: CityPrayerInfo,
        date: LocalDate = LocalDate.now(city.zoneId)
    ): DailyPrayerSchedule {
        val coordinates = Coordinates(city.latitude, city.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = buildCalculationParameters(city)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        val fajrZdt = prayerTimes.fajr.toCityZonedDateTime(city.zoneId)
        val sunriseZdt = prayerTimes.sunrise.toCityZonedDateTime(city.zoneId)
        val dhuhrZdt = prayerTimes.dhuhr.toCityZonedDateTime(city.zoneId)
        val asrZdt = prayerTimes.asr.toCityZonedDateTime(city.zoneId)
        val maghribZdt = prayerTimes.maghrib.toCityZonedDateTime(city.zoneId)
        val ishaZdt = prayerTimes.isha.toCityZonedDateTime(city.zoneId)

        return DailyPrayerSchedule(
            fajr = fajrZdt.format(timeFormatter),
            sunrise = sunriseZdt.format(timeFormatter),
            dhuhr = dhuhrZdt.format(timeFormatter),
            asr = asrZdt.format(timeFormatter),
            maghrib = maghribZdt.format(timeFormatter),
            isha = ishaZdt.format(timeFormatter),
            fajrTime = fajrZdt,
            sunriseTime = sunriseZdt,
            dhuhrTime = dhuhrZdt,
            asrTime = asrZdt,
            maghribTime = maghribZdt,
            ishaTime = ishaZdt
        )
    }

    private val prayerTranslations = mapOf(
        "Fajr" to mapOf(AppLanguage.ENGLISH to "Fajr", AppLanguage.URDU to "فجر", AppLanguage.ARABIC to "الفجر", AppLanguage.TURKISH to "İmsak", AppLanguage.HINDI to "फज्र"),
        "Sunrise" to mapOf(AppLanguage.ENGLISH to "Sunrise", AppLanguage.URDU to "طلوعِ آفتاب", AppLanguage.ARABIC to "الشروق", AppLanguage.TURKISH to "Güneş", AppLanguage.HINDI to "सूर्योदय"),
        "Dhuhr" to mapOf(AppLanguage.ENGLISH to "Dhuhr", AppLanguage.URDU to "ظہر", AppLanguage.ARABIC to "الظهر", AppLanguage.TURKISH to "Öğle", AppLanguage.HINDI to "जुहर"),
        "Asr" to mapOf(AppLanguage.ENGLISH to "Asr", AppLanguage.URDU to "عصر", AppLanguage.ARABIC to "العصر", AppLanguage.TURKISH to "İkindi", AppLanguage.HINDI to "असर"),
        "Maghrib" to mapOf(AppLanguage.ENGLISH to "Maghrib", AppLanguage.URDU to "مغرب", AppLanguage.ARABIC to "المغرب", AppLanguage.TURKISH to "Akşam", AppLanguage.HINDI to "मग़रिब"),
        "Isha" to mapOf(AppLanguage.ENGLISH to "Isha", AppLanguage.URDU to "عشاء", AppLanguage.ARABIC to "العشاء", AppLanguage.TURKISH to "Yatsı", AppLanguage.HINDI to "ईशा")
    )

    fun getPrayerName(prayerKey: String, lang: AppLanguage): String {
        return prayerTranslations[prayerKey]?.get(lang) ?: prayerKey
    }

    /**
     * Calculates the next obligatory prayer (Fajr, Dhuhr, Asr, Maghrib, or Isha — never Sunrise)
     * using the selected city's ZoneId. If today's Isha has passed in the city, returns
     * tomorrow's Fajr calculated for tomorrow's date.
     */
    fun getNextPrayer(
        city: CityPrayerInfo,
        lang: AppLanguage = AppLanguage.ENGLISH,
        nowInstant: Instant = Instant.now()
    ): NextPrayer {
        val cityNow = nowInstant.atZone(city.zoneId)
        val todayDate = cityNow.toLocalDate()
        val todaySchedule = getDailySchedule(city, todayDate)

        // Only obligatory five prayers — Sunrise is excluded
        val obligatoryPrayers = listOf(
            "Fajr" to todaySchedule.fajrTime,
            "Dhuhr" to todaySchedule.dhuhrTime,
            "Asr" to todaySchedule.asrTime,
            "Maghrib" to todaySchedule.maghribTime,
            "Isha" to todaySchedule.ishaTime
        )

        for ((prayerKey, prayerZdt) in obligatoryPrayers) {
            if (prayerZdt.isAfter(cityNow)) {
                val rawTime = prayerZdt.format(timeFormatter)
                val formattedTime = if (lang == AppLanguage.URDU || lang == AppLanguage.ARABIC) {
                    toEasternDigits(rawTime)
                } else {
                    rawTime
                }
                val minsRemaining = Duration.between(cityNow, prayerZdt).toMinutes().coerceAtLeast(0)
                return NextPrayer(
                    prayerName = getPrayerName(prayerKey, lang),
                    timeFormatted = formattedTime,
                    minutesRemaining = minsRemaining
                )
            }
        }

        // After Isha: next prayer is tomorrow's Fajr calculated for tomorrow's date
        val tomorrowSchedule = getDailySchedule(city, todayDate.plusDays(1))
        val tomorrowFajrZdt = tomorrowSchedule.fajrTime
        val rawTime = tomorrowFajrZdt.format(timeFormatter)
        val formattedTime = if (lang == AppLanguage.URDU || lang == AppLanguage.ARABIC) {
            toEasternDigits(rawTime)
        } else {
            rawTime
        }
        val minsRemaining = Duration.between(cityNow, tomorrowFajrZdt).toMinutes().coerceAtLeast(0)
        return NextPrayer(
            prayerName = getPrayerName("Fajr", lang),
            timeFormatted = formattedTime,
            minutesRemaining = minsRemaining
        )
    }

    fun formatRelativeTime(
        timestamp: Long,
        lang: AppLanguage = AppLanguage.ENGLISH,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val diffMillis = (nowMillis - timestamp).coerceAtLeast(0L)
        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when (lang) {
            AppLanguage.URDU -> when {
                diffMinutes < 1 -> "ابھی ابھی"
                diffMinutes < 60 -> "${toEasternDigits(diffMinutes)} منٹ قبل"
                diffHours < 24 -> "${toEasternDigits(diffHours)} گھنٹے قبل"
                diffDays == 1L -> "کل"
                diffDays < 7 -> "${toEasternDigits(diffDays)} دن قبل"
                else -> "${toEasternDigits(diffDays / 7)} ہفتے قبل"
            }
            AppLanguage.ARABIC -> when {
                diffMinutes < 1 -> "الآن"
                diffMinutes < 60 -> "منذ ${toEasternDigits(diffMinutes)} دقيقة"
                diffHours < 24 -> "منذ ${toEasternDigits(diffHours)} ساعة"
                diffDays == 1L -> "أمس"
                diffDays < 7 -> "منذ ${toEasternDigits(diffDays)} أيام"
                else -> "منذ ${toEasternDigits(diffDays / 7)} أسابيع"
            }
            AppLanguage.TURKISH -> when {
                diffMinutes < 1 -> "Az önce"
                diffMinutes < 60 -> "${diffMinutes} dk önce"
                diffHours == 1L -> "1 saat önce"
                diffHours < 24 -> "${diffHours} saat önce"
                diffDays == 1L -> "Dün"
                diffDays < 7 -> "${diffDays} gün önce"
                else -> "${diffDays / 7} hafta önce"
            }
            AppLanguage.HINDI -> when {
                diffMinutes < 1 -> "अभी-अभी"
                diffMinutes < 60 -> "${diffMinutes} मिनट पहले"
                diffHours < 24 -> "${diffHours} घंटे पहले"
                diffDays == 1L -> "कल"
                diffDays < 7 -> "${diffDays} दिन पहले"
                else -> "${diffDays / 7} सप्ताह पहले"
            }
            AppLanguage.ENGLISH -> when {
                diffMinutes < 1 -> "Just now"
                diffMinutes < 60 -> "${diffMinutes}m ago"
                diffHours == 1L -> "1 hour ago"
                diffHours < 24 -> "${diffHours} hours ago"
                diffDays == 1L -> "Yesterday"
                diffDays < 7 -> "${diffDays} days ago"
                else -> "${diffDays / 7} weeks ago"
            }
        }
    }
}
