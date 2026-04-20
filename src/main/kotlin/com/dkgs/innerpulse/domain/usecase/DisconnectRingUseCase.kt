package com.dkgs.innerpulse.domain.usecase

import com.dkgs.innerpulse.core.util.Result
import com.dkgs.innerpulse.domain.repository.IRingRepository

/**
 * Use case for disconnecting from a ring device
 */
class DisconnectRingUseCase(
    private val repository: IRingRepository
) {
    /**
     * Disconnect from currently connected ring
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(): Result<Unit> {
        return repository.disconnect()
    }
}
