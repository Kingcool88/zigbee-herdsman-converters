package se.zenmode.app.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import se.zenmode.app.data.ZenStore
import se.zenmode.app.receiver.AlarmReceiver
import se.zenmode.core.ScheduleCalc
import java.time.Instant
import java.time.ZoneId

object AlarmScheduler {
    const val ACTION_END = "se.zenmode.app.END"
    const val ACTION_RELOCK = "se.zenmode.app.RELOCK"
    const val ACTION_SCHEDULE_START = "se.zenmode.app.SCHEDULE_START"
    const val ACTION_DND_RESTORE = "se.zenmode.app.DND_RESTORE"

    private val requestCodes = mapOf(ACTION_END to 1, ACTION_RELOCK to 2, ACTION_SCHEDULE_START to 3, ACTION_DND_RESTORE to 4)

    private fun pendingIntent(ctx: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            requestCodes.getValue(action),
            Intent(ctx, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun set(ctx: Context, action: String, atMillis: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx, action)
        if (canScheduleExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            // Fallback: kan bli några minuter sen, men fungerar utan specialbehörighet.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun cancel(ctx: Context, action: String) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx, action))
    }

    fun setEnd(ctx: Context, endsAt: Long?) = if (endsAt == null) cancel(ctx, ACTION_END) else set(ctx, ACTION_END, endsAt)

    fun setRelock(ctx: Context, at: Long) = set(ctx, ACTION_RELOCK, at)

    fun setDndRestore(ctx: Context, at: Long) = set(ctx, ACTION_DND_RESTORE, at)

    fun cancelSessionAlarms(ctx: Context) {
        cancel(ctx, ACTION_END)
        cancel(ctx, ACTION_RELOCK)
        cancel(ctx, ACTION_DND_RESTORE)
    }

    /** Ställ larm för nästa schemalagda zen-pass (eller ta bort om inga finns). */
    fun scheduleNextScheduleStart(ctx: Context) {
        val now = Instant.now().atZone(ZoneId.systemDefault())
        val next = ScheduleCalc.nextStart(ZenStore.get(ctx).schedules, now)
        if (next == null) cancel(ctx, ACTION_SCHEDULE_START) else set(ctx, ACTION_SCHEDULE_START, next.start.toInstant().toEpochMilli())
    }
}
