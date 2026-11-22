package se.zenmode.app.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.telecom.TelecomManager
import android.util.Log
import se.zenmode.app.receiver.ZenDeviceAdminReceiver

/**
 * Valfritt "riktigt" lås via Lock Task Mode. Kräver att appen är device owner:
 *   adb shell dpm set-device-owner se.zenmode.app/.receiver.ZenDeviceAdminReceiver
 * Utan device owner används tillgänglighetstjänsten i stället.
 */
object Kiosk {
    private const val TAG = "ZenKiosk"

    private fun dpm(ctx: Context) = ctx.getSystemService(DevicePolicyManager::class.java)

    private fun admin(ctx: Context) = ComponentName(ctx, ZenDeviceAdminReceiver::class.java)

    fun isDeviceOwner(ctx: Context): Boolean = dpm(ctx).isDeviceOwnerApp(ctx.packageName)

    fun defaultDialer(ctx: Context): String? = ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    /** Anropas när zen startar. */
    fun prepare(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        try {
            val dpm = dpm(ctx)
            val packages = listOfNotNull(ctx.packageName, defaultDialer(ctx)).distinct().toTypedArray()
            dpm.setLockTaskPackages(admin(ctx), packages)
            dpm.setLockTaskFeatures(
                admin(ctx),
                DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS or DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO,
            )
            // Förhindra att man startar i felsäkert läge för att ta sig ur.
            dpm.addUserRestriction(admin(ctx), UserManager.DISALLOW_SAFE_BOOT)
        } catch (e: SecurityException) {
            Log.e(TAG, "Kunde inte förbereda Lock Task", e)
        }
    }

    /** Anropas när zen avslutas. */
    fun release(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        try {
            dpm(ctx).clearUserRestriction(admin(ctx), UserManager.DISALLOW_SAFE_BOOT)
        } catch (e: SecurityException) {
            Log.e(TAG, "Kunde inte släppa restriktioner", e)
        }
    }

    /** Släpp device owner-rollen (bara när inget pass är aktivt). */
    fun removeDeviceOwner(ctx: Context) {
        if (!isDeviceOwner(ctx)) return
        try {
            release(ctx)
            dpm(ctx).setLockTaskPackages(admin(ctx), emptyArray())
            @Suppress("DEPRECATION")
            dpm(ctx).clearDeviceOwnerApp(ctx.packageName)
        } catch (e: SecurityException) {
            Log.e(TAG, "Kunde inte ta bort device owner", e)
        }
    }
}
