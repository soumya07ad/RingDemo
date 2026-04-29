package com.dkgs.innerpulse.data.repository

import android.content.Context
import android.util.Log
import com.dkgs.innerpulse.ble.BleConnectionState
import com.dkgs.innerpulse.ble.RingData
import com.dkgs.innerpulse.data.ble.JMRingManager
import com.dkgs.innerpulse.ble.MeasurementTimer
import com.dkgs.innerpulse.core.util.Result
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.RingHealthData
import com.dkgs.innerpulse.domain.model.ScanStatus
import com.dkgs.innerpulse.domain.repository.IRingRepository
import com.gps.track.jmring.bean.JMScanBean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Implementation of IRingRepository
 * Uses JMRingManager (Official JMRing SDK)
 * 
 * Replaces the old NativeGattManager implementation.
 */
class RingRepositoryImpl(
    private val context: Context
) : IRingRepository {
    
    companion object {
        private const val TAG = "RingRepositoryImpl"
        
        @Volatile
        private var INSTANCE: RingRepositoryImpl? = null
        
        fun getInstance(context: Context): RingRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RingRepositoryImpl(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Use JMRingManager (Official SDK)
    private val jmRingManager: JMRingManager by lazy { 
        JMRingManager.getInstance(context)
    }
    
    // Domain state flows
    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    override val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()
    
    private val _ringData = MutableStateFlow(RingHealthData())
    override val ringData: StateFlow<RingHealthData> = _ringData.asStateFlow()
    
    // Track connected ring
    override val measurementTimer: StateFlow<MeasurementTimer> = MutableStateFlow(MeasurementTimer()).asStateFlow() // SDK handles internal timing
    private var connectedRing: Ring? = null
    
    init {
        observeManagerStates()
    }
    
    /**
     * Observe JMRingManager states and map to domain states
     */
    private fun observeManagerStates() {
        // Observe connection state
        scope.launch {
            jmRingManager.connectionState.collect { state ->
                _connectionStatus.value = mapConnectionState(state)
            }
        }
        
        // Observe ring data
        scope.launch {
            jmRingManager.ringData.collect { data ->
                _ringData.value = mapRingData(data)
            }
        }
        
        // Observe scan results
        scope.launch {
            jmRingManager.scanResults.collect { results ->
                if (results.isNotEmpty()) {
                    val rings = results.map { 
                        Ring(
                            macAddress = it.mac ?: "", 
                            name = it.name ?: "JMRing",
                            rssi = it.rssi ?: -100,
                            isConnected = false
                        )
                    }
                    _scanStatus.value = ScanStatus.DevicesFound(rings)
                }
            }
        }
    }
    
    /**
     * Map BLE Connection State to domain ConnectionStatus
     */
    private fun mapConnectionState(state: BleConnectionState): ConnectionStatus {
        Log.d(TAG, "Mapping BLE state to domain status: $state")
        return when (state) {
            is BleConnectionState.Disconnected -> {
                connectedRing = null
                ConnectionStatus.Disconnected
            }
            is BleConnectionState.Connecting -> ConnectionStatus.Connecting
            is BleConnectionState.Connected -> {
                Log.i(TAG, "✓ Mapping CONNECTED state for ring: ${state.ring.macAddress}")
                connectedRing = state.ring.copy(isConnected = true)
                ConnectionStatus.Connected(connectedRing!!)
            }
            is BleConnectionState.Error -> {
                connectedRing = null
                ConnectionStatus.Disconnected
            }
        }
    }
    
    /**
     * Map BLE RingData to domain RingHealthData
     */
    private fun mapRingData(data: RingData): RingHealthData {
        return RingHealthData(
            battery = data.battery,
            isCharging = data.isCharging,
            heartRate = data.heartRate,
            heartRateMeasuring = data.heartRateMeasuring,
            spO2 = data.spO2,
            spO2Measuring = data.spO2Measuring,
            stress = data.stress,
            stressMeasuring = data.stressMeasuring,
            steps = data.steps,
            distance = data.distance,
            calories = data.calories,
            sleepData = data.sleepData,
            firmwareInfo = data.firmwareInfo,
            healthScore = data.healthScore,
            lastUpdate = data.lastUpdate
        )
    }
    
    override fun initialize() {
        // SDK initialized in FitnessApplication
    }
    
    override suspend fun startScan(durationSeconds: Int): Result<List<Ring>> {
        return try {
            _scanStatus.value = ScanStatus.Scanning
            jmRingManager.startScan()
            Result.success(emptyList()) // Results come via flow
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Scan failed")
            Result.error("Scan failed: ${e.message}", e)
        }
    }
    
    override fun stopScan() {
        jmRingManager.stopScan()
        _scanStatus.value = ScanStatus.Idle
    }
    
    override suspend fun connect(macAddress: String, deviceName: String?, ringType: Int): Result<Ring> {
        return try {
            val ring = Ring(
                macAddress = macAddress,
                name = deviceName ?: "JMRing",
                isConnected = false
            )
            
            connectedRing = ring
            _connectionStatus.value = ConnectionStatus.Connecting
            
            Log.i(TAG, "🔗 JMRing SDK connect: $macAddress, type: $ringType")
            
            // Using ringType from parameter
            jmRingManager.connectRing("12345", macAddress, ringType)
            
            Result.success(ring)
            
        } catch (e: Exception) {
            connectedRing = null
            _connectionStatus.value = ConnectionStatus.Disconnected
            Result.error("Connection failed: ${e.message}", e)
        }
    }
    
    override suspend fun disconnect(): Result<Unit> {
        jmRingManager.disconnect()
        connectedRing = null
        _connectionStatus.value = ConnectionStatus.Disconnected
        return Result.success(Unit)
    }
    
    override suspend fun getBattery(): Result<Int> {
        val battery = _ringData.value.battery
        return if (battery != null && battery > 0) {
            Result.success(battery)
        } else {
            Result.error("Battery data not available")
        }
    }
    
    override fun isConnected(): Boolean {
        return jmRingManager.connectionState.value is BleConnectionState.Connected
    }
    
    override fun getConnectedRing(): Ring? = connectedRing
    
    // ═══════════════════════════════════
    // Measurements (delegate to manager)
    // ═══════════════════════════════════
    
    override fun startHeartRateMeasurement() {
        Log.i(TAG, "Starting HR measurement (SDK)")
        jmRingManager.startMeasurement(1)
    }
    
    override fun stopHeartRateMeasurement() {
        jmRingManager.stopMeasurement(1)
    }
    
    
    override fun startSpO2Measurement() {
        Log.i(TAG, "Starting SpO2 measurement (SDK)")
        jmRingManager.startMeasurement(2)
    }
    
    override fun stopSpO2Measurement() {
        jmRingManager.stopMeasurement(2)
    }
    
    override fun startStressMeasurement() {
        Log.i(TAG, "Starting stress measurement (SDK)")
        jmRingManager.startMeasurement(7)
    }
    
    override fun stopStressMeasurement() {
        jmRingManager.stopMeasurement(7)
    }
    
    override fun requestSleepHistory() {
        Log.i(TAG, "Requesting sleep history (SDK)")
        jmRingManager.fetchCachedData()
    }
    
    fun refreshDeviceInfo() {
        jmRingManager.fetchCachedData()
    }
    
    fun refreshStepsData() {
        jmRingManager.fetchCachedData()
    }
    
    fun refreshBloodPressure() {
        // Not supported directly in SDK measurement types (1, 2, 6, 7)
    }
    
    fun refreshStress() {
        jmRingManager.fetchCachedData()
    }
    
    fun refreshAllData() {
        jmRingManager.fetchCachedData()
        jmRingManager.requestSleepScore()
    }
    
    override fun stopMeasurement() {
        // Generic stop if needed
    }
}
