package com.dkgs.innerpulse.domain.repository

import com.dkgs.innerpulse.core.util.Result
import com.dkgs.innerpulse.domain.model.ConnectionStatus
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.model.RingHealthData
import com.dkgs.innerpulse.domain.model.ScanStatus
import com.dkgs.innerpulse.ble.MeasurementTimer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Ring operations
 * Defines the contract for data layer implementations
 */
interface IRingRepository {
    
    /**
     * Observe connection status changes
     */
    val connectionStatus: StateFlow<ConnectionStatus>
    
    /**
     * Observe scan status changes
     */
    val scanStatus: StateFlow<ScanStatus>
    
    /**
     * Observe ring health data updates
     */
    val ringData: StateFlow<RingHealthData>
    
    
    /**
     * Observe measurement timer for timed vital sign measurements
     */
    val measurementTimer: StateFlow<MeasurementTimer>
    /**
     * Initialize BLE components
     */
    fun initialize()
    
    /**
     * Start scanning for nearby ring devices
     * @param durationSeconds How long to scan
     */
    suspend fun startScan(durationSeconds: Int = 6): Result<List<Ring>>
    
    /**
     * Stop current scan
     */
    fun stopScan()
    
    /**
     * Connect to a ring device by MAC address
     * @param macAddress BLE MAC address
     * @param deviceName Optional device name
     * @param ringType Type of the ring (1 for 研强, 2 for 小七)
     */
    suspend fun connect(macAddress: String, deviceName: String? = null, ringType: Int = 1): Result<Ring>
    
    /**
     * Disconnect from currently connected ring
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Get current battery level
     */
    suspend fun getBattery(): Result<Int>
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean
    
    /**
     * Get connected ring info or null
     */
    fun getConnectedRing(): Ring?
    
    /**
     * Start heart rate measurement
     */
    fun startHeartRateMeasurement()
    
    /**
     * Stop heart rate measurement
     */
    fun stopHeartRateMeasurement()
    
    /**
     * Start SpO2 (blood oxygen) measurement
     */
    fun startSpO2Measurement()
    
    /**
     * Stop SpO2 measurement
     */
    fun stopSpO2Measurement()
    
    /**
     * Start stress measurement
     */
    fun startStressMeasurement()
    
    /**
     * Stop stress measurement
     */
    fun stopStressMeasurement()

    /**
     * Start HRV measurement
     */
    fun startHrvMeasurement()

    /**
     * Stop HRV measurement
     */
    fun stopHrvMeasurement()

    /**
     * Start Blood Pressure measurement
     */
    fun startBloodPressureMeasurement()

    /**
     * Stop Blood Pressure measurement
     */
    fun stopBloodPressureMeasurement()
    
    /**
     * Request sleep history from ring
     */
    fun requestSleepHistory()
    
    /**
     * Stop current measurement
     */
    fun stopMeasurement()
}
