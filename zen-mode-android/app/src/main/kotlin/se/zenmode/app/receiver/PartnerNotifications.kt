package se.zenmode.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import se.zenmode.app.R
import se.zenmode.app.ZenApp
import se.zenmode.app.data.ZenStore
import se.zenmode.core.PhoneMatch

object PartnerNotifications {
    const val EXTRA_SENDER = "sender"
    const val EXTRA_CODE = "code"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    fun showRequest(ctx: Context, sender: String, code: String, body: String) {
        val store = ZenStore.get(ctx)
        val name = if (store.partnerNumber.isNotBlank() && PhoneMatch.same(sender, store.partnerNumber) && store.partnerName.isNotBlank()) {
            store.partnerName
        } else {
            sender
        }
        val notificationId = 1000 + code.toInt() % 100_000

        fun action(action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                notificationId * 10 + requestCode,
                Intent(ctx, PartnerActionReceiver::class.java)
                    .setAction(action)
                    .putExtra(EXTRA_SENDER, sender)
                    .putExtra(EXTRA_CODE, code)
                    .putExtra(EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val n = NotificationCompat.Builder(ctx, ZenApp.CHANNEL_PARTNER)
            .setSmallIcon(R.drawable.ic_zen)
            .setContentTitle("$name vill låsa upp zen-läget")
            .setContentText("Godkänn ${ZenStore.GRACE_MINUTES} minuters paus?")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Godkänn", action(PartnerActionReceiver.ACTION_APPROVE, 1))
            .addAction(0, "Neka", action(PartnerActionReceiver.ACTION_DENY, 2))
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java).notify(notificationId, n)
        } catch (_: SecurityException) {
            // Saknar notisbehörighet – partnern kan fortfarande svara med koden via SMS.
        }
    }
}
