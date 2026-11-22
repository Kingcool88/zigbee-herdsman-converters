package se.zenmode.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import se.zenmode.app.data.ZenStore

@Composable
fun rememberStore(): Pair<ZenStore, Long> {
    val store = ZenStore.get(LocalContext.current)
    val version by store.changes.collectAsState()
    return store to version
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

/** Visas när man försöker ändra något under ett pågående zen-pass. */
@Composable
fun LockedBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Zen är aktivt – inställningar kan inte ändras förrän passet är slut.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
