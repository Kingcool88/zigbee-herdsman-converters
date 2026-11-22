package se.zenmode.app.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DAY_LABELS = listOf(
    DayOfWeek.MONDAY to "Mån",
    DayOfWeek.TUESDAY to "Tis",
    DayOfWeek.WEDNESDAY to "Ons",
    DayOfWeek.THURSDAY to "Tor",
    DayOfWeek.FRIDAY to "Fre",
    DayOfWeek.SATURDAY to "Lör",
    DayOfWeek.SUNDAY to "Sön",
)

fun formatCountdown(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

fun formatMinuteOfDay(min: Int): String = "%02d:%02d".format((min / 60) % 24, min % 60)

fun formatClock(epochMs: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
