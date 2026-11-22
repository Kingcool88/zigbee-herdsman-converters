package se.zenmode.app.geo

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import se.zenmode.app.data.ZenStore
import se.zenmode.app.receiver.GeofenceReceiver

object GeofenceManager {
    private const val TAG = "ZenGeo"

    fun hasPermissions(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ctx.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            0,
            Intent(ctx, GeofenceReceiver::class.java),
            // Måste vara MUTABLE – Play Services fyller i händelsedata.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

    /** Registrera om alla aktiva zoner. Geozoner rensas av systemet vid omstart, så detta körs vid boot. */
    @SuppressLint("MissingPermission")
    fun registerAll(ctx: Context) {
        try {
            val client = LocationServices.getGeofencingClient(ctx)
            val pi = pendingIntent(ctx)
            client.removeGeofences(pi)
            if (!hasPermissions(ctx)) return
            val zones = ZenStore.get(ctx).zones.filter { it.enabled }
            if (zones.isEmpty()) return
            val fences = zones.map {
                Geofence.Builder()
                    .setRequestId(it.id)
                    .setCircularRegion(it.latitude, it.longitude, it.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(fences)
                .build()
            client.addGeofences(request, pi)
                .addOnSuccessListener { Log.i(TAG, "${fences.size} geozoner registrerade") }
                .addOnFailureListener { Log.e(TAG, "Kunde inte registrera geozoner", it) }
        } catch (e: Exception) {
            Log.e(TAG, "Geozoner otillgängliga", e)
        }
    }
}
