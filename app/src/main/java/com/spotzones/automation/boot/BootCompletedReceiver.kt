package com.spotzones.automation.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spotzones.automation.work.GeofenceSyncWorker

/**
 * After a reboot or app update, geofences are gone. Re-arm them so automation keeps working without
 * the user re-opening the app. The actual (async) work is delegated to [GeofenceSyncWorker].
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                GeofenceSyncWorker.enqueue(context)
        }
    }
}
