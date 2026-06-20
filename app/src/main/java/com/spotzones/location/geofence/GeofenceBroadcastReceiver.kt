package com.spotzones.location.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.spotzones.automation.work.AutomationWorker
import com.spotzones.domain.model.TransitionTrigger
import timber.log.Timber

/**
 * Entry point for OS geofence transitions. Kept deliberately thin: validate the event, then hand off
 * to [AutomationWorker] so the actual (suspending, network-touching) evaluation runs in a managed
 * background context rather than the tight broadcast window.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Timber.w("Geofence event error code=%d", event.errorCode)
            return
        }
        val trigger = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> TransitionTrigger.ZONE_ENTER
            Geofence.GEOFENCE_TRANSITION_EXIT -> TransitionTrigger.ZONE_EXIT
            else -> return
        }
        Timber.d("Geofence transition: %s (%d zones)", trigger, event.triggeringGeofences?.size ?: 0)
        AutomationWorker.enqueue(context, trigger)
    }
}
