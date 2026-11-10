package com.example

import com.example.model.AppLanguage
import com.example.util.PrayerTimesUtil
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesUtilTest {

    @Test
    fun testCitiesAndCalculationMethods() {
        assertEquals(7, PrayerTimesUtil.supportedCities.size)
        val expectedIds = listOf("ISB", "LHE", "KHI", "PEW", "QTA", "MAK", "MED")
        assertEquals(expectedIds, PrayerTimesUtil.supportedCities.map { it.id })

        val isb = PrayerTimesUtil.findCityById("ISB")
        val mak = PrayerTimesUtil.findCityById("MAK")

        assertEquals("Islamabad", isb.name)
        assertEquals(ZoneId.of("Asia/Karachi"), isb.zoneId)
        assertTrue(isb.isPakistan)

        assertEquals("Makkah", mak.name)
        assertEquals(ZoneId.of("Asia/Riyadh"), mak.zoneId)
        assertFalse(mak.isPakistan)

        // Verify chronological ordering for all 7 cities in both summer and winter
        for (city in PrayerTimesUtil.supportedCities) {
            for (date in listOf(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 21))) {
                val schedule = PrayerTimesUtil.getDailySchedule(city, date)
                assertTrue(schedule.fajrTime.isBefore(schedule.sunriseTime))
                assertTrue(schedule.sunriseTime.isBefore(schedule.dhuhrTime))
                assertTrue(schedule.dhuhrTime.isBefore(schedule.asrTime))
                assertTrue(schedule.asrTime.isBefore(schedule.maghribTime))
                assertTrue(schedule.maghribTime.isBefore(schedule.ishaTime))
            }
        }
    }

    @Test
    fun testSunriseNeverReturnedAsNextPrayer() {
        val isb = PrayerTimesUtil.findCityById("ISB")
        val date = LocalDate.of(2026, 6, 10)
        val schedule = PrayerTimesUtil.getDailySchedule(isb, date)

        // Pick an instant 5 minutes after Fajr (which is before Sunrise)
        val afterFajrBeforeSunrise = schedule.fajrTime.plusMinutes(5).toInstant()
        val nextPrayer = PrayerTimesUtil.getNextPrayer(isb, AppLanguage.ENGLISH, afterFajrBeforeSunrise)

        // Next prayer must be Dhuhr, never Sunrise
        assertNotEquals("Sunrise", nextPrayer.prayerName)
        assertEquals("Dhuhr", nextPrayer.prayerName)
        assertEquals(schedule.dhuhr, nextPrayer.timeFormatted)
    }

    @Test
    fun testNextPrayerAcrossMidnightUsesTomorrowsFajr() {
        val lhe = PrayerTimesUtil.findCityById("LHE")
        val today = LocalDate.of(2026, 3, 20)
        val todaySchedule = PrayerTimesUtil.getDailySchedule(lhe, today)
        val tomorrowSchedule = PrayerTimesUtil.getDailySchedule(lhe, today.plusDays(1))

        // 30 minutes after today's Isha (before midnight)
        val afterIsha = todaySchedule.ishaTime.plusMinutes(30)
        val nextAfterIsha = PrayerTimesUtil.getNextPrayer(lhe, AppLanguage.ENGLISH, afterIsha.toInstant())

        assertEquals("Fajr", nextAfterIsha.prayerName)
        assertEquals(tomorrowSchedule.fajr, nextAfterIsha.timeFormatted)

        val expectedMinutes = Duration.between(afterIsha, tomorrowSchedule.fajrTime).toMinutes()
        assertEquals(expectedMinutes, nextAfterIsha.minutesRemaining)
        assertTrue(nextAfterIsha.minutesRemaining > 0)
    }

    @Test
    fun testCityTimeZoneHandling() {
        val khi = PrayerTimesUtil.findCityById("KHI") // Asia/Karachi (UTC+5)
        val mak = PrayerTimesUtil.findCityById("MAK") // Asia/Riyadh (UTC+3)

        // At 12:30 PM PKT (07:30 AM UTC), it is 10:30 AM in Makkah (before Dhuhr in Makkah)
        val instant = ZonedDateTime.of(2026, 4, 15, 10, 30, 0, 0, ZoneId.of("Asia/Riyadh")).toInstant()

        val makNext = PrayerTimesUtil.getNextPrayer(mak, AppLanguage.ENGLISH, instant)
        val khiNext = PrayerTimesUtil.getNextPrayer(khi, AppLanguage.ENGLISH, instant)

        // In Makkah at 10:30 AM local time, next prayer is Dhuhr
        assertEquals("Dhuhr", makNext.prayerName)
        assertNotNull(khiNext.prayerName)
        assertNotEquals(mak.zoneId, khi.zoneId)
    }

    @Test
    fun testHijriDateAdjustment() {
        val baseDate = LocalDate.of(2026, 4, 10)
        val hijriZero = PrayerTimesUtil.formatHijriDateForLocalDate(baseDate, adjustmentDays = 0)
        val hijriMinusOne = PrayerTimesUtil.formatHijriDateForLocalDate(baseDate, adjustmentDays = -1)
        val hijriMinusTwo = PrayerTimesUtil.formatHijriDateForLocalDate(baseDate, adjustmentDays = -2)
        val hijriPlusOne = PrayerTimesUtil.formatHijriDateForLocalDate(baseDate, adjustmentDays = 1)
        val hijriPlusTwo = PrayerTimesUtil.formatHijriDateForLocalDate(baseDate, adjustmentDays = 2)

        assertTrue(hijriZero.isNotBlank())
        assertTrue(hijriZero.endsWith("AH"))
        assertNotEquals(hijriZero, hijriMinusOne)
        assertNotEquals(hijriZero, hijriPlusOne)
        assertEquals(
            PrayerTimesUtil.formatHijriDateForLocalDate(baseDate.minusDays(1), adjustmentDays = 0),
            hijriMinusOne
        )
        assertEquals(
            PrayerTimesUtil.formatHijriDateForLocalDate(baseDate.minusDays(2), adjustmentDays = 0),
            hijriMinusTwo
        )
        assertEquals(
            PrayerTimesUtil.formatHijriDateForLocalDate(baseDate.plusDays(1), adjustmentDays = 0),
            hijriPlusOne
        )
        assertEquals(
            PrayerTimesUtil.formatHijriDateForLocalDate(baseDate.plusDays(2), adjustmentDays = 0),
            hijriPlusTwo
        )
    }

    @Test
    fun testRelativeTimeAcrossBucketsAndLanguages() {
        val now = 1_760_000_000_000L
        val minuteMs = 60_000L
        val hourMs = 60 * minuteMs
        val dayMs = 24 * hourMs

        // English buckets
        assertEquals("Just now", PrayerTimesUtil.formatRelativeTime(now - 20_000L, AppLanguage.ENGLISH, now))
        assertEquals("15m ago", PrayerTimesUtil.formatRelativeTime(now - 15 * minuteMs, AppLanguage.ENGLISH, now))
        assertEquals("1 hour ago", PrayerTimesUtil.formatRelativeTime(now - 1 * hourMs, AppLanguage.ENGLISH, now))
        assertEquals("5 hours ago", PrayerTimesUtil.formatRelativeTime(now - 5 * hourMs, AppLanguage.ENGLISH, now))
        assertEquals("Yesterday", PrayerTimesUtil.formatRelativeTime(now - 1 * dayMs, AppLanguage.ENGLISH, now))
        assertEquals("4 days ago", PrayerTimesUtil.formatRelativeTime(now - 4 * dayMs, AppLanguage.ENGLISH, now))
        assertEquals("2 weeks ago", PrayerTimesUtil.formatRelativeTime(now - 14 * dayMs, AppLanguage.ENGLISH, now))

        // Urdu buckets with Eastern digits
        assertEquals("ابھی ابھی", PrayerTimesUtil.formatRelativeTime(now - 10_000L, AppLanguage.URDU, now))
        assertEquals("۱۵ منٹ قبل", PrayerTimesUtil.formatRelativeTime(now - 15 * minuteMs, AppLanguage.URDU, now))
        assertEquals("۲ گھنٹے قبل", PrayerTimesUtil.formatRelativeTime(now - 2 * hourMs, AppLanguage.URDU, now))
        assertEquals("کل", PrayerTimesUtil.formatRelativeTime(now - 1 * dayMs, AppLanguage.URDU, now))

        // Arabic, Turkish, Hindi
        assertEquals("الآن", PrayerTimesUtil.formatRelativeTime(now, AppLanguage.ARABIC, now))
        assertEquals("Az önce", PrayerTimesUtil.formatRelativeTime(now, AppLanguage.TURKISH, now))
        assertEquals("अभी-अभी", PrayerTimesUtil.formatRelativeTime(now, AppLanguage.HINDI, now))
    }
}
