package se.zenmode.app.ui

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
import se.zenmode.core.AllowedContact
import se.zenmode.core.PhoneMatch

@Composable
fun ContactsTab(modifier: Modifier) {
    val ctx = LocalContext.current
    val (store, version) = rememberStore()
    val contacts = remember(version) { store.allowedContacts }
    val allowStarred = remember(version) { store.allowStarred }
    val locked = remember(version) { store.state.active }
    val screeningOk = remember(version) { Permissions.callScreening(ctx) }
    var showManual by remember { mutableStateOf(false) }

    fun add(name: String, number: String) {
        if (number.isBlank() || contacts.any { PhoneMatch.same(it.number, number) }) return
        store.allowedContacts = contacts + AllowedContact(name.trim(), number.trim())
    }
    val pick = rememberPhonePicker { name, number -> add(name, number) }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Samtal som går igenom", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Allt annat är tyst under zen. Larm (väckarklocka) fungerar alltid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (locked) LockedBanner()
        if (!screeningOk && contacts.isNotEmpty()) {
            Text(
                "OBS: Ge appen rollen Samtalsfiltrering under Inställn., annars ringer inte dessa kontakter igenom.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Släpp även igenom stjärnmärkta kontakter", Modifier.weight(1f))
            Switch(checked = allowStarred, enabled = !locked, onCheckedChange = { store.allowStarred = it })
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts, key = { it.number }) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name.ifBlank { c.number }, style = MaterialTheme.typography.titleMedium)
                            if (c.name.isNotBlank()) Text(c.number, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(enabled = !locked, onClick = { store.allowedContacts = contacts - c }) {
                            Icon(Icons.Default.Delete, contentDescription = "Ta bort")
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !locked, onClick = pick, modifier = Modifier.weight(1f)) { Text("Välj kontakt") }
            OutlinedButton(enabled = !locked, onClick = { showManual = true }, modifier = Modifier.weight(1f)) { Text("Ange nummer") }
        }
    }

    if (showManual) {
        var name by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("Lägg till nummer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Namn") }, singleLine = true)
                    OutlinedTextField(number, { number = it }, label = { Text("Telefonnummer") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = {
                TextButton(enabled = PhoneMatch.digits(number).length >= 3, onClick = {
                    add(name, number)
                    showManual = false
                }) { Text("Lägg till") }
            },
            dismissButton = { TextButton(onClick = { showManual = false }) { Text("Avbryt") } },
        )
    }
}
