package com.dkgs.innerpulse.domain.repository

import kotlinx.coroutines.flow.StateFlow
import com.dkgs.innerpulse.domain.model.PairedRing

interface ISettingsRepository {
    val notificationsEnabled: StateFlow<Boolean>
    val metricUnitsEnabled: StateFlow<Boolean>
    val bedtimeReminderEnabled: StateFlow<Boolean>
    val dataSyncEnabled: StateFlow<Boolean>
    val userName: StateFlow<String>
    val userDob: StateFlow<String>
    val userGender: StateFlow<String>
    val ringType: StateFlow<Int>
    val ringMacAddress: StateFlow<String>

    fun setNotificationsEnabled(enabled: Boolean)
    fun setMetricUnitsEnabled(enabled: Boolean)
    fun setBedtimeReminderEnabled(enabled: Boolean)
    fun setDataSyncEnabled(enabled: Boolean)
    fun setRingType(type: Int)
    fun setRingMacAddress(mac: String)
    fun saveProfile(name: String, dob: String, gender: String)

    val pairedRings: StateFlow<List<PairedRing>>
    fun addPairedRing(ring: PairedRing)
    fun removePairedRing(macAddress: String)
}
