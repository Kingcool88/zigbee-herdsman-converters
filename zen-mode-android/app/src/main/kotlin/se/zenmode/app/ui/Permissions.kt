package se.zenmode.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import se.zenmode.app.core.AlarmScheduler
import se.zenmode.app.core.DndManager
import se.zenmode.app.core.Kiosk
import se.zenmode.app.service.ZenAccessibilityService

object Permissions {
    fun granted(ctx: Context, permission: String) = ctx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun notifications(ctx: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(ctx, Manifest.permission.POST_NOTIFICATIONS)

    fun sms(ctx: Context) = granted(ctx, Manifest.permission.SEND_SMS) && granted(ctx, Manifest.permission.RECEIVE_SMS)

    fun fineLocation(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)

    fun backgroundLocation(ctx: Context) = granted(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun callScreening(ctx: Context): Boolean = ctx.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    fun ignoringBattery(ctx: Context): Boolean = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    fun lockAvailable(ctx: Context) = ZenAccessibilityService.isEnabled(ctx) || Kiosk.isDeviceOwner(ctx)

    /** Det som måste vara på för att zen ska fungera alls. */
    fun missingCritical(ctx: Context): List<String> = listOfNotNull(
        "Stör ej-åtkomst".takeUnless { DndManager.hasAccess(ctx) },
        "Tillgänglighetstjänst (låsningen)".takeUnless { lockAvailable(ctx) },
        "Exakta alarm".takeUnless { AlarmScheduler.canScheduleExact(ctx) },
        "Notiser".takeUnless { notifications(ctx) },
    )
}

/** Öppnar kontaktväljaren och returnerar namn + nummer. Kräver ingen kontaktbehörighet. */
@Composable
fun rememberPhonePicker(onPicked: (name: String, number: String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) onPicked(c.getString(0) ?: "", c.getString(1) ?: "")
            }
        } catch (_: SecurityException) {
        }
    }
    return { launcher.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }
}
