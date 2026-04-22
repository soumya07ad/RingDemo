package com.dkgs.innerpulse.data.ble

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dkgs.innerpulse.ble.BleConnectionState
import com.dkgs.innerpulse.ble.RingData
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.SleepData
import com.dkgs.innerpulse.domain.model.FirmwareInfo
import com.gps.track.jmring.bean.JMHealthAllBean
import com.gps.track.jmring.bean.JMScanBean
import com.gps.track.jmring.bean.JMSleepBean
import com.gps.track.jmring.bean.JMStressBean
import com.gps.track.jmring.ble.RingBleUtils
import com.gps.track.jmring.callback.JMMeasureResultListener
import com.gps.track.jmring.callback.JMRingAllCacheResultListener
import com.gps.track.jmring.callback.JMRingBatteryListener
import com.gps.track.jmring.callback.JMRingConnectListener
import com.gps.track.jmring.callback.JMRingHealthAllBeanListener
import com.gps.track.jmring.callback.JMRingSleepAllBeanListener
import com.gps.track.jmring.callback.JMRingStressBeanListener
import com.gps.track.jmring.callback.JMRingScanListener
import com.gps.track.jmring.utils.RingDataUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.ringDataStore: DataStore<Preferences> by preferencesDataStore(name = "ring_prefs")

/**
 * Manager for official JMRing SDK integration.
 * Replaces the custom NativeGattManager.
 */
