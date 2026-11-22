package se.zenmode.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.zenmode.app.core.AlarmScheduler
import se.zenmode.core.Schedule
import java.time.DayOfWeek
import java.util.UUID

@Composable
fun ScheduleTab(modifier: Modifier) {
    val ctx = LocalContext.current
    val (store, version) = rememberStore()
    val schedules = remember(version) { store.schedules }
    val locked = remember(version) { store.state.active }
    var showAdd by remember { mutableStateOf(false) }

    fun save(list: List<Schedule>) {
        store.schedules = list
        AlarmScheduler.scheduleNextScheduleStart(ctx)
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Schemalagd zen", style = MaterialTheme.typography.headlineSmall)
        if (locked) LockedBanner()
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (schedules.isEmpty()) item { Text("Inga scheman ännu.", color = MaterialTheme.colorScheme.secondary) }
            items(schedules, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name.ifBlank { "Schema" }, style = MaterialTheme.typography.titleMedium)
                            val end = s.startMinuteOfDay + s.durationMinutes
                            Text("${formatMinuteOfDay(s.startMinuteOfDay)}–${formatMinuteOfDay(end)} (${formatMinutes(s.durationMinutes)})")
                            Text(DAY_LABELS.filter { it.first in s.days }.joinToString(" ") { it.second }, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = s.enabled,
                            enabled = !locked,
                            onCheckedChange = { on -> save(schedules.map { if (it.id == s.id) it.copy(enabled = on) else it }) },
                        )
                        IconButton(enabled = !locked, onClick = { save(schedules.filterNot { it.id == s.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ta bort")
                        }
                    }
                }
            }
        }
        Button(onClick = { showAdd = true }, enabled = !locked, modifier = Modifier.fillMaxWidth()) { Text("Lägg till schema") }
    }

    if (showAdd) {
        ScheduleDialog(
            onDismiss = { showAdd = false },
            onSave = {
                showAdd = false
                save(schedules + it)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDialog(onDismiss: () -> Unit, onSave: (Schedule) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var start by remember { mutableIntStateOf(9 * 60) }
    var duration by remember { mutableStateOf("60") }
    var days by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) }
    val durationMin = duration.toIntOrNull()
    val valid = durationMin != null && durationMin in 1..1440 && days.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nytt schema") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Namn (t.ex. Fokus jobb)") }, singleLine = true)
                OutlinedButton(onClick = {
                    TimePickerDialog(ctx, { _, h, m -> start = h * 60 + m }, start / 60, start % 60, true).show()
                }) { Text("Start: ${formatMinuteOfDay(start)}") }
                OutlinedTextField(
                    duration,
                    { duration = it.filter(Char::isDigit).take(4) },
                    label = { Text("Längd i minuter") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DAY_LABELS.forEach { (day, label) ->
                        FilterChip(
                            selected = day in days,
                            onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(Schedule(UUID.randomUUID().toString(), name.trim(), days, start, durationMin!!))
            }) { Text("Spara") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Avbryt") } },
    )
}

