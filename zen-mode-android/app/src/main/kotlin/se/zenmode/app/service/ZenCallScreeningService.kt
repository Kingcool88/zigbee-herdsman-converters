package se.zenmode.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import se.zenmode.app.core.DndManager
import se.zenmode.app.data.ZenStore
import se.zenmode.core.PhoneMatch

/**
 * Släpper igenom samtal från kontakter i appens lista trots Stör ej.
 * Kräver att appen har rollen "Samtalsfiltrering / nummerpresentation och spam".
 * Vi blockerar aldrig samtal här – Stör ej sköter tystnaden.
 */
class ZenCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        try {
            if (details.callDirection == Call.Details.DIRECTION_INCOMING) {
                val number = details.handle?.schemeSpecificPart
                val store = ZenStore.get(this)
                if (number != null && store.state.active && store.allowedContacts.any { PhoneMatch.same(it.number, number) }) {
                    Log.i("ZenCalls", "Tillåten kontakt ringer – släpper igenom")
                    DndManager.allowRingingTemporarily(this)
                }
            }
        } finally {
            respondToCall(details, CallResponse.Builder().build())
        }
    }
}
