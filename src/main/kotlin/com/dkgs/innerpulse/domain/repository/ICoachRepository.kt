package com.dkgs.innerpulse.domain.repository

import com.dkgs.innerpulse.data.local.entity.CoachMessageEntity
import kotlinx.coroutines.flow.Flow

interface ICoachRepository {
    fun getAllSessions(): Flow<List<CoachMessageEntity>>
    fun getMessagesBySession(sessionId: String): Flow<List<CoachMessageEntity>>
    suspend fun saveMessage(text: String, isUser: Boolean, sessionId: String)
    suspend fun clearHistory()
    suspend fun deleteSession(sessionId: String)
    suspend fun getAiResponse(message: String): String
}
