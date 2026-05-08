package com.dkgs.innerpulse.data.ble

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.crrepa.ble.CRPBleClient
import com.crrepa.ble.conn.CRPBleConnection
import com.crrepa.ble.conn.CRPBleDevice
import com.crrepa.ble.conn.bean.CRPHeartRateInfo
import com.crrepa.ble.conn.bean.CRPSleepInfo
import com.crrepa.ble.conn.bean.CRPStepsInfo
import com.crrepa.ble.conn.listener.CRPBleConnectionStateListener
import com.crrepa.ble.conn.listener.CRPHeartRateChangeListener
import com.crrepa.ble.conn.listener.CRPSleepChangeListener
import com.crrepa.ble.conn.listener.CRPStepsChangeListener
import com.crrepa.ble.scan.bean.CRPScanDevice
import com.crrepa.ble.scan.callback.CRPScanCallback
import com.dkgs.innerpulse.FitnessApplication
import com.dkgs.innerpulse.ble.BleConnectionState
import com.dkgs.innerpulse.ble.BleDevice
import com.dkgs.innerpulse.ble.RingData
import com.dkgs.innerpulse.domain.model.Ring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.crrepa.ble.conn.listener.CRPBatteryListener
import com.crrepa.ble.conn.listener.CRPBloodOxygenChangeListener
import com.crrepa.ble.conn.listener.CRPHrvChangeListener
import com.crrepa.ble.conn.listener.CRPStressChangeListener

/**
 * Manager for Crrepa Smart Ring SDK (MYRingSDK).
 * Replaces the old JMRingManager.
 */
class CrrepaRingManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CrrepaRingManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: CrrepaRingManager? = null

        fun getInstance(context: Context): CrrepaRingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrrepaRingManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val bleClient: CRPBleClient? by lazy {
        (context.applicationContext as? FitnessApplication)?.getBleClient()
    }

    private val bluetoothAdapter by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State Flows
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()

    private val _scanResults = MutableStateFlow<List<CRPScanDevice>>(emptyList())
    val scanResults: StateFlow<List<CRPScanDevice>> = _scanResults.asStateFlow()

    private var bleDevice: CRPBleDevice? = null
    private var bleConnection: CRPBleConnection? = null
    private var connectedRing: Ring? = null

    // ═══════════════════════════════════
    // Connectivity
    // ═══════════════════════════════════

    fun connectRing(macAddress: String) {
        val client = bleClient ?: return
        
        Log.i(TAG, "Connecting to ring: $macAddress")
        _connectionState.value = BleConnectionState.Connecting
        
        bleDevice = client.getBleDevice(macAddress)
        bleConnection = bleDevice?.connect()
        
        bleConnection?.setConnectionStateListener(object : CRPBleConnectionStateListener {
            override fun onConnectionStateChange(newState: Int) {
                Log.d(TAG, "onConnectionStateChange: $newState")
                when (newState) {
                    CRPBleConnectionStateListener.STATE_CONNECTED -> {
                        Log.i(TAG, "✓ Connected to Crrepa Ring")
                        val ring = Ring(macAddress = macAddress, name = "Smart Ring", isConnected = true)
                        connectedRing = ring
                        _connectionState.value = BleConnectionState.Connected(ring)
                        setupDataListeners()
                        syncTime()
                        // Query initial data
                        fetchCachedData()
                    }
                    CRPBleConnectionStateListener.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Ring disconnected")
                        _connectionState.value = BleConnectionState.Disconnected
                        connectedRing = null
                    }
                }
            }
        })
    }

    private fun setupDataListeners() {
        val conn = bleConnection ?: return
        
        // 1. Battery Listener
        conn.setBatteryListener(object : CRPBatteryListener {
            override fun onBattery(battery: Int) {
                Log.d(TAG, "Battery: $battery%")
                _ringData.value = _ringData.value.copy(battery = battery)
            }

            override fun onRealTimeBattery(battery: Int, mv: Int) {
                _ringData.value = _ringData.value.copy(battery = battery)
            }
        })

        // 2. Steps / Distance / Calories
        conn.setStepsChangeListener(object : CRPStepsChangeListener {
            override fun onCurrentSteps(info: CRPStepsInfo) {
                Log.d(TAG, "Steps update: ${info.steps}")
                updateStepsInfo(info)
            }

            override fun onHistorySteps(p0: com.crrepa.ble.conn.type.CRPHistoryDay?, p1: CRPStepsInfo?) {}
            override fun onHistoryStepsDetails(p0: com.crrepa.ble.conn.bean.CRPStepsDetailsInfo?) {}
        })

        // 3. Heart Rate
        conn.setHeartRateChangeListener(object : CRPHeartRateChangeListener {
            override fun onRealtimeHeartRate(hr: Int) {
                Log.d(TAG, "Heart rate update: $hr")
                _ringData.value = _ringData.value.copy(heartRate = hr, lastUpdate = System.currentTimeMillis())
            }

            override fun onHeartRate(hr: Int) {
                _ringData.value = _ringData.value.copy(heartRate = hr, lastUpdate = System.currentTimeMillis())
            }

            override fun onTimingHeartRate(p0: CRPHeartRateInfo?) {}
            override fun onHistoryHeartRate(p0: MutableList<com.crrepa.ble.conn.bean.CRPHistoryHeartRateInfo>?) {}
            override fun onTimingInterval(p0: Int) {}
        })

        // 4. Blood Oxygen (SpO2)
        conn.setBloodOxygenChangeListener(object : CRPBloodOxygenChangeListener {
            override fun onBloodOxygen(bloodOxygen: Int) {
                Log.d(TAG, "SpO2 update: $bloodOxygen%")
                _ringData.value = _ringData.value.copy(spO2 = bloodOxygen.toFloat(), spO2Measuring = false)
            }

            override fun onHistoryBloodOxygen(p0: MutableList<com.crrepa.ble.conn.bean.CRPHistoryBloodOxygenInfo>?) {}
            override fun onTimingBloodOxygen(p0: com.crrepa.ble.conn.bean.CRPTimingBloodOxygenInfo?) {}
            override fun onTimingInterval(p0: Int) {}
            override fun onSupportBloodOxygenType(p0: com.crrepa.ble.conn.type.CRPBloodOxygenType?) {}
        })

        // 5. Stress
        conn.setStressChangeListener(object : CRPStressChangeListener {
            override fun onStressChange(stress: Int) {
                Log.d(TAG, "Stress update: $stress")
                _ringData.value = _ringData.value.copy(stress = stress, stressMeasuring = false)
            }

            override fun onHistoryStressChange(p0: MutableList<com.crrepa.ble.conn.bean.CRPHistoryStressInfo>?) {}
            override fun onTimingInterval(p0: Int) {}
            override fun onTimingStress(p0: com.crrepa.ble.conn.bean.CRPTimingStressInfo?) {}
        })

        // 6. Sleep
        conn.setSleepChangeListener(object : CRPSleepChangeListener {
            override fun onSleepInfo(info: CRPSleepInfo) {
                Log.d(TAG, "Sleep update: ${info.totalTime} mins")
                _ringData.value = _ringData.value.copy(
                    sleepData = _ringData.value.sleepData.copy(
                        totalMinutes = info.totalTime,
                        lightMinutes = info.lightTime,
                        deepMinutes = info.deepTime
                    )
                )
            }

            override fun onHistorySleepChange(p0: com.crrepa.ble.conn.type.CRPHistoryDay?, p1: CRPSleepInfo?) {}
            override fun onHistorySleepListChange(p0: MutableList<com.crrepa.ble.conn.bean.CRPHistorySleepTimeInfo>?) {}
            override fun onSleepDetails(p0: com.crrepa.ble.conn.bean.CRPSleepDetailsInfo?) {}
            override fun onSleepChronotype(p0: com.crrepa.ble.conn.bean.CRPSleepChronotypeInfo?) {}
            override fun onSleepEnd(p0: Boolean) {}
        })
    }

    private fun updateStepsInfo(info: CRPStepsInfo) {
        _ringData.value = _ringData.value.copy(
            steps = info.steps,
            distance = info.distance,
            calories = info.calories,
            lastUpdate = System.currentTimeMillis()
        )
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting from ring")
        bleDevice?.disconnect()
        bleConnection?.close()
        _connectionState.value = BleConnectionState.Disconnected
    }

    private fun syncTime() {
        Log.i(TAG, "Syncing time with ring")
        bleConnection?.syncTime()
    }

    // ═══════════════════════════════════
    // Scanning (Native + SDK)
    // ═══════════════════════════════════

    private var nativeScanCallback: ScanCallback? = null

    fun startScan() {
        // Start SDK Scan first
        startSdkScan()
        // Also start Native Scan for better discovery
        startNativeScan()
    }

    private fun startSdkScan() {
        val client = bleClient ?: return
        Log.i(TAG, "Starting SDK BLE scan")
        client.scanDevice(object : CRPScanCallback {
            override fun onScanning(device: CRPScanDevice) {
                val name = device.device.name
                // Filter out dummy/unrelated BLE devices
                if (!name.isNullOrBlank() && 
                    (name.contains("Ring", ignoreCase = true) || 
                     name.contains("MY", ignoreCase = true) || 
                     name.contains("CR", ignoreCase = true))) {
                    addScanResult(device)
                }
            }

            override fun onScanComplete(results: MutableList<CRPScanDevice>?) {}
        }, 15000)
    }

    private fun startNativeScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        Log.i(TAG, "Starting Native BLE scan")
        
        nativeScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = result.scanRecord?.deviceName ?: device.name
                if (name != null && (name.contains("Ring", ignoreCase = true) || name.contains("MY", ignoreCase = true))) {
                    Log.d(TAG, "Native scan found: $name [${device.address}]")
                    // We can't easily convert ScanResult to CRPScanDevice without internal SDK knowledge,
                    // but usually SDK scan catches it if it's visible to native scan.
                    // If SDK scan misses it, we might need to use getBleDevice(address) directly.
                }
            }
        }
        scanner.startScan(nativeScanCallback)
    }

    private fun addScanResult(device: CRPScanDevice) {
        val currentList = _scanResults.value.toMutableList()
        if (currentList.none { it.device.address == device.device.address }) {
            currentList.add(device)
            _scanResults.value = currentList
        }
    }

    fun stopScan() {
        Log.i(TAG, "Stopping scans")
        bleClient?.cancelScan()
        nativeScanCallback?.let {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
            nativeScanCallback = null
        }
    }

    // ═══════════════════════════════════
    // Measurements
    // ═══════════════════════════════════

    fun startMeasurement(type: Int) {
        val conn = bleConnection ?: return
        Log.i(TAG, "Starting measurement type: $type")
        when (type) {
            1 -> { // HR
                conn.startMeasureHeartRate()
                _ringData.value = _ringData.value.copy(heartRateMeasuring = true)
            }
            2 -> { // SpO2
                conn.startMeasureBloodOxygen()
                _ringData.value = _ringData.value.copy(spO2Measuring = true)
            }
            7 -> { // Stress
                conn.startMeasureStress()
                _ringData.value = _ringData.value.copy(stressMeasuring = true)
            }
        }
    }

    fun stopMeasurement(type: Int) {
        val conn = bleConnection ?: return
        Log.i(TAG, "Stopping measurement type: $type")
        when (type) {
            1 -> {
                conn.stopMeasureHeartRate()
                _ringData.value = _ringData.value.copy(heartRateMeasuring = false)
            }
            2 -> {
                conn.stopMeasureBloodOxygen()
                _ringData.value = _ringData.value.copy(spO2Measuring = false)
            }
            7 -> {
                conn.stopMeasureStress()
                _ringData.value = _ringData.value.copy(stressMeasuring = false)
            }
        }
    }

    fun fetchCachedData() {
        val conn = bleConnection ?: return
        Log.i(TAG, "Fetching cached data")
        conn.queryBattery()
        conn.queryCurrentSteps()
        conn.queryHistorySleep(com.crrepa.ble.conn.type.CRPHistoryDay.TODAY)
    }
}
