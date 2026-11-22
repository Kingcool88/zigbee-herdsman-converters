package se.zenmode.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Ren logik för schemalagda zen-pass. Ingen Android-beroende kod, så den kan enhetstestas. */
object ScheduleCalc {
    data class Window(val schedule: Schedule, val start: ZonedDateTime, val end: ZonedDateTime)

    private fun startOn(date: LocalDate, s: Schedule, now: ZonedDateTime): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.of(s.startMinuteOfDay / 60, s.startMinuteOfDay % 60), now.zone)

    /** Det schemafönster som pågår just nu (det som slutar senast om flera överlappar). */
    fun activeWindow(schedules: List<Schedule>, now: ZonedDateTime): Window? {
        var best: Window? = null
        for (s in schedules) {
            if (!s.enabled || s.days.isEmpty()) continue
            // Ett pass kan ha startat igår och gå över midnatt.
            for (offset in -1L..0L) {
                val date = now.toLocalDate().plusDays(offset)
                if (date.dayOfWeek !in s.days) continue
                val start = startOn(date, s, now)
                val end = start.plusMinutes(s.durationMinutes.toLong())
                if (!now.isBefore(start) && now.isBefore(end)) {
                    if (best == null || end.isAfter(best.end)) best = Window(s, start, end)
                }
            }
        }
        return best
    }

    /** Nästa schemastart som ligger strikt efter [now]. */
    fun nextStart(schedules: List<Schedule>, now: ZonedDateTime): Window? {
        var best: Window? = null
        for (s in schedules) {
            if (!s.enabled || s.days.isEmpty()) continue
            for (offset in 0L..7L) {
                val date = now.toLocalDate().plusDays(offset)
                if (date.dayOfWeek !in s.days) continue
                val start = startOn(date, s, now)
                if (!start.isAfter(now)) continue
                if (best == null || start.isBefore(best.start)) {
                    best = Window(s, start, start.plusMinutes(s.durationMinutes.toLong()))
                }
                break
            }
        }
        return best
    }
}
