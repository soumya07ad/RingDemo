package com.dkgs.innerpulse.domain.usecase

import com.dkgs.innerpulse.core.util.Result
import com.dkgs.innerpulse.domain.model.Ring
import com.dkgs.innerpulse.domain.repository.IRingRepository

/**
 * Use case for connecting to a ring device
 * Encapsulates the business logic for device connection
 */
class ConnectRingUseCase(
    private val repository: IRingRepository
) {
    /**
     * Connect to a ring device
     * @param macAddress BLE MAC address
     * @param deviceName Optional device name for display
     * @return Result containing connected ring or error
     */
    suspend operator fun invoke(macAddress: String, deviceName: String? = null, ringType: Int = 1): Result<Ring> {
        // Validate MAC address format
        if (!isValidMacAddress(macAddress)) {
            return Result.error("Invalid MAC address format")
        }
        
        return repository.connect(macAddress, deviceName, ringType)
    }
    
    /**
     * Connect to a Ring object
     */
    suspend fun connect(ring: Ring, ringType: Int): Result<Ring> {
        return invoke(ring.macAddress, ring.name, ringType)
    }
    
    /**
     * Check if MAC address is potentially valid
     */
    private fun isValidMacAddress(mac: String): Boolean {
        // More lenient check since SDK's formatMacAddress will handle standardization.
        // Just ensures it's not empty and contains enough hex characters.
        val hexOnly = mac.filter { it.isLetterOrDigit() }
        return hexOnly.length >= 10 // Most MACs are 12 hex chars, but let's be flexible
    }
}
