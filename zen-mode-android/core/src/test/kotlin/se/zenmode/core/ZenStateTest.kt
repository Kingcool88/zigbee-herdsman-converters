package se.zenmode.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZenStateTest {
    @Test
    fun graceUnlocksTemporarily() {
        val s = ZenState(active = true, endsAt = 10_000, graceUntil = 5_000)
        assertFalse(s.isLockedAt(4_000))
        assertTrue(s.isLockedAt(5_000))
    }

    @Test
    fun geofenceNeverExpires() {
        assertFalse(ZenState(active = true, endsAt = null).isExpiredAt(Long.MAX_VALUE))
    }

    @Test
    fun inactiveNeverLocked() {
        assertFalse(ZenState().isLockedAt(0))
    }
}
