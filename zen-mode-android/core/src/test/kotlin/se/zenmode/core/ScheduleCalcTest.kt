package se.zenmode.core

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScheduleCalcTest {
    private val zone = ZoneId.of("Europe/Stockholm")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    // 2026-11-23 är en måndag.
    private val workdays = Schedule("w", "Jobb", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), 9 * 60, 120)
    private val night = Schedule("n", "Natt", setOf(DayOfWeek.MONDAY), 22 * 60, 9 * 60)

    @Test
    fun activeInsideWindow() {
        val w = assertNotNull(ScheduleCalc.activeWindow(listOf(workdays), at(2026, 11, 23, 10, 0)))
        assertEquals(at(2026, 11, 23, 11, 0), w.end)
    }

    @Test
    fun notActiveAtEnd() {
        assertNull(ScheduleCalc.activeWindow(listOf(workdays), at(2026, 11, 23, 11, 0)))
    }

    @Test
    fun activeAcrossMidnightFromYesterday() {
        // Måndagskvällens pass pågår fortfarande tisdag 03:00.
        val w = assertNotNull(ScheduleCalc.activeWindow(listOf(night), at(2026, 11, 24, 3, 0)))
        assertEquals(at(2026, 11, 24, 7, 0), w.end)
    }

    @Test
    fun disabledIgnored() {
        assertNull(ScheduleCalc.activeWindow(listOf(workdays.copy(enabled = false)), at(2026, 11, 23, 10, 0)))
    }

    @Test
    fun nextStartSameDay() {
        val w = assertNotNull(ScheduleCalc.nextStart(listOf(workdays), at(2026, 11, 23, 8, 0)))
        assertEquals(at(2026, 11, 23, 9, 0), w.start)
    }

    @Test
    fun nextStartSkipsToNextMatchingDay() {
        // Tisdag efter start -> nästa måndag.
        val w = assertNotNull(ScheduleCalc.nextStart(listOf(workdays), at(2026, 11, 24, 9, 0)))
        assertEquals(at(2026, 11, 30, 9, 0), w.start)
    }

    @Test
    fun nextStartPicksEarliest() {
        val w = assertNotNull(ScheduleCalc.nextStart(listOf(night, workdays), at(2026, 11, 23, 12, 0)))
        assertEquals("n", w.schedule.id)
    }

    @Test
    fun noDaysNoStart() {
        assertNull(ScheduleCalc.nextStart(listOf(workdays.copy(days = emptySet())), at(2026, 11, 23, 8, 0)))
    }
}
