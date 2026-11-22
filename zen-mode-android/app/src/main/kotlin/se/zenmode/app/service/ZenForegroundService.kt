package se.zenmode.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import se.zenmode.app.R
import se.zenmode.app.ZenApp
import se.zenmode.app.data.ZenStore
import se.zenmode.app.ui.LockActivity
import se.zenmode.core.ZenState

/** Håller appen vid liv under zen och visar en nedräkning i notisfältet. */
class ZenForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = ZenStore.get(this).state
        if (!state.active) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(this, state), type)
        return START_STICKY
    }

    companion object {
        private const val TAG = "ZenFgService"
        const val NOTIFICATION_ID = 1

        fun buildNotification(ctx: Context, state: ZenState): Notification {
            val now = System.currentTimeMillis()
            val open = PendingIntent.getActivity(
                ctx,
                0,
                Intent(ctx, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val b = NotificationCompat.Builder(ctx, ZenApp.CHANNEL_STATUS)
                .setSmallIcon(R.drawable.ic_zen)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(open)
            val grace = state.graceUntil
            when {
                grace != null && grace > now -> b.setContentTitle("Paus – låses igen om").setWhen(grace)
                state.endsAt != null -> b.setContentTitle("Zen-läge aktivt").setContentText("Tid kvar").setWhen(state.endsAt!!)
                else -> b.setContentTitle("Zen-läge aktivt").setContentText("Tills du lämnar zonen")
            }
            if (grace != null && grace > now || state.endsAt != null) {
                b.setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
            }
            return b.build()
        }

        fun refresh(ctx: Context) {
            val state = ZenStore.get(ctx).state
            if (!state.active) return
            try {
                ctx.startForegroundService(Intent(ctx, ZenForegroundService::class.java))
            } catch (e: Exception) {
                // Android 12+ kan neka start från bakgrunden. Uppdatera då bara notisen.
                Log.w(TAG, "Kunde inte starta förgrundstjänst, uppdaterar notis", e)
                try {
                    ctx.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(ctx, state))
                } catch (_: SecurityException) {
                }
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ZenForegroundService::class.java))
            ctx.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }
    }
}
