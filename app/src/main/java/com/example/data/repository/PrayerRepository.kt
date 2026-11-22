package com.example.data.repository

import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.SunnahTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerItem
import com.example.data.model.PrayerName
import com.example.data.model.UserSettings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerRepository {

    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale("ar"))

    fun computeSchedule(settings: UserSettings, nowMillis: Long = System.currentTimeMillis()): DailyPrayerSchedule {
        val coordinates = Coordinates(settings.latitude, settings.longitude)
        val params = settings.calculationMethod.adhanMethod.parameters.apply {
            madhab = settings.madhab.adhanMadhab
        }

        val todayDate = Date(nowMillis)
        val todayComponents = DateComponents.from(todayDate)
        val prayerTimes = PrayerTimes(coordinates, todayComponents, params)
        val sunnahTimes = SunnahTimes(prayerTimes)

        val nextEnum = prayerTimes.nextPrayer(todayDate)

        val rawList = listOf(
            PrayerName.FAJR to prayerTimes.fajr,
            PrayerName.SUNRISE to prayerTimes.sunrise,
            PrayerName.DHUHR to prayerTimes.dhuhr,
            PrayerName.ASR to prayerTimes.asr,
            PrayerName.MAGHRIB to prayerTimes.maghrib,
            PrayerName.ISHA to prayerTimes.isha
        )

        fun mapPrayerEnum(name: PrayerName): Prayer = when (name) {
            PrayerName.FAJR -> Prayer.FAJR
            PrayerName.SUNRISE -> Prayer.SUNRISE
            PrayerName.DHUHR -> Prayer.DHUHR
            PrayerName.ASR -> Prayer.ASR
            PrayerName.MAGHRIB -> Prayer.MAGHRIB
            PrayerName.ISHA -> Prayer.ISHA
        }

        val items = rawList.map { (name, date) ->
            val adhanMillis = date.time
            val offsetMins = settings.iqamahOffsets.offsetFor(name)
            val iqamahMillis = if (name.isObligatory && offsetMins > 0) {
                adhanMillis + offsetMins * 60_000L
            } else {
                null
            }
            val isNext = nextEnum == mapPrayerEnum(name)
            val isPassed = adhanMillis <= nowMillis
            PrayerItem(
                prayer = name,
                adhanTimeMillis = adhanMillis,
                iqamahTimeMillis = iqamahMillis,
                iqamahOffsetMinutes = offsetMins,
                formattedAdhanTime = timeFormatter.format(date),
                formattedIqamahTime = iqamahMillis?.let { timeFormatter.format(Date(it)) },
                isNext = isNext,
                isPassed = isPassed
            )
        }

        val nextPrayerItem: PrayerItem
        val remainingMillis: Long
        val foundTodayNext = items.firstOrNull { it.isNext }

        if (foundTodayNext != null) {
            nextPrayerItem = foundTodayNext
            remainingMillis = (foundTodayNext.adhanTimeMillis - nowMillis).coerceAtLeast(0L)
        } else {
            val tomorrowCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            val tomorrowPrayerTimes = PrayerTimes(
                coordinates,
                DateComponents.from(tomorrowCal.time),
                params
            )
            val tomorrowFajrMillis = tomorrowPrayerTimes.fajr.time
            val fajrOffset = settings.iqamahOffsets.offsetFor(PrayerName.FAJR)
            val tomorrowIqamah = if (fajrOffset > 0) tomorrowFajrMillis + fajrOffset * 60_000L else null

            nextPrayerItem = PrayerItem(
                prayer = PrayerName.FAJR,
                adhanTimeMillis = tomorrowFajrMillis,
                iqamahTimeMillis = tomorrowIqamah,
                iqamahOffsetMinutes = fajrOffset,
                formattedAdhanTime = timeFormatter.format(tomorrowPrayerTimes.fajr),
                formattedIqamahTime = tomorrowIqamah?.let { timeFormatter.format(Date(it)) },
                isNext = true,
                isPassed = false
            )
            remainingMillis = (tomorrowFajrMillis - nowMillis).coerceAtLeast(0L)
        }

        val qiblaDegrees = Qibla(coordinates).direction
        val midnightDate = sunnahTimes.middleOfTheNight
        val lastThirdDate = sunnahTimes.lastThirdOfTheNight

        return DailyPrayerSchedule(
            prayers = items,
            nextPrayer = nextPrayerItem,
            remainingMillis = remainingMillis,
            qiblaDegrees = qiblaDegrees,
            hijriDate = HijriCalendarHelper.todayHijri(),
            gregorianDateAr = HijriCalendarHelper.formatGregorianDateAr(todayDate),
            cityName = settings.cityName,
            countryName = settings.countryName,
            midnightTimeMillis = midnightDate.time,
            formattedMidnightTime = timeFormatter.format(midnightDate),
            lastThirdNightMillis = lastThirdDate.time,
            formattedLastThirdNight = timeFormatter.format(lastThirdDate)
        )
    }

    fun getUpcomingAlarmsForTodayAndTomorrow(
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Pair<PrayerName, Long>> {
        val coordinates = Coordinates(settings.latitude, settings.longitude)
        val params = settings.calculationMethod.adhanMethod.parameters.apply {
            madhab = settings.madhab.adhanMadhab
        }

        val results = mutableListOf<Pair<PrayerName, Long>>()
        for (dayOffset in 0..1) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val pt = PrayerTimes(coordinates, DateComponents.from(cal.time), params)
            val obligatory = listOf(
                PrayerName.FAJR to pt.fajr.time,
                PrayerName.DHUHR to pt.dhuhr.time,
                PrayerName.ASR to pt.asr.time,
                PrayerName.MAGHRIB to pt.maghrib.time,
                PrayerName.ISHA to pt.isha.time
            )
            obligatory.forEach { (name, timeMs) ->
                if (timeMs > nowMillis) {
                    results.add(name to timeMs)
                }
            }
        }
        return results
    }
}
