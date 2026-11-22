package se.zenmode.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import se.zenmode.app.core.ZenController
import se.zenmode.app.data.ZenStore

class MainActivity : ComponentActivity() {
    /** Ökas i onResume så att behörighetslistan läses om när man kommer tillbaka från Inställningar. */
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZenTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            TABS.forEachIndexed { i, (label, icon) ->
                                NavigationBarItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    val m = Modifier.padding(padding)
                    when (tab) {
                        0 -> ZenTab(m, resumeTick)
                        1 -> ScheduleTab(m)
                        2 -> ZonesTab(m)
                        3 -> ContactsTab(m)
                        else -> SettingsTab(m, resumeTick)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
        // Appen själv får inte bli en väg runt låset.
        if (ZenStore.get(this).state.isLockedAt(System.currentTimeMillis())) {
            ZenController.showLock(this)
            finish()
        }
    }

    private companion object {
        val TABS: List<Pair<String, ImageVector>> = listOf(
            "Zen" to Icons.Default.Home,
            "Schema" to Icons.Default.DateRange,
            "Zoner" to Icons.Default.Place,
            "Kontakter" to Icons.Default.Person,
            "Inställn." to Icons.Default.Settings,
        )
    }
}
