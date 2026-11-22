package se.zenmode.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import se.zenmode.app.core.AlarmScheduler
import se.zenmode.app.core.DndManager
import se.zenmode.app.core.ZenController

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_END -> ZenController.onEndAlarm(context)
            AlarmScheduler.ACTION_RELOCK -> ZenController.relock(context)
            AlarmScheduler.ACTION_SCHEDULE_START -> ZenController.onScheduleAlarm(context)
            AlarmScheduler.ACTION_DND_RESTORE -> DndManager.reapplyIfActive(context)
        }
    }
}
