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
        val regex = "^[0-9a-f]{12}$".toRegex()
        return mac.matches(regex)
    }

    /**
     * Parse any user input into the raw format expected by the SDK's internal formatter.
     * The Demo app successfully connects when passing lowercase, colon-less strings
     * to RingBleUtils.formatMacAddress().
     */
    private fun formatMacAddress(mac: String): String {
        return mac.filter { it.isLetterOrDigit() }.lowercase()
    }
}
