package com.dkgs.innerpulse.domain.repository

import com.dkgs.innerpulse.domain.model.ActivityStreakData
import com.dkgs.innerpulse.domain.model.MilestoneData

interface IStreakRepository {
    suspend fun markActivityCompleted(activityType: String, date: String)
    suspend fun getAllActivityStreaks(): List<ActivityStreakData>
    suspend fun getLongestCurrentStreak(): Pair<String, Int>
    fun getMilestoneProgress(currentStreak: Int): List<MilestoneData>
}
