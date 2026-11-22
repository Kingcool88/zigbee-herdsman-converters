package se.zenmode.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import se.zenmode.app.core.ZenController
import se.zenmode.core.ZenSource
import kotlin.math.roundToInt

private val PRESETS = listOf(15, 30, 60, 120, 240)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenTab(modifier: Modifier, resumeTick: Int) {
    val ctx = LocalContext.current
    val (store, version) = rememberStore()
    val state = remember(version) { store.state }
    val missing = remember(resumeTick, version) { Permissions.missingCritical(ctx) }
    var minutes by remember { mutableIntStateOf(store.lastQuickMinutes) }
    var confirm by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Zen Mode", style = MaterialTheme.typography.headlineMedium)

        if (missing.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Saknade behörigheter", style = MaterialTheme.typography.titleSmall)
                    missing.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    Text("Fixa under fliken Inställn.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.active) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val sourceText = when (state.source) {
                        ZenSource.SCHEDULE -> "Schemalagt pass"
                        ZenSource.GEOFENCE -> "Geozon"
                        else -> "Manuellt pass"
                    }
                    Text(sourceText, style = MaterialTheme.typography.titleMedium)
                    state.endsAt?.let { Text(formatCountdown(it - now), fontSize = 40.sp) }
                    state.graceUntil?.let { if (it > now) Text("Paus i ${formatCountdown(it - now)} till") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { ZenController.showLock(ctx) }) { Text("Öppna zen-skärmen") }
                }
            }
            return@Column
        }

        Text("Starta nu", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { p ->
                FilterChip(selected = minutes == p, onClick = { minutes = p }, label = { Text(formatMinutes(p)) })
            }
        }
        Text(formatMinutes(minutes), style = MaterialTheme.typography.displaySmall)
        Slider(
            value = minutes.toFloat(),
            onValueChange = { minutes = ((it / 5f).roundToInt() * 5).coerceIn(5, 720) },
            valueRange = 5f..720f,
        )
        Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Starta zen i ${formatMinutes(minutes)}") }
        Text(
            "När zen har startat kan det inte avbrytas. Bara din partner kan ge dig en paus på 5 minuter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Starta zen-läge?") },
            text = { Text("Mobilen låses i ${formatMinutes(minutes)}. Du kan inte avbryta själv.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    store.lastQuickMinutes = minutes
                    ZenController.startManual(ctx, minutes)
                }) { Text("Starta") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Avbryt") } },
        )
    }
}
