package se.zenmode.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import se.zenmode.app.core.ZenController
import se.zenmode.app.data.ZenStore
import se.zenmode.core.PhoneMatch
import se.zenmode.core.UnlockSms

/**
 * Tar emot SMS i båda rollerna:
 *  - Låst mobil: partnern svarar med koden -> lås upp i 5 min.
 *  - Partnerns mobil: en begäran kommer in -> visa notis med Godkänn/Neka.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val store = ZenStore.get(context)
        val now = System.currentTimeMillis()

        val pending = store.pendingUnlock
        val fromPartner = store.partnerNumber.isNotBlank() && PhoneMatch.same(sender, store.partnerNumber)
        if (pending != null && fromPartner && store.state.active) {
            when {
                now > pending.expiresAt -> Log.i(TAG, "Svar på utgången begäran ignoreras")
                UnlockSms.isDenialOf(body, pending.code) -> store.pendingUnlock = pending.copy(denied = true)
                UnlockSms.isApprovalOf(body, pending.code) -> {
                    Log.i(TAG, "Partnern godkände upplåsning")
                    ZenController.grantGrace(context)
                }
            }
            if (!UnlockSms.isRequest(body)) return
        }

        val code = UnlockSms.parseRequestCode(body) ?: return
        PartnerNotifications.showRequest(context, sender, code, body)
    }

    private companion object {
        const val TAG = "ZenSms"
    }
}
