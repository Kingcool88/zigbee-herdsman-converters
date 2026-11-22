package se.zenmode.app.core

import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.content.Context
import android.util.Log
import se.zenmode.app.data.SavedDnd
import se.zenmode.app.data.ZenStore

/**
 * Styr Androids Stör ej-läge. Kräver "Åtkomst till Stör ej" (ACCESS_NOTIFICATION_POLICY).
 *
 * Under zen: inga notiser och inga samtal, utom
 *  - alarm (väckarklockan ska fungera),
 *  - stjärnmärkta kontakter om användaren valt det,
 *  - kontakter i appens egen lista (släpps igenom av [se.zenmode.app.service.ZenCallScreeningService]).
 */
object DndManager {
    private const val TAG = "ZenDnd"
    const val TEMP_RING_MS = 60_000L

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun hasAccess(ctx: Context): Boolean = nm(ctx).isNotificationPolicyAccessGranted

    fun enable(ctx: Context) {
        if (!hasAccess(ctx)) {
            Log.w(TAG, "Saknar Stör ej-åtkomst – notiser blockeras inte")
            return
        }
        val store = ZenStore.get(ctx)
        val nm = nm(ctx)
        // Spara bara användarens ursprungliga läge en gång (inte om vi redan är aktiva, t.ex. efter omstart).
        if (store.savedDnd == null) {
            val p = nm.notificationPolicy
            store.savedDnd = SavedDnd(
                filter = nm.currentInterruptionFilter,
                categories = p.priorityCategories,
                callSenders = p.priorityCallSenders,
                messageSenders = p.priorityMessageSenders,
                suppressedEffects = p.suppressedVisualEffects,
            )
        }
        applyZenPolicy(ctx)
    }

    private fun applyZenPolicy(ctx: Context) {
        val store = ZenStore.get(ctx)
        var categories = Policy.PRIORITY_CATEGORY_ALARMS
        if (store.allowStarred) categories = categories or Policy.PRIORITY_CATEGORY_CALLS
        val suppressed = Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
            Policy.SUPPRESSED_EFFECT_LIGHTS or
            Policy.SUPPRESSED_EFFECT_PEEK or
            Policy.SUPPRESSED_EFFECT_STATUS_BAR or
            Policy.SUPPRESSED_EFFECT_BADGE or
            Policy.SUPPRESSED_EFFECT_AMBIENT or
            Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST
        try {
            val nm = nm(ctx)
            nm.notificationPolicy = Policy(categories, Policy.PRIORITY_SENDERS_STARRED, Policy.PRIORITY_SENDERS_STARRED, suppressed)
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } catch (e: SecurityException) {
            Log.e(TAG, "Kunde inte sätta Stör ej", e)
        }
    }

    /** Ett samtal från en tillåten kontakt: släpp på ljudet en kort stund så att det ringer. */
    fun allowRingingTemporarily(ctx: Context) {
        if (!hasAccess(ctx)) return
        nm(ctx).setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        AlarmScheduler.setDndRestore(ctx, System.currentTimeMillis() + TEMP_RING_MS)
    }

    fun reapplyIfActive(ctx: Context) {
        if (ZenStore.get(ctx).state.active) applyZenPolicy(ctx)
    }

    fun restore(ctx: Context) {
        val store = ZenStore.get(ctx)
        val saved = store.savedDnd ?: return
        if (hasAccess(ctx)) {
            try {
                val nm = nm(ctx)
                nm.notificationPolicy = Policy(saved.categories, saved.callSenders, saved.messageSenders, saved.suppressedEffects)
                val filter = if (saved.filter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    saved.filter
                }
                nm.setInterruptionFilter(filter)
            } catch (e: SecurityException) {
                Log.e(TAG, "Kunde inte återställa Stör ej", e)
            }
        }
        store.savedDnd = null
    }
}
