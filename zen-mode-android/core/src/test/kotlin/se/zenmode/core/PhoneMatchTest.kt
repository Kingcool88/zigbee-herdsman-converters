package se.zenmode.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneMatchTest {
    @Test
    fun internationalVsLocal() {
        assertTrue(PhoneMatch.same("+46 70-123 45 67", "070 123 45 67"))
        assertTrue(PhoneMatch.same("0046701234567", "0701234567"))
    }

    @Test
    fun differentNumbers() {
        assertFalse(PhoneMatch.same("+46701234567", "+46701234568"))
    }

    @Test
    fun shortNumbersExact() {
        assertTrue(PhoneMatch.same("112", "112"))
        assertFalse(PhoneMatch.same("112", "+46701234112"))
    }

    @Test
    fun emptyNeverMatches() {
        assertFalse(PhoneMatch.same("", ""))
        assertFalse(PhoneMatch.same("abc", "abc"))
    }

    @Test
    fun emulatorNumbers() {
        // Emulatorer har nummer som 15555215554 / 5554.
        assertTrue(PhoneMatch.same("+15555215554", "15555215554"))
    }
}