class JMRingManager private constructor(private val context: Context) : 
    JMRingConnectListener, JMRingBatteryListener, JMRingHealthAllBeanListener, 
    JMRingStressBeanListener, JMRingSleepAllBeanListener, JMMeasureResultListener,
    JMRingAllCacheResultListener, JMRingScanListener {

    companion object {
        private const val TAG = "JMRingManager"
        private val RING_TYPE_KEY = intPreferencesKey("ring_type")

        @Volatile
        private var INSTANCE: JMRingManager? = null

        fun getInstance(context: Context): JMRingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JMRingManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State Flows
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _ringData = MutableStateFlow(RingData())
    val ringData: StateFlow<RingData> = _ringData.asStateFlow()

    private val _scanResults = MutableStateFlow<List<JMScanBean>>(emptyList())
    val scanResults: StateFlow<List<JMScanBean>> = _scanResults.asStateFlow()

    private var connectedRing: Ring? = null
    private var currentUserId: String = "12345" // Default or fetched from TokenManager
    private var measurementTimeoutJobs = mutableMapOf<Int, Job>() // type to Job
    
    private var connectionRetries = 0
    private val MAX_RETRIES = 5

    init {
        Log.i(TAG, "Initializing JMRingManager with official SDK")
        setupListeners()
    }

    private fun setupListeners() {
        RingBleUtils.setJMRingConnectListener(this) // SDK-level connect listener
        val manager = RingBleUtils.getRingBleManager()
        manager.setJMRingConnectListener(this)
        manager.setJMRingBatteryListener(this)
        manager.setJMRingHealthAllBeanListener(this)
        manager.setJMRingStressBeanListener(this)
        manager.setJMRingSleepAllBeanListener(this)
        manager.setJMMeasureResultListener(this)
        manager.setJMRingAllCacheResultListener(this)
        RingBleUtils.setJMRingScanListener(this)
    }

    // ═══════════════════════════════════
    // Connectivity
    // ═══════════════════════════════════

    fun connectRing(userId: String, macAddress: String, ringType: Int) {
        connectionRetries = 0
        val formattedMac = RingBleUtils.formatMacAddress(macAddress)
        val sn = "780901703208128" // Default SN from Demo SDK for compatibility
        // Demo app ALWAYS hardcodes ringType=2 in setRingData (see MainActivity.kt line 68/84).
        // Using type=1 triggers an AIZO cloud auth request that rejects our package name,
        // causing an infinite authentication retry loop that blocks onConnectSuccess.
        val safeRingType = 2
        
        currentUserId = userId
        connectedRing = Ring(macAddress = formattedMac, name = "JMRing", isConnected = false)
        Log.i(TAG, "Connecting to ring: $formattedMac, type: $safeRingType (requested: $ringType), SN: $sn")
        
        scope.launch {
            saveRingType(safeRingType)
        }

        RingBleUtils.setRingData(userId, formattedMac, sn, safeRingType)
        
        // IMPORTANT: Re-setup listeners here. The SDK swaps the underlying manager 
        // (e.g., to YcbtRingBleManager for Xiaoqi rings) inside setRingData.
        setupListeners()
        
        RingBleUtils.getRingBleManager().onConnect()
    }

    fun disconnect() {
        Log.i(TAG, "Disconnecting from ring")
        RingBleUtils.getRingBleManager().onDisconnect()
    }

    fun cleanup() {
        val manager = RingBleUtils.getRingBleManager()
        manager.removeJMRingConnectListener(this)
        manager.removeJMRingBatteryListener(this)
        manager.removeJMRingHealthAllBeanListener(this)
        manager.removeJMRingStressBeanListener(this)
        manager.removeJMRingSleepAllBeanListener(this)
        manager.removeJMMeasureResultListener(this)
        manager.removeJMRingAllCacheResultListener(this)
        RingBleUtils.removeJMRingConnectListener(this)
        RingBleUtils.stopScanBle()
    }

    // ═══════════════════════════════════
    // Scanning
    // ═══════════════════════════════════

    fun startScan() {
        Log.i(TAG, "Starting BLE scan")
        _scanResults.value = emptyList()
        RingBleUtils.startScanBleDevice()
    }

    fun stopScan() {
        Log.i(TAG, "Stopping BLE scan")
        RingBleUtils.stopScanBle()
    }

    override fun onScanListener(list: List<JMScanBean>) {
        _scanResults.value = list
    }

    // ═══════════════════════════════════
    // Measurements & Data
    // ═══════════════════════════════════

    fun startMeasurement(type: Int) {
        Log.i(TAG, "Starting real-time measurement type: $type")
        
        // Update local measuring flag based on type
        _ringData.value = _ringData.value.let { data ->
            when (type) {
                1 -> data.copy(heartRateMeasuring = true)
                2 -> data.copy(spO2Measuring = true)
                7 -> data.copy(stressMeasuring = true)
                else -> data
            }
        }
        
        // Start a safety timeout (demo recommends 1 min)
        measurementTimeoutJobs[type]?.cancel()
        measurementTimeoutJobs[type] = scope.launch {
            delay(65000) // 1 minute + 5s buffer
            Log.w(TAG, "Measurement type $type timed out locally")
            _ringData.value = _ringData.value.let { data ->
                when (type) {
                    1 -> data.copy(heartRateMeasuring = false)
                    2 -> data.copy(spO2Measuring = false)
                    7 -> data.copy(stressMeasuring = false)
                    else -> data
                }
            }
        }
        
        RingBleUtils.getRingBleManager().startStopMeasurement(type)
    }

    fun stopMeasurement(type: Int) {
        Log.i(TAG, "Stopping measurement type: $type")
        
        measurementTimeoutJobs[type]?.cancel()
        _ringData.value = _ringData.value.let { data ->
            when (type) {
                1 -> data.copy(heartRateMeasuring = false)
                2 -> data.copy(spO2Measuring = false)
                7 -> data.copy(stressMeasuring = false)
                else -> data
            }
        }
        
        RingBleUtils.getRingBleManager().startStopMeasurement(type)
    }

    fun requestSleepScore() {
        Log.i(TAG, "Computing sleep scores")
        val calendar = java.util.Calendar.getInstance()
        // Reset to midnight as seen in demo zeroFromHour
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        RingDataUtils.computeAllSleepScore(calendar.timeInMillis, true) { healthScore, sleepScore, _ ->
            Log.i(TAG, "Scores computed: Health=$healthScore, Sleep=$sleepScore")
            _ringData.value = _ringData.value.copy(
                sleepData = _ringData.value.sleepData.copy(
                    quality = sleepScore.toInt()
                ),
                lastUpdate = System.currentTimeMillis()
            )
        }
    }

    fun fetchCachedData() {
        Log.i(TAG, "Fetching cached data from ring")
        RingBleUtils.getRingBleManager().handlerCacheRing(TAG)
    }

    // ═══════════════════════════════════
    // SDK Callbacks
    // ═══════════════════════════════════

    override fun onConnecting() {
        Log.i(TAG, "Callback: onConnecting")
        _connectionState.value = BleConnectionState.Connecting
    }

    override fun onConnectSuccess() {
        connectionRetries = 0
        Log.i(TAG, "Callback: onConnectSuccess - SUCCESSFULLY CONNECTED")
        val ring = connectedRing?.copy(isConnected = true) ?: Ring(macAddress = "", name = "JMRing", isConnected = true)
        _connectionState.value = BleConnectionState.Connected(ring)
        
        // Auto-fetch historical data immediately after connection as requested
        val currentTime = System.currentTimeMillis()
        val manager = RingBleUtils.getRingBleManager()
        Log.i(TAG, "Auto-fetching historical data (Health, Sleep, Stress)")
        manager.getActivityHealthData(currentTime)
        manager.getActivitySleepData(currentTime)
        manager.getActivityStressData(currentTime)
        
        fetchCachedData()
    }

    override fun onDisconnect(isCallback: Boolean) {
        Log.i(TAG, "Ring disconnected (isCallback=$isCallback)")
        _connectionState.value = BleConnectionState.Disconnected
    }

    override fun onConnectFail() {
        // Demo SDK behavior: immediately retry connection (see demo MainActivity line 184)
        connectionRetries++
        if (connectionRetries <= MAX_RETRIES) {
            Log.w(TAG, "Connection failed, retrying immediately ($connectionRetries/$MAX_RETRIES)")
            RingBleUtils.getRingBleManager().onConnect()
        } else {
            Log.e(TAG, "Connection failed after $MAX_RETRIES retries")
            connectionRetries = 0
            _connectionState.value = BleConnectionState.Error("Connection failed after $MAX_RETRIES attempts")
        }
    }

    override fun onBatteryListener(isCharge: Boolean, electricity: Int?) {
        Log.d(TAG, "Battery level: $electricity%, charging: $isCharge")
        
        // Safety trigger: If we receive battery data, the ring is definitely connected and authenticated.
        // This helps if the SDK missed the onConnectSuccess callback.
        if (_connectionState.value !is BleConnectionState.Connected) {
            Log.i(TAG, "✓ Data flow detected (battery). Transitioning to Connected state.")
            val ring = connectedRing?.copy(isConnected = true) ?: Ring(macAddress = "", name = "Ring", isConnected = true)
            _connectionState.value = BleConnectionState.Connected(ring)
        }
        
        // Fetch firmware version (seen in demo app onBatteryListener)
        val firmwareParams = RingBleUtils.getRingBleManager().getFirmwareParameters()
        val version = firmwareParams?.version ?: ""
        
        _ringData.value = _ringData.value.copy(
            battery = electricity,
            isCharging = isCharge,
            firmwareInfo = _ringData.value.firmwareInfo.copy(
                version = version,
                lastUpdate = System.currentTimeMillis()
            ),
            lastUpdate = System.currentTimeMillis()
        )
    }

    override fun onHealthAllBeanListener(tag: String, isRingData: Boolean, reqTime: Long?, list: List<JMHealthAllBean>) {
        if (list.isEmpty()) return
        
        // Safety trigger: If we receive health data, the ring is definitely connected and authenticated.
        if (_connectionState.value !is BleConnectionState.Connected) {
            Log.i(TAG, "✓ Data flow detected (health). Transitioning to Connected state.")
            val ring = connectedRing?.copy(isConnected = true) ?: Ring(macAddress = "", name = "Ring", isConnected = true)
            _connectionState.value = BleConnectionState.Connected(ring)
        }

        val bean = list.last()
        Log.d(TAG, "Health data update: HR=${bean.dailyHeartRate}, Steps=${bean.stepDiff}")
        
        _ringData.value = _ringData.value.copy(
            heartRate = bean.dailyHeartRate?.toInt() ?: _ringData.value.heartRate,
            spO2 = bean.spo2?.toFloat() ?: _ringData.value.spO2,
            // BP fields removed as they do not exist in JMHealthAllBean
            steps = bean.stepDiff?.toInt() ?: _ringData.value.steps,
            calories = bean.caloriesDiff?.toInt() ?: _ringData.value.calories,
            distance = bean.distanceDiff?.toInt() ?: _ringData.value.distance,
            lastUpdate = System.currentTimeMillis()
        )
    }

    override fun onStressBeanListener(tag: String, isRingData: Boolean, reqTime: Long?, list: List<JMStressBean>) {
        if (list.isEmpty()) return
        val bean = list.last()
        Log.d(TAG, "Stress data update: ${bean.pressureIndex}")
        
        _ringData.value = _ringData.value.copy(
            stress = bean.pressureIndex?.toInt() ?: 0,
            lastUpdate = System.currentTimeMillis()
        )
    }

    override fun onSleepAllBeanListener(tag: String, isRingData: Boolean, reqTime: Long?, jmSleepBean: JMSleepBean?) {
        jmSleepBean?.let { bean ->
            Log.d(TAG, "Sleep data update received")
            
            // Logic: Loop through mergeSleepDetails(). 
            // Count entries where sleepMode == 1 (light) or 2 (deep). 
            // Each entry = 1 minute.
            val details = bean.mergeSleepDetails() ?: emptyList()
            var lightMinutes = 0
            var deepMinutes = 0
            
            for (detail in details) {
                when (detail.sleepMode) {
                    1 -> lightMinutes++
                    2 -> deepMinutes++
                }
            }
            
            val totalMinutes = lightMinutes + deepMinutes
            val sleepHours = totalMinutes / 60.0
            
            Log.i(TAG, "Calculated sleep: $totalMinutes mins ($sleepHours hours)")
            
            // Automatic background sync to database
            scope.launch {
                try {
                    val dateStr = java.time.LocalDate.now().toString()
                    val container = com.dkgs.innerpulse.core.di.AppContainer.getInstance(context)
                    container.sleepRepository.logSleep(dateStr, sleepHours)
                    Log.i(TAG, "✓ Sleep data automatically saved to database for $dateStr")

                    _ringData.value = _ringData.value.copy(
                        sleepData = _ringData.value.sleepData.copy(
                            totalMinutes = totalMinutes,
                            lightMinutes = lightMinutes,
                            deepMinutes = deepMinutes
                        ),
                        lastUpdate = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving sleep data", e)
                }
            }
        }
    }

    override fun onRingAllCache() {
        Log.i(TAG, "Cache sync triggered by ring")
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val reqTime = calendar.timeInMillis
        val manager = RingBleUtils.getRingBleManager()
        
        Log.i(TAG, "Syncing daily metrics for: $reqTime")
        manager.getActivityHealthData(reqTime)
        manager.getActivitySleepData(reqTime)
        manager.getActivityStressData(reqTime)
    }

    override fun onMeasureResult(type: Int, isSuccess: Boolean, healthBean: JMHealthAllBean?, stressBean: JMStressBean?) {
        Log.i(TAG, "Measurement result: type=$type, success=$isSuccess")
        
        // Clear measuring flag regardless of success
        measurementTimeoutJobs[type]?.cancel()
        _ringData.value = _ringData.value.let { data ->
            when (type) {
                1 -> data.copy(heartRateMeasuring = false)
                2 -> data.copy(spO2Measuring = false)
                7 -> data.copy(stressMeasuring = false)
                else -> data
            }
        }
        
        if (!isSuccess) return
        
        _ringData.value = _ringData.value.copy(
            heartRate = healthBean?.dailyHeartRate?.toInt() ?: _ringData.value.heartRate,
            spO2 = healthBean?.spo2?.toFloat() ?: _ringData.value.spO2,
            stress = stressBean?.pressureIndex?.toInt() ?: _ringData.value.stress,
            lastUpdate = System.currentTimeMillis()
        )
        
        // If HR/SpO2/Stress measurement was triggered, might want to re-calculate score
        requestSleepScore()
    }

    // ═══════════════════════════════════
    // DataStore Helpers
    // ═══════════════════════════════════

    private suspend fun saveRingType(ringType: Int) {
        context.ringDataStore.edit { preferences ->
            preferences[RING_TYPE_KEY] = ringType
        }
    }

    fun getRingTypeFlow(): Flow<Int> = context.ringDataStore.data.map { preferences ->
        preferences[RING_TYPE_KEY] ?: 1 // Default to 研强
    }
}
