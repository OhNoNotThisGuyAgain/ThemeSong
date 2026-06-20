package com.spotzones.automation.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.spotzones.R
import com.spotzones.core.notification.NotificationChannels
import com.spotzones.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the foreground + transition notifications. Pure builder — no side effects. */
@Singleton
class AutomationNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun foreground(activeZoneName: String?, paused: Boolean): Notification {
        val title = context.getString(R.string.automation_running)
        val text = when {
            paused -> context.getString(R.string.automation_paused)
            activeZoneName != null -> context.getString(R.string.automation_active_zone, activeZoneName)
            else -> context.getString(R.string.automation_no_zone)
        }

        val builder = NotificationCompat.Builder(context, NotificationChannels.AUTOMATION)
            .setSmallIcon(R.drawable.ic_stat_zone)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (paused) {
            builder.addAction(0, context.getString(R.string.action_resume), action(AutomationService.ACTION_RESUME))
        } else {
            builder.addAction(0, context.getString(R.string.action_pause), action(AutomationService.ACTION_PAUSE))
        }
        builder.addAction(0, context.getString(R.string.action_skip), action(AutomationService.ACTION_SKIP))
        return builder.build()
    }

    fun transition(zoneName: String, playlistName: String?): Notification =
        NotificationCompat.Builder(context, NotificationChannels.TRANSITIONS)
            .setSmallIcon(R.drawable.ic_stat_zone)
            .setContentTitle(zoneName)
            .setContentText(playlistName?.let { "Now playing $it" } ?: "Zone music started")
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(actionName: String): PendingIntent {
        val intent = Intent(context, AutomationService::class.java).setAction(actionName)
        return PendingIntent.getService(
            context,
            actionName.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val FOREGROUND_ID = 1001
        const val TRANSITION_ID_BASE = 2000
    }
}
