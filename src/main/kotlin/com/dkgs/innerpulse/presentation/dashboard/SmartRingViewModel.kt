package com.dkgs.innerpulse.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.model.RingConnectionState
import com.dkgs.innerpulse.domain.repository.IRingRepository
import com.dkgs.innerpulse.domain.repository.ISettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Smart Ring connection management on the Dashboard.
 *
 * Observes the ring repository's connection status and exposes a simplified
 * RingConnectionState for the UI. Provides disconnect and auto-reconnect actions.
 *
 * Auto-reconnect flow:
 * - On Dashboard load, checks if a saved MAC address exists in settings.
 * - If the ring is not already connected, starts a BLE scan.
 * - When the saved ring is found in scan results, auto-connects.
 * - This enables "connect once via MAC, reconnect via BLE scan" behavior.
 */
class SmartRingViewModel(
    private val ringRepository: IRingRepository,
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SmartRingViewModel"
    }

    private val _connectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RingConnectionState> = _connectionState.asStateFlow()

    private val _isAutoReconnecting = MutableStateFlow(false)
    val isAutoReconnecting: StateFlow<Boolean> = _isAutoReconnecting.asStateFlow()

    val pairedRing = settingsRepository.pairedRings

    init {
        observeConnectionStatus()
    }

    /**
     * Observe the repository's connection status and map it to the simplified enum.
     */
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            ringRepository.connectionStatus.collect { status ->
                _connectionState.value = when (status) {
                    is ConnectionStatus.Connected -> {
                        _isAutoReconnecting.value = false
                        RingConnectionState.CONNECTED
                    }
                    is ConnectionStatus.Connecting -> RingConnectionState.CONNECTING
                    else -> RingConnectionState.DISCONNECTED
                }
            }
        }
    }

    /**
     * Auto-reconnect to the previously paired ring.
     *
     * Flow:
     * 1. Check if a saved MAC address exists (from first-time pairing).
     * 2. If ring is already connected, skip.
     * 3. Start a BLE scan to discover nearby devices.
     * 4. When the saved ring appears in scan results, connect to it.
     * 5. If not found within timeout, stop gracefully.
     *
     * This is called once per Dashboard lifecycle to avoid repeated scans.
     */
    private var autoReconnectAttempted = false

    fun autoReconnect() {
        if (autoReconnectAttempted) return
        autoReconnectAttempted = true
        attemptSmartConnect(onReconnectFailed = {})
    }

    /**
     * Manually trigger a smart connection attempt.
     */
    fun manualReconnect(onReconnectFailed: () -> Unit) {
        attemptSmartConnect(onReconnectFailed = onReconnectFailed)
    }

    private var lastAttemptTime = 0L

    private fun attemptSmartConnect(onReconnectFailed: () -> Unit) {
        // Prevent spamming attempts (at least 2 seconds between starts)
        val now = System.currentTimeMillis()
        if (now - lastAttemptTime < 2000) return
        lastAttemptTime = now

        val savedMac = settingsRepository.ringMacAddress.value
        val savedRingType = settingsRepository.ringType.value

        // No saved ring — first-time user, skip
        if (savedMac.isBlank()) {
            Log.d(TAG, "No saved MAC address found. Skipping smart connect.")
            onReconnectFailed()
            return
        }

        // Already connected — nothing to do
        if (ringRepository.isConnected()) {
            Log.d(TAG, "Ring already connected. Skipping smart connect.")
            return
        }

        Log.i(TAG, "Smart Connect: Starting BLE scan to find saved ring ($savedMac)")
        _isAutoReconnecting.value = true

        viewModelScope.launch {
            try {
                // Start BLE scan
                ringRepository.startScan(durationSeconds = 8)

                // Poll scan results for up to 10 seconds looking for our saved ring
                var found = false
                val startTime = System.currentTimeMillis()
                val timeoutMs = 10_000L

                while (!found && (System.currentTimeMillis() - startTime) < timeoutMs) {
                    delay(1000) // Check every second

                    // If connected during scan (e.g., SDK auto-reconnected), stop
                    if (ringRepository.isConnected()) {
                        Log.i(TAG, "Ring connected during scan. Stopping smart connect scan.")
                        found = true
                        break
                    }

                    // Check scan results from the repository's scan status
                    val scanStatus = ringRepository.scanStatus.value
                    val devices = scanStatus.getDevicesOrEmpty()

                    val matchingDevice = devices.find { device ->
                        device.macAddress.replace(":", "").lowercase() ==
                            savedMac.replace(":", "").lowercase()
                    }

                    if (matchingDevice != null) {
                        Log.i(TAG, "Smart Connect: Found saved ring! Connecting to ${matchingDevice.macAddress}")
                        ringRepository.stopScan()
                        ringRepository.connect(savedMac, matchingDevice.name, savedRingType)
                        found = true
                    }
                }

                if (!found) {
                    Log.w(TAG, "Smart Connect: Saved ring not found within timeout.")
                    ringRepository.stopScan()
                    _isAutoReconnecting.value = false
                    onReconnectFailed()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Smart Connect failed", e)
                _isAutoReconnecting.value = false
                onReconnectFailed()
            }
        }
    }

    /**
     * Disconnect the ring:
     * - Stops BLE communication via the repository
     * - Resets connection state to DISCONNECTED
     * - Does NOT clear the saved MAC (allows future auto-reconnect)
     */
    fun disconnectRing() {
        viewModelScope.launch {
            ringRepository.disconnect()
            _connectionState.value = RingConnectionState.DISCONNECTED
        }
    }

    /**
     * Forget the ring completely:
     * - Disconnects from the ring
     * - Clears the saved MAC address so auto-reconnect won't trigger
     * - User must re-pair via the Ring Setup screen
     */
    fun forgetRing() {
        viewModelScope.launch {
            ringRepository.disconnect()
            settingsRepository.setRingMacAddress("")
            _connectionState.value = RingConnectionState.DISCONNECTED
            autoReconnectAttempted = false
            Log.i(TAG, "Ring forgotten. Saved MAC address cleared.")
        }
    }
}
