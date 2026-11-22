package se.zenmode.core

import java.time.DayOfWeek

/** Vad som startade det aktuella zen-läget. */
enum class ZenSource { MANUAL, SCHEDULE, GEOFENCE }

/**
 * Hela zen-tillståndet. Sparas persistent så att det överlever omstart.
 *
 * @param endsAt när zen-läget avslutas (epoch ms). `null` = tills man lämnar geozonen.
 * @param graceUntil om satt: mobilen är tillfälligt upplåst (godkänt av partner) fram till denna tid.
 */
data class ZenState(
    val active: Boolean = false,
    val source: ZenSource? = null,
    val sourceId: String? = null,
    val startedAt: Long = 0L,
    val endsAt: Long? = null,
    val graceUntil: Long? = null,
) {
    fun isLockedAt(now: Long): Boolean = active && !inGraceAt(now)

    fun inGraceAt(now: Long): Boolean = active && graceUntil != null && now < graceUntil

    fun isExpiredAt(now: Long): Boolean = active && endsAt != null && now >= endsAt
}

data class Schedule(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    /** Minuter efter midnatt, 0..1439. */
    val startMinuteOfDay: Int,
    /** 1..1440 minuter. Får gå över midnatt. */
    val durationMinutes: Int,
    val enabled: Boolean = true,
)

data class GeoZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val enabled: Boolean = true,
)

data class AllowedContact(
    val name: String,
    val number: String,
)

/** En skickad begäran om tillfällig upplåsning som väntar på partnerns svar. */
data class PendingUnlock(
    val code: String,
    val requestedAt: Long,
    val expiresAt: Long,
    val denied: Boolean = false,
)
