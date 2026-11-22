package se.zenmode.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import se.zenmode.core.AllowedContact
import se.zenmode.core.GeoZone
import se.zenmode.core.PendingUnlock
import se.zenmode.core.Schedule
import se.zenmode.core.ZenSource
import se.zenmode.core.ZenState
import java.time.DayOfWeek

/** Sparade Stör ej-inställningar från innan zen-läget startade, så att de kan återställas. */
data class SavedDnd(
    val filter: Int,
    val categories: Int,
    val callSenders: Int,
    val messageSenders: Int,
    val suppressedEffects: Int,
)

/**
 * All persistent data. Ligger i "device protected storage" så att den kan läsas direkt efter omstart,
 * innan användaren har låst upp mobilen med PIN (Direct Boot).
 */
class ZenStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.createDeviceProtectedStorageContext().getSharedPreferences("zen", Context.MODE_PRIVATE)

    private val _changes = MutableStateFlow(0L)

    /** Ökas vid varje ändring – UI:t lyssnar på denna och läser om. */
    val changes: StateFlow<Long> = _changes

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        // commit() (synkront) så att tillståndet garanterat är sparat om processen dör direkt efter.
        prefs.edit().apply(block).commit()
        _changes.value = _changes.value + 1
    }

    var state: ZenState
        get() = prefs.getString(KEY_STATE, null)?.let(::stateFromJson) ?: ZenState()
        set(v) = edit { putString(KEY_STATE, stateToJson(v)) }

    var schedules: List<Schedule>
        get() = readArray(KEY_SCHEDULES) { o ->
            Schedule(
                id = o.getString("id"),
                name = o.optString("name"),
                days = o.getJSONArray("days").let { a -> (0 until a.length()).map { DayOfWeek.of(a.getInt(it)) }.toSet() },
                startMinuteOfDay = o.getInt("start"),
                durationMinutes = o.getInt("duration"),
                enabled = o.optBoolean("enabled", true),
            )
        }
        set(v) = writeArray(KEY_SCHEDULES, v) { s ->
            JSONObject()
                .put("id", s.id)
                .put("name", s.name)
                .put("days", JSONArray(s.days.map { it.value }.sorted()))
                .put("start", s.startMinuteOfDay)
                .put("duration", s.durationMinutes)
                .put("enabled", s.enabled)
        }

    var zones: List<GeoZone>
        get() = readArray(KEY_ZONES) { o ->
            GeoZone(
                id = o.getString("id"),
                name = o.optString("name"),
                latitude = o.getDouble("lat"),
                longitude = o.getDouble("lng"),
                radiusMeters = o.getDouble("radius").toFloat(),
                enabled = o.optBoolean("enabled", true),
            )
        }
        set(v) = writeArray(KEY_ZONES, v) { z ->
            JSONObject()
                .put("id", z.id)
                .put("name", z.name)
                .put("lat", z.latitude)
                .put("lng", z.longitude)
                .put("radius", z.radiusMeters.toDouble())
                .put("enabled", z.enabled)
        }

    var allowedContacts: List<AllowedContact>
        get() = readArray(KEY_CONTACTS) { o -> AllowedContact(o.optString("name"), o.getString("number")) }
        set(v) = writeArray(KEY_CONTACTS, v) { c -> JSONObject().put("name", c.name).put("number", c.number) }

    var pendingUnlock: PendingUnlock?
        get() = prefs.getString(KEY_PENDING, null)?.let {
            val o = JSONObject(it)
            PendingUnlock(o.getString("code"), o.getLong("requestedAt"), o.getLong("expiresAt"), o.optBoolean("denied"))
        }
        set(v) = edit {
            if (v == null) {
                remove(KEY_PENDING)
            } else {
                putString(
                    KEY_PENDING,
                    JSONObject()
                        .put("code", v.code)
                        .put("requestedAt", v.requestedAt)
                        .put("expiresAt", v.expiresAt)
                        .put("denied", v.denied)
                        .toString(),
                )
            }
        }

    var savedDnd: SavedDnd?
        get() = prefs.getString(KEY_SAVED_DND, null)?.let {
            val o = JSONObject(it)
            SavedDnd(o.getInt("f"), o.getInt("c"), o.getInt("cs"), o.getInt("ms"), o.getInt("se"))
        }
        set(v) = edit {
            if (v == null) {
                remove(KEY_SAVED_DND)
            } else {
                putString(
                    KEY_SAVED_DND,
                    JSONObject()
                        .put("f", v.filter)
                        .put("c", v.categories)
                        .put("cs", v.callSenders)
                        .put("ms", v.messageSenders)
                        .put("se", v.suppressedEffects)
                        .toString(),
                )
            }
        }

    var partnerNumber: String
        get() = prefs.getString("partner_number", "") ?: ""
        set(v) = edit { putString("partner_number", v.trim()) }

    var partnerName: String
        get() = prefs.getString("partner_name", "") ?: ""
        set(v) = edit { putString("partner_name", v.trim()) }

    var ownerName: String
        get() = prefs.getString("owner_name", "") ?: ""
        set(v) = edit { putString("owner_name", v.trim()) }

    /** Släpp även igenom samtal från stjärnmärkta kontakter (Androids egen Stör ej-funktion). */
    var allowStarred: Boolean
        get() = prefs.getBoolean("allow_starred", false)
        set(v) = edit { putBoolean("allow_starred", v) }

    var lastQuickMinutes: Int
        get() = prefs.getInt("last_quick_minutes", 60)
        set(v) = edit { putInt("last_quick_minutes", v) }

    private fun <T> readArray(key: String, map: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { map(arr.getJSONObject(it)) }
    }

    private fun <T> writeArray(key: String, items: List<T>, map: (T) -> JSONObject) {
        val arr = JSONArray()
        items.forEach { arr.put(map(it)) }
        edit { putString(key, arr.toString()) }
    }

    private fun stateToJson(s: ZenState): String =
        JSONObject()
            .put("active", s.active)
            .put("source", s.source?.name ?: JSONObject.NULL)
            .put("sourceId", s.sourceId ?: JSONObject.NULL)
            .put("startedAt", s.startedAt)
            .put("endsAt", s.endsAt ?: JSONObject.NULL)
            .put("graceUntil", s.graceUntil ?: JSONObject.NULL)
            .toString()

    private fun stateFromJson(raw: String): ZenState {
        val o = JSONObject(raw)
        fun optLong(k: String): Long? = if (o.isNull(k)) null else o.getLong(k)
        fun optStr(k: String): String? = if (o.isNull(k)) null else o.getString(k)
        return ZenState(
            active = o.optBoolean("active"),
            source = optStr("source")?.let { runCatching { ZenSource.valueOf(it) }.getOrNull() },
            sourceId = optStr("sourceId"),
            startedAt = o.optLong("startedAt"),
            endsAt = optLong("endsAt"),
            graceUntil = optLong("graceUntil"),
        )
    }

    companion object {
        const val GRACE_MINUTES = 5
        const val UNLOCK_REQUEST_VALID_MINUTES = 15
        const val UNLOCK_REQUEST_COOLDOWN_MS = 60_000L

        private const val KEY_STATE = "state"
        private const val KEY_SCHEDULES = "schedules"
        private const val KEY_ZONES = "zones"
        private const val KEY_CONTACTS = "allowed_contacts"
        private const val KEY_PENDING = "pending_unlock"
        private const val KEY_SAVED_DND = "saved_dnd"

        @Volatile
        private var instance: ZenStore? = null

        fun get(context: Context): ZenStore =
            instance ?: synchronized(this) { instance ?: ZenStore(context).also { instance = it } }
    }
}
