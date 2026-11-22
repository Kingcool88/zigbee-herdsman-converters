package se.zenmode.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.zenmode.app.core.AlarmScheduler
import se.zenmode.app.core.DndManager
import se.zenmode.app.core.Kiosk
import se.zenmode.app.geo.GeofenceManager
import se.zenmode.app.service.ZenAccessibilityService

@Composable
fun SettingsTab(modifier: Modifier, resumeTick: Int) {
    val ctx = LocalContext.current
    val (store, version) = rememberStore()
    val locked = remember(version) { store.state.active }
    var ownerName by remember { mutableStateOf(store.ownerName) }
    var partnerName by remember { mutableStateOf(store.partnerName) }
    var partnerNumber by remember { mutableStateOf(store.partnerNumber) }
    var permTick by remember { mutableIntStateOf(0) }
    val refresh = { permTick++ }

    val pickPartner = rememberPhonePicker { name, number ->
        partnerName = name
        partnerNumber = number
    }
    val requestPerms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    val requestOne = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh()
        GeofenceManager.registerAll(ctx)
    }
    val forResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    val pkgUri = Uri.parse("package:${ctx.packageName}")

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Inställningar", style = MaterialTheme.typography.headlineSmall)
        if (locked) LockedBanner()

        SectionTitle("Partner för upplåsning")
        Text(
            "Personen som får godkänna en paus på 5 min. Hen får ett SMS och godkänner antingen i Zen Mode-appen " +
                "eller genom att svara med koden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        OutlinedTextField(ownerName, { ownerName = it }, label = { Text("Ditt namn (visas i SMS:et)") }, singleLine = true,
            enabled = !locked, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(partnerName, { partnerName = it }, label = { Text("Partnerns namn") }, singleLine = true,
            enabled = !locked, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(partnerNumber, { partnerNumber = it }, label = { Text("Partnerns telefonnummer") }, singleLine = true,
            enabled = !locked, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(enabled = !locked, onClick = pickPartner) { Text("Välj kontakt") }
            Button(enabled = !locked, onClick = {
                store.ownerName = ownerName
                store.partnerName = partnerName
                store.partnerNumber = partnerNumber
            }) { Text("Spara") }
        }

        SectionTitle("Behörigheter")
        // permTick/resumeTick gör att raderna läses om efter att användaren varit i systeminställningarna.
        val tick = permTick + resumeTick
        PermissionRow(tick, "Stör ej-åtkomst", "Tystar alla notiser och samtal under zen.", { DndManager.hasAccess(ctx) }) {
            forResult.launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        PermissionRow(
            tick,
            "Tillgänglighetstjänst",
            "Själva låset: håller dig kvar på zen-skärmen. (Android 13+: om knappen är grå, gå till Appinfo → ⋮ → Tillåt begränsade inställningar.)",
            { ZenAccessibilityService.isEnabled(ctx) },
        ) {
            forResult.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        PermissionRow(tick, "Exakta alarm", "Så att zen startar och slutar på sekunden.", { AlarmScheduler.canScheduleExact(ctx) }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                forResult.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri))
            }
        }
        PermissionRow(tick, "Notiser", "Nedräkning i notisfältet och upplåsningsförfrågningar.", { Permissions.notifications(ctx) }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPerms.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }
        PermissionRow(tick, "SMS", "Skicka och ta emot upplåsningsförfrågningar.", { Permissions.sms(ctx) }) {
            requestPerms.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS))
        }
        PermissionRow(tick, "Samtalsfiltrering", "Släpper igenom samtal från dina valda kontakter.", { Permissions.callScreening(ctx) }) {
            val rm = ctx.getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) forResult.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
        PermissionRow(tick, "Plats", "Behövs för geozoner.", { Permissions.fineLocation(ctx) }) {
            requestPerms.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        PermissionRow(tick, "Plats i bakgrunden", "Välj \"Tillåt alltid\" så att geozoner fungerar när appen är stängd.", {
            Permissions.backgroundLocation(ctx)
        }) {
            if (Permissions.fineLocation(ctx)) requestOne.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        PermissionRow(tick, "Ingen batterioptimering", "Hindrar Android från att stänga av appen.", { Permissions.ignoringBattery(ctx) }) {
            forResult.launch(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
        }

        SectionTitle("Kiosklås (valfritt, starkast)")
        val owner = remember(tick) { Kiosk.isDeviceOwner(ctx) }
        Text(
            if (owner) {
                "Aktivt: appen är device owner och låser mobilen med Lock Task Mode."
            } else {
                "Inte aktivt. Kräver en mobil/emulator utan konton och följande kommando:"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        if (owner && !locked) {
            OutlinedButton(onClick = {
                Kiosk.removeDeviceOwner(ctx)
                refresh()
            }) { Text("Ta bort kiosklås (device owner)") }
        }
        if (!owner) {
            Text(
                "adb shell dpm set-device-owner se.zenmode.app/.receiver.ZenDeviceAdminReceiver",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PermissionRow(tick: Int, title: String, description: String, check: () -> Boolean, onFix: () -> Unit) {
    val ok = remember(tick) { check() }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text((if (ok) "✓ " else "✗ ") + title, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        if (!ok) TextButton(onClick = onFix) { Text("Fixa") }
    }
}
