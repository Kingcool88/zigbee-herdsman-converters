package se.zenmode.app.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import se.zenmode.app.core.Kiosk
import se.zenmode.app.core.ZenController
import se.zenmode.app.data.ZenStore

/**
 * Låset för mobiler där appen inte är device owner: så fort en annan app hamnar i förgrunden
 * medan zen är aktivt skickas användaren tillbaka till zen-skärmen.
 *
 * Tjänsten läser inte innehållet i andra appar, bara vilket paket som visas.
 */
class ZenAccessibilityService : AccessibilityService() {
    private var lastBringBack = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (ZenStore.get(this).state.isLockedAt(System.currentTimeMillis())) ZenController.showLock(this)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val state = ZenStore.get(this).state
        val now = System.currentTimeMillis()
        if (!state.active || state.isExpiredAt(now)) return

        if (!state.isLockedAt(now)) {
            // Under en godkänd paus får man använda mobilen – men inte Inställningar,
            // annars kan man stänga av tillgänglighetstjänsten och fuska sig ur.
            if (pkg in SETTINGS_PACKAGES) performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        if (pkg in allowedPackages()) return

        if (pkg == SYSTEM_UI) {
            // Notisfältet / snabbinställningar: stäng dem. På själva låsskärmen (PIN) gör vi inget.
            val keyguard = getSystemService(KeyguardManager::class.java)
            if (!keyguard.isKeyguardLocked) performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        if (now - lastBringBack < 300) return
        lastBringBack = now
        ZenController.showLock(this)
    }

    private fun allowedPackages(): Set<String> {
        val set = mutableSetOf(packageName, "android", "com.android.server.telecom", "com.android.phone", "com.android.emergency")
        Kiosk.defaultDialer(this)?.let { set += it }
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
            ?.let { set += it }
        return set
    }

    companion object {
        private const val SYSTEM_UI = "com.android.systemui"
        private val SETTINGS_PACKAGES = setOf("com.android.settings")

        @Volatile
        var instance: ZenAccessibilityService? = null
            private set

        fun isEnabled(ctx: Context): Boolean {
            val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(ctx, ZenAccessibilityService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
