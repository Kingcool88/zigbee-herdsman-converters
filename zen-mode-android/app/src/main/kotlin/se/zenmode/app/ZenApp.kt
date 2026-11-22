package se.zenmode.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class ZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Zen-status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Visar att zen-läget är aktivt och hur länge det är kvar."
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PARTNER, "Upplåsningsbegäran", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "När en vän ber dig godkänna att deras mobil låses upp i några minuter."
            },
        )
    }

    companion object {
        const val CHANNEL_STATUS = "zen_status"
        const val CHANNEL_PARTNER = "partner_requests"
    }
}
