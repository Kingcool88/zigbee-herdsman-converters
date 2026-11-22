package se.zenmode.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import se.zenmode.app.core.SmsSender
import se.zenmode.core.UnlockSms

/** Partnerns knappar Godkänn / Neka i notisen. */
class PartnerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sender = intent.getStringExtra(PartnerNotifications.EXTRA_SENDER) ?: return
        val code = intent.getStringExtra(PartnerNotifications.EXTRA_CODE) ?: return
        val id = intent.getIntExtra(PartnerNotifications.EXTRA_NOTIFICATION_ID, 0)
        val text = when (intent.action) {
            ACTION_APPROVE -> UnlockSms.approval(code)
            ACTION_DENY -> UnlockSms.denial(code)
            else -> return
        }
        try {
            SmsSender.send(context, sender, text)
        } catch (e: Exception) {
            Log.e("ZenPartner", "Kunde inte skicka svar", e)
        }
        context.getSystemService(NotificationManager::class.java).cancel(id)
    }

    companion object {
        const val ACTION_APPROVE = "se.zenmode.app.PARTNER_APPROVE"
        const val ACTION_DENY = "se.zenmode.app.PARTNER_DENY"
    }
}
