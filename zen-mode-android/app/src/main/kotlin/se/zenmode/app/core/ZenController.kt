package se.zenmode.app.core

import android.content.Context
import android.content.Intent
import android.util.Log
import se.zenmode.app.data.ZenStore
import se.zenmode.app.geo.GeofenceManager
import se.zenmode.app.service.ZenAccessibilityService
import se.zenmode.app.service.ZenForegroundService
import se.zenmode.app.ui.LockActivity
import se.zenmode.core.ScheduleCalc
import se.zenmode.core.ZenSource
import se.zenmode.core.ZenState
import java.time.Instant
import java.time.ZoneId

/** Central logik: starta, stoppa, pausa (partner-godkänt) och återställa zen-läget. */
object ZenController {
    private const val TAG = "ZenController"

    private fun now() = System.currentTimeMillis()

    fun startManual(ctx: Context, minutes: Int) {
        start(ctx, ZenSource.MANUAL, null, now() + minutes * 60_000L)
    }

    /**
     * Startar zen. Om zen redan är aktivt förlängs det bara om det nya passet slutar senare
     * (det går aldrig att förkorta ett pågående pass).
     */
    fun start(ctx: Context, source: ZenSource, sourceId: String?, endsAt: Long?) {
        val store = ZenStore.get(ctx)
        val current = store.state
        val newState = if (current.active && !current.isExpiredAt(now())) {
            val extendedEnd = when {
                current.endsAt == null -> null
                endsAt == null -> current.endsAt
                else -> maxOf(current.endsAt!!, endsAt)
            }
            current.copy(endsAt = extendedEnd)
        } else {
            ZenState(active = true, source = source, sourceId = sourceId, startedAt = now(), endsAt = endsAt)
        }
        store.state = newState
        Log.i(TAG, "Zen startat: $newState")
        activate(ctx, newState)
        showLock(ctx)
    }

    /** Sätter på allt som hör till ett aktivt pass. Idempotent – används även efter omstart. */
    private fun activate(ctx: Context, state: ZenState) {
        DndManager.enable(ctx)
        Kiosk.prepare(ctx)
        AlarmScheduler.setEnd(ctx, state.endsAt)
        state.graceUntil?.let { if (it > now()) AlarmScheduler.setRelock(ctx, it) }
        ZenForegroundService.refresh(ctx)
    }

    fun stop(ctx: Context) {
        val store = ZenStore.get(ctx)
        Log.i(TAG, "Zen avslutat")
        store.state = ZenState()
        store.pendingUnlock = null
        AlarmScheduler.cancelSessionAlarms(ctx)
        DndManager.restore(ctx)
        Kiosk.release(ctx)
        ZenForegroundService.stop(ctx)
        // LockActivity lyssnar på ZenStore och stänger sig själv.
    }

    /** Partnern har godkänt: lås upp i [ZenStore.GRACE_MINUTES] minuter. */
    fun grantGrace(ctx: Context) {
        val store = ZenStore.get(ctx)
        val state = store.state
        if (!state.active) return
        var until = now() + ZenStore.GRACE_MINUTES * 60_000L
        state.endsAt?.let { until = minOf(until, it) }
        store.pendingUnlock = null
        store.state = state.copy(graceUntil = until)
        AlarmScheduler.setRelock(ctx, until)
        ZenForegroundService.refresh(ctx)
    }

    fun relock(ctx: Context) {
        val store = ZenStore.get(ctx)
        val state = store.state
        if (!state.active) return
        if (state.isExpiredAt(now())) {
            stop(ctx)
            return
        }
        store.state = state.copy(graceUntil = null)
        ZenForegroundService.refresh(ctx)
        showLock(ctx)
    }

    fun onEndAlarm(ctx: Context) {
        val state = ZenStore.get(ctx).state
        // Liten marginal: alarm kan komma någon sekund för tidigt.
        if (state.active && state.endsAt != null && now() >= state.endsAt!! - 2_000) {
            stop(ctx)
        } else if (state.active) {
            AlarmScheduler.setEnd(ctx, state.endsAt)
        }
    }

    fun onScheduleAlarm(ctx: Context) {
        startScheduleIfInWindow(ctx)
        AlarmScheduler.scheduleNextScheduleStart(ctx)
    }

    private fun startScheduleIfInWindow(ctx: Context) {
        val zonedNow = Instant.ofEpochMilli(now()).atZone(ZoneId.systemDefault())
        val window = ScheduleCalc.activeWindow(ZenStore.get(ctx).schedules, zonedNow) ?: return
        start(ctx, ZenSource.SCHEDULE, window.schedule.id, window.end.toInstant().toEpochMilli())
    }

    /**
     * Efter omstart, uppdatering av appen eller ändrad tid. Säkerställer att ett pågående pass
     * fortsätter och att larm och geozoner är registrerade.
     */
    fun restore(ctx: Context, fullyBooted: Boolean) {
        val store = ZenStore.get(ctx)
        val state = store.state
        when {
            state.isExpiredAt(now()) -> stop(ctx)
            state.active -> {
                val fixed = if (state.graceUntil != null && state.graceUntil!! <= now()) state.copy(graceUntil = null) else state
                if (fixed != state) store.state = fixed
                activate(ctx, fixed)
            }
            else -> startScheduleIfInWindow(ctx)
        }
        AlarmScheduler.scheduleNextScheduleStart(ctx)
        if (fullyBooted) GeofenceManager.registerAll(ctx)
    }

    /** Visa låsskärmen. Går via tillgänglighetstjänsten om den körs (får starta aktiviteter från bakgrunden). */
    fun showLock(ctx: Context) {
        val launcher = ZenAccessibilityService.instance ?: ctx
        try {
            launcher.startActivity(
                Intent(launcher, LockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            )
        } catch (e: Exception) {
            // Om Android blockerar start från bakgrunden fångar tillgänglighetstjänsten nästa appbyte.
            Log.w(TAG, "Kunde inte visa låsskärmen just nu", e)
        }
    }
}
