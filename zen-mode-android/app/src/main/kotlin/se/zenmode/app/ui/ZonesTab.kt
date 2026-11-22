package se.zenmode.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import se.zenmode.app.geo.GeofenceManager
import se.zenmode.core.GeoZone
import java.util.Locale
import java.util.UUID

@Composable
fun ZonesTab(modifier: Modifier) {
    val ctx = LocalContext.current
    val (store, version) = rememberStore()
    val zones = remember(version) { store.zones }
    val locked = remember(version) { store.state.active }
    var showAdd by remember { mutableStateOf(false) }

    fun save(list: List<GeoZone>) {
        store.zones = list
        GeofenceManager.registerAll(ctx)
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Geozoner", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Zen startar när du kommer in i en zon och slutar när du lämnar den. Kräver platsåtkomst \"Tillåt alltid\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (locked) LockedBanner()
        LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (zones.isEmpty()) item { Text("Inga zoner ännu.", color = MaterialTheme.colorScheme.secondary) }
            items(zones, key = { it.id }) { z ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(z.name.ifBlank { "Zon" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                String.format(Locale.ROOT, "%.5f, %.5f · %d m", z.latitude, z.longitude, z.radiusMeters.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = z.enabled,
                            enabled = !locked,
                            onCheckedChange = { on -> save(zones.map { if (it.id == z.id) it.copy(enabled = on) else it }) },
                        )
                        IconButton(enabled = !locked, onClick = { save(zones.filterNot { it.id == z.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ta bort")
                        }
                    }
                }
            }
        }
        Button(onClick = { showAdd = true }, enabled = !locked, modifier = Modifier.fillMaxWidth()) { Text("Lägg till zon") }
    }

    if (showAdd) {
        ZoneDialog(
            onDismiss = { showAdd = false },
            onSave = {
                showAdd = false
                save(zones + it)
            },
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ZoneDialog(onDismiss: () -> Unit, onSave: (GeoZone) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("150") }
    val latV = lat.replace(',', '.').toDoubleOrNull()
    val lngV = lng.replace(',', '.').toDoubleOrNull()
    val radiusV = radius.toFloatOrNull()
    val valid = latV != null && latV in -90.0..90.0 && lngV != null && lngV in -180.0..180.0 && radiusV != null && radiusV >= 50f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny geozon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Namn (t.ex. Hemma)") }, singleLine = true)
                OutlinedButton(onClick = {
                    if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(ctx, "Ge platsbehörighet under Inställn. först", Toast.LENGTH_SHORT).show()
                        return@OutlinedButton
                    }
                    LocationServices.getFusedLocationProviderClient(ctx)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { loc ->
                            if (loc == null) {
                                Toast.makeText(ctx, "Ingen position just nu", Toast.LENGTH_SHORT).show()
                            } else {
                                lat = String.format(Locale.ROOT, "%.6f", loc.latitude)
                                lng = String.format(Locale.ROOT, "%.6f", loc.longitude)
                            }
                        }
                }) { Text("Använd nuvarande plats") }
                OutlinedTextField(lat, { lat = it }, label = { Text("Latitud") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(lng, { lng = it }, label = { Text("Longitud") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(radius, { radius = it.filter(Char::isDigit) }, label = { Text("Radie i meter (minst 50)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(GeoZone(UUID.randomUUID().toString(), name.trim(), latV!!, lngV!!, radiusV!!))
            }) { Text("Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}
