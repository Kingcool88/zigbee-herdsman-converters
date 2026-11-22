package se.zenmode.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import se.zenmode.app.core.ZenController

/** Ser till att zen-läget fortsätter efter omstart – redan innan PIN är inmatad (LOCKED_BOOT_COMPLETED). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val fullyBooted = intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ZenController.restore(context, fullyBooted)
    }
}
