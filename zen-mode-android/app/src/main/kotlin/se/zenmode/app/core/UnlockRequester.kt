package se.zenmode.app.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import se.zenmode.app.data.ZenStore
import se.zenmode.core.PendingUnlock
import se.zenmode.core.UnlockSms

object SmsSender {
    fun send(ctx: Context, to: String, text: String) {
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        // Svenska tecken gör att SMS:et kodas som UCS-2 (max 70 tecken/del) – dela upp vid behov.
        val parts = sms.divideMessage(text)
        sms.sendMultipartTextMessage(to, null, parts, null, null)
    }
}

object UnlockRequester {
    sealed interface Result {
        data object Sent : Result

        data object NoPartner : Result

        data class TooSoon(val secondsLeft: Long) : Result

        data class Failed(val message: String) : Result
    }

    fun request(ctx: Context): Result {
        val store = ZenStore.get(ctx)
        val partner = store.partnerNumber
        if (partner.isBlank()) return Result.NoPartner
        val now = System.currentTimeMillis()
        store.pendingUnlock?.let { p ->
            val wait = p.requestedAt + ZenStore.UNLOCK_REQUEST_COOLDOWN_MS - now
            if (wait > 0) return Result.TooSoon((wait + 999) / 1000)
        }
        val code = UnlockSms.generateCode()
        return try {
            SmsSender.send(ctx, partner, UnlockSms.request(store.ownerName, code, ZenStore.GRACE_MINUTES))
            if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                // Underlättar test i emulatorn. Loggas aldrig i release-bygget.
                Log.d("ZenUnlock", "Kod skickad till $partner: $code")
            }
            store.pendingUnlock = PendingUnlock(
                code = code,
                requestedAt = now,
                expiresAt = now + ZenStore.UNLOCK_REQUEST_VALID_MINUTES * 60_000L,
            )
            Result.Sent
        } catch (e: Exception) {
            Log.e("ZenUnlock", "SMS misslyckades", e)
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
