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
     * Check if MAC address matches standard format XX:XX:XX:XX:XX:XX
     */
    private fun isValidMacAddress(mac: String): Boolean {
        val regex = "^([0-9A-F]{2}[:]){5}([0-9A-F]{2})$".toRegex()
        return mac.matches(regex)
    }

    /**
     * Parse any user input into standard uppercase MAC format
     */
    private fun formatMacAddress(mac: String): String {
        val uppercaseMac = mac.uppercase()
        val hexOnly = uppercaseMac.filter { it.isLetterOrDigit() }
        
        if (hexOnly.length == 12) {
            val formatted = java.lang.StringBuilder()
            for (i in 0 until 12 step 2) {
                formatted.append(hexOnly.substring(i, i + 2))
                if (i < 10) formatted.append(":")
            }
            return formatted.toString()
        }
        return uppercaseMac
    }
}
