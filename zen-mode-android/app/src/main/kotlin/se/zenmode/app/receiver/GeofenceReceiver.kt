package se.zenmode.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import se.zenmode.app.core.ZenController
import se.zenmode.app.data.ZenStore
import se.zenmode.core.ZenSource

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w("ZenGeo", "Geofence-fel ${event.errorCode}")
            return
        }
        val ids = event.triggeringGeofences?.map { it.requestId } ?: return
        if (ids.isEmpty()) return
        val state = ZenStore.get(context).state
        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> ZenController.start(context, ZenSource.GEOFENCE, ids.first(), null)
            Geofence.GEOFENCE_TRANSITION_EXIT ->
                if (state.active && state.source == ZenSource.GEOFENCE && state.sourceId in ids) ZenController.stop(context)
        }
    }
}
