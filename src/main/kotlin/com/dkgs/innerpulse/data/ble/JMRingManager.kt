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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        currentUserId = userId
        connectedRing = Ring(macAddress = macAddress, name = "JMRing", isConnected = false)
        Log.i(TAG, "Connecting to ring: $macAddress, type: $ringType")
        
        scope.launch {
            saveRingType(ringType)
        }

        RingBleUtils.setRingData(userId, macAddress, "", ringType)
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
        RingBleUtils.getRingBleManager().startStopMeasurement(type)
    }

    fun stopMeasurement(type: Int) {
        Log.i(TAG, "Stopping measurement type: $type")
        RingBleUtils.getRingBleManager().startStopMeasurement(type)
    }

    fun fetchCachedData() {
        Log.i(TAG, "Fetching cached data from ring")
        RingBleUtils.getRingBleManager().handlerCacheRing(TAG)
    }

    // ═══════════════════════════════════
    // SDK Callbacks
    // ═══════════════════════════════════

    override fun onConnecting() {
        _connectionState.value = BleConnectionState.Connecting
    }

    override fun onConnectSuccess() {
        Log.i(TAG, "Successfully connected to ring")
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
        Log.e(TAG, "Connection failed")
        _connectionState.value = BleConnectionState.Error("Connection failed")
    }

    override fun onBatteryListener(isCharge: Boolean, electricity: Int?) {
        Log.d(TAG, "Battery level: $electricity%, charging: $isCharge")
        _ringData.value = _ringData.value.copy(
            battery = electricity,
            isCharging = isCharge,
            lastUpdate = System.currentTimeMillis()
        )
    }

    override fun onHealthAllBeanListener(tag: String, isRingData: Boolean, reqTime: Long?, list: List<JMHealthAllBean>) {
        if (list.isEmpty()) return
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
        val currentTime = System.currentTimeMillis()
        val manager = RingBleUtils.getRingBleManager()
        manager.getActivityHealthData(currentTime)
        manager.getActivitySleepData(currentTime)
        manager.getActivityStressData(currentTime)
    }

    override fun onMeasureResult(type: Int, isSuccess: Boolean, healthBean: JMHealthAllBean?, stressBean: JMStressBean?) {
        Log.i(TAG, "Measurement result: type=$type, success=$isSuccess")
        if (!isSuccess) return
        
        _ringData.value = _ringData.value.copy(
            heartRate = healthBean?.dailyHeartRate?.toInt() ?: _ringData.value.heartRate,
            spO2 = healthBean?.spo2?.toFloat() ?: _ringData.value.spO2,
            // BP fields removed as they do not exist in JMHealthAllBean
            stress = stressBean?.pressureIndex?.toInt() ?: _ringData.value.stress,
            lastUpdate = System.currentTimeMillis()
        )
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
