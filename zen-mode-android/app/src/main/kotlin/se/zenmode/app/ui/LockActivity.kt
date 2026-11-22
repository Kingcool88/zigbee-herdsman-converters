package se.zenmode.app.ui

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import se.zenmode.app.core.UnlockRequester
import se.zenmode.app.data.ZenStore
import se.zenmode.core.ZenSource

/** Zen-skärmen som visas hela tiden medan zen är aktivt. */
class LockActivity : ComponentActivity() {
    private val store by lazy { ZenStore.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Tillbaka-knappen gör ingenting under zen.
                }
            },
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            ZenTheme {
                LockScreen(
                    store = store,
                    onLeave = ::leave,
                    onRequestUnlock = ::requestUnlock,
                    onEmergency = { startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!store.state.isLockedAt(System.currentTimeMillis())) {
            leave()
            return
        }
        // Riktigt kiosklås om appen är device owner.
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val am = getSystemService(ActivityManager::class.java)
        if (dpm.isLockTaskPermitted(packageName) && am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            try {
                startLockTask()
            } catch (_: Exception) {
            }
        }
    }

    private fun leave() {
        val am = getSystemService(ActivityManager::class.java)
        if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            try {
                stopLockTask()
            } catch (_: Exception) {
            }
        }
        if (!isFinishing) finish()
    }

    private fun requestUnlock() {
        val msg = when (val r = UnlockRequester.request(this)) {
            UnlockRequester.Result.Sent -> "Förfrågan skickad"
            UnlockRequester.Result.NoPartner -> "Ingen partner inlagd"
            is UnlockRequester.Result.TooSoon -> "Vänta ${r.secondsLeft} s innan du frågar igen"
            is UnlockRequester.Result.Failed -> "Kunde inte skicka SMS: ${r.message}"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun LockScreen(store: ZenStore, onLeave: () -> Unit, onRequestUnlock: () -> Unit, onEmergency: () -> Unit) {
    val version by store.changes.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val state = remember(version) { store.state }
    val pending = remember(version) { store.pendingUnlock }
    val partner = remember(version) { store.partnerName.ifBlank { store.partnerNumber } }

    LaunchedEffect(state, now) {
        if (!state.isLockedAt(now)) onLeave()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Zen", color = ZenAccent, fontSize = 22.sp, fontWeight = FontWeight.Light, letterSpacing = 6.sp)
        Spacer(Modifier.height(24.dp))
        val endsAt = state.endsAt
        if (endsAt != null) {
            Text(formatCountdown(endsAt - now), fontSize = 64.sp, fontWeight = FontWeight.Thin, color = MaterialTheme.colorScheme.onBackground)
            Text("Slutar ${formatClock(endsAt)}", color = MaterialTheme.colorScheme.secondary)
        } else {
            Text("Aktivt", fontSize = 48.sp, fontWeight = FontWeight.Thin, color = MaterialTheme.colorScheme.onBackground)
            if (state.source == ZenSource.GEOFENCE) Text("Tills du lämnar zonen", color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(48.dp))
        Text(
            "Andas. Telefonen väntar.",
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(64.dp))

        if (partner.isNotBlank()) {
            TextButton(onClick = onRequestUnlock) {
                Text("Be $partner om ${ZenStore.GRACE_MINUTES} min paus", fontSize = 13.sp)
            }
            if (pending != null) {
                val text = when {
                    pending.denied -> "$partner sa nej."
                    now > pending.expiresAt -> "Förfrågan har gått ut."
                    else -> "Väntar på svar (skickad ${formatClock(pending.requestedAt)})…"
                }
                Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onEmergency) { Text("Nödsamtal / telefon") }
    }
}
