package com.spotzones.location

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import com.spotzones.core.permissions.PermissionChecker
import com.spotzones.domain.location.ContextProvider
import com.spotzones.domain.location.LocationProvider
import com.spotzones.domain.model.AudioRoute
import com.spotzones.domain.model.BluetoothDeviceInfo
import com.spotzones.domain.model.EvaluationContext
import com.spotzones.domain.model.MovementState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds an [EvaluationContext] snapshot from the device's current sensors and system state.
 *
 * Every signal is read defensively: missing permissions, absent hardware or transient failures
 * collapse to a null/neutral value rather than throwing, so the rule engine always receives a usable
 * context. Movement state is supplied by [MovementTracker] (Activity Recognition) when available.
 */
@Singleton
class ContextProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationProvider: LocationProvider,
    private val movementTracker: MovementTracker,
    private val permissions: PermissionChecker,
) : ContextProvider {

    override suspend fun currentContext(): EvaluationContext {
        val now = LocalDateTime.now()
        val location = locationProvider.lastKnownLocation().getOrNull()
        val audioRoute = currentAudioRoute()

        return EvaluationContext(
            location = location,
            locationAccuracyMeters = null,
            time = now.toLocalTime(),
            dayOfWeek = now.dayOfWeek,
            speedMph = movementTracker.currentSpeedMph(),
            movementState = movementTracker.currentState(),
            batteryPercent = batteryPercent(),
            isCharging = isCharging(),
            connectedBluetooth = connectedBluetooth(),
            wifiSsid = wifiSsid(),
            audioRoute = audioRoute,
            headphonesConnected = audioRoute in HEADPHONE_ROUTES,
        )
    }

    private fun batteryPercent(): Int? {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    @SuppressLint("MissingPermission")
    private fun connectedBluetooth(): Set<BluetoothDeviceInfo> {
        if (!permissions.hasBluetoothConnect()) return emptySet()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return emptySet()
        val adapter: BluetoothAdapter = manager.adapter ?: return emptySet()
        if (!adapter.isEnabled) return emptySet()
        return try {
            // Devices currently connected for audio (A2DP / Headset) are the ones users care about.
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                BluetoothDeviceInfo(address = device.address, name = device.name, isAudio = true)
            }.toSet()
        } catch (e: SecurityException) {
            emptySet()
        }
    }

    @Suppress("DEPRECATION")
    private fun wifiSsid(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        // SSID requires location permission on modern Android; degrade gracefully if denied.
        if (!permissions.hasAnyLocation()) return null
        val active = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(active) ?: return null
        if (!caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return null
        return wifi.connectionInfo?.ssid?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    }

    private fun currentAudioRoute(): AudioRoute {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return AudioRoute.UNKNOWN
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var hasBtA2dp = false
        var hasWired = false
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> hasWired = true
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> hasBtA2dp = true
            }
        }
        return when {
            hasWired -> AudioRoute.WIRED_HEADPHONES
            hasBtA2dp -> AudioRoute.BLUETOOTH_HEADPHONES
            else -> AudioRoute.SPEAKER
        }
    }

    private companion object {
        val HEADPHONE_ROUTES = setOf(
            AudioRoute.WIRED_HEADPHONES,
            AudioRoute.BLUETOOTH_HEADPHONES,
            AudioRoute.BLUETOOTH_CAR,
        )
    }
}
