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
    suspend operator fun invoke(macAddress: String, deviceName: String? = null, ringType: Int = 2): Result<Ring> {
        val formattedMac = formatMacAddress(macAddress)
        
        // Validate MAC address format
        if (!isValidMacAddress(formattedMac)) {
            return Result.error("Invalid MAC address format. Please provide 12 hex characters.")
        }
        
        return repository.connect(formattedMac, deviceName, ringType)
    }

    
    /**
     * Connect to a Ring object
     */
    suspend fun connect(ring: Ring, ringType: Int): Result<Ring> {
        return invoke(ring.macAddress, ring.name, ringType)
    }
    
    /**
     * Check if MAC address is a valid 12-character hex string (after formatting)
     */
    private fun isValidMacAddress(mac: String): Boolean {
        // Allow both 12-char hex and colon-separated formats, case-insensitive
        val regex = "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$|^[0-9A-Fa-f]{12}$".toRegex()
        return mac.matches(regex)
    }

    /**
     * Format the MAC address to uppercase as required by Android's BluetoothAdapter
     * (The legacy SDK used lowercase, but Crrepa requires standard uppercase)
     */
    private fun formatMacAddress(mac: String): String {
        return mac.trim().uppercase()
    }
}
