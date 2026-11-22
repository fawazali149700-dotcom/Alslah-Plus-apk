package com.example.data.repository

import android.os.Build
import com.example.data.model.HijriDateModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

object HijriCalendarHelper {

    val hijriMonthsAr = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val arabicWeekdays = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الإثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    fun todayHijri(): HijriDateModel {
        val cal = Calendar.getInstance()
        return gregorianToHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun gregorianToHijri(year: Int, month: Int, day: Int): HijriDateModel {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val weekdayAr = arabicWeekdays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return try {
                val localDate = LocalDate.of(year, month, day)
                val hijrahDate = HijrahChronology.INSTANCE.date(localDate)
                val hYear = hijrahDate.get(ChronoField.YEAR)
                val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
                val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)
                HijriDateModel(
                    day = hDay,
                    month = hMonth,
                    year = hYear,
                    monthNameAr = hijriMonthsAr.getOrElse(hMonth - 1) { "" },
                    weekdayAr = weekdayAr
                )
            } catch (_: Exception) {
                tabularGregorianToHijri(year, month, day, weekdayAr)
            }
        }
        return tabularGregorianToHijri(year, month, day, weekdayAr)
    }

    fun hijriToGregorian(hYear: Int, hMonth: Int, hDay: Int): Triple<Int, Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijrahDate = HijrahDate.of(hYear, hMonth, hDay)
                val localDate = LocalDate.from(hijrahDate)
                return Triple(localDate.year, localDate.monthValue, localDate.dayOfMonth)
            } catch (_: Exception) {
                // Fallback if day exceeds month length
            }
        }
        return tabularHijriToGregorian(hYear, hMonth, hDay)
    }

    fun formatGregorianDateAr(date: Date = Date()): String {
        val formatter = SimpleDateFormat("d MMMM yyyy م", Locale("ar"))
        return formatter.format(date)
    }

    fun formatGregorianTripleAr(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance().apply { set(year, month - 1, day) }
        val formatter = SimpleDateFormat("EEEE، d MMMM yyyy م", Locale("ar"))
        return formatter.format(cal.time)
    }

    private fun tabularGregorianToHijri(
        gYear: Int,
        gMonth: Int,
        gDay: Int,
        weekdayAr: String
    ): HijriDateModel {
        var m = gMonth
        var y = gYear
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0).toInt()
        val b = 2 - a + floor(a / 4.0).toInt()
        val jd = floor(365.25 * (y + 4716)).toInt() +
            floor(30.6001 * (m + 1)).toInt() + gDay + b - 1524

        val l = jd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0).toInt()
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316.0).toInt()) * (floor((50 * l2) / 17719.0).toInt()) +
            (floor(l2 / 5670.0).toInt()) * (floor((43 * l2) / 15238.0).toInt())
        val l3 = l2 - (floor((30 - j) / 15.0).toInt()) * (floor((17719 * j) / 50.0).toInt()) -
            (floor(j / 16.0).toInt()) * (floor((15238 * j) / 43.0).toInt()) + 29
        val hMonth = floor((24 * l3) / 709.0).toInt()
        val hDay = l3 - floor((709 * hMonth) / 24.0).toInt()
        val hYear = 30 * n + j - 30

        return HijriDateModel(
            day = hDay,
            month = hMonth,
            year = hYear,
            monthNameAr = hijriMonthsAr.getOrElse(hMonth - 1) { "" },
            weekdayAr = weekdayAr
        )
    }

    private fun tabularHijriToGregorian(hYear: Int, hMonth: Int, hDay: Int): Triple<Int, Int, Int> {
        val jd = floor((11 * hYear + 3) / 30.0).toInt() +
            354 * hYear +
            30 * hMonth -
            floor((hMonth - 1) / 2.0).toInt() +
            hDay + 1948440 - 385

        var l = jd + 68569
        val n = floor((4 * l) / 146097.0).toInt()
        l -= floor((146097 * n + 3) / 4.0).toInt()
        val i = floor((4000 * (l + 1)) / 1461001.0).toInt()
        l = l - floor((1461 * i) / 4.0).toInt() + 31
        val j = floor((80 * l) / 2447.0).toInt()
        val day = l - floor((2447 * j) / 80.0).toInt()
        l = floor(j / 11.0).toInt()
        val month = j + 2 - 12 * l
        val year = 100 * (n - 49) + i + l
        return Triple(year, month, day)
    }
}
