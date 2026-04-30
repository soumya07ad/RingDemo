package com.dkgs.innerpulse.ble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dkgs.innerpulse.MainActivity
import com.dkgs.innerpulse.R
import com.dkgs.innerpulse.data.ble.JMRingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the BLE connection to the smart ring alive
 * even when the app is in the background. Android requires a persistent
 * notification for this, which also provides user feedback on connection state.
 *
 * Lifecycle:
 *  - Started by RingViewModel when the user connects to a ring.
 *  - Stopped when the user explicitly disconnects, or the ring disconnects
 *    and no reconnection succeeds.
 */
class RingService : Service() {

    companion object {
        private const val TAG = "RingService"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "ring_connection_channel"

        // Intent actions
        const val ACTION_START = "com.dkgs.innerpulse.ble.ACTION_START"
        const val ACTION_STOP = "com.dkgs.innerpulse.ble.ACTION_STOP"

        /** Convenience helper to start the service */
        fun start(context: Context) {
            val intent = Intent(context, RingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Convenience helper to stop the service */
        fun stop(context: Context) {
            val intent = Intent(context, RingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RingService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Received STOP action — stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                Log.i(TAG, "Received START action — promoting to foreground")
                val notification = buildNotification("Connecting to ring…")
                startForeground(NOTIFICATION_ID, notification)
                observeConnectionState()
            }
        }
        // If the system kills us, restart automatically so the connection survives
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "RingService destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    // ═══════════════════════════════════
    // Notification Management
    // ═══════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Ring Connection",
                NotificationManager.IMPORTANCE_LOW // Silent — no sound or vibration
            ).apply {
                description = "Keeps the smart ring connection alive in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        // Tapping the notification opens the app
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Disconnect" action button
        val stopIntent = Intent(this, RingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("InnerPulse")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                stopPending
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    // ═══════════════════════════════════
    // Connection State Observer
    // ═══════════════════════════════════

    private fun observeConnectionState() {
        val ringManager = JMRingManager.getInstance(applicationContext)

        serviceScope.launch {
            ringManager.connectionState.collectLatest { state ->
                when (state) {
                    is BleConnectionState.Connected -> {
                        val battery = ringManager.ringData.value.battery
                        val batteryText = if (battery != null) " • Battery: $battery%" else ""
                        updateNotification("Ring connected$batteryText")
                    }
                    is BleConnectionState.Connecting -> {
                        updateNotification("Connecting to ring…")
                    }
                    is BleConnectionState.Disconnected -> {
                        updateNotification("Ring disconnected")
                        // Don't auto-stop — the JMRingManager retry logic may reconnect
                    }
                    is BleConnectionState.Error -> {
                        updateNotification("Connection error: ${state.message}")
                    }
                }
            }
        }

        // Also observe battery updates to keep the notification fresh
        serviceScope.launch {
            ringManager.ringData.collectLatest { data ->
                if (ringManager.connectionState.value is BleConnectionState.Connected) {
                    val batteryText = if (data.battery != null) " • Battery: ${data.battery}%" else ""
                    val stepsText = if (data.steps > 0) " • Steps: ${data.steps}" else ""
                    updateNotification("Ring connected$batteryText$stepsText")
                }
            }
        }
    }
}
