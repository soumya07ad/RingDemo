package com.dkgs.innerpulse.data.repository

import com.dkgs.innerpulse.data.local.dao.CoachDao
import com.dkgs.innerpulse.data.local.entity.CoachMessageEntity
import com.dkgs.innerpulse.domain.repository.ICoachRepository
import kotlinx.coroutines.flow.Flow

class CoachRepositoryImpl(
    private val coachDao: CoachDao
) : ICoachRepository {

    override fun getAllSessions(): Flow<List<CoachMessageEntity>> {
        return coachDao.getAllSessions()
    }

    override fun getMessagesBySession(sessionId: String): Flow<List<CoachMessageEntity>> {
        return coachDao.getMessagesBySession(sessionId)
    }

    override suspend fun saveMessage(text: String, isUser: Boolean, sessionId: String) {
        coachDao.insertMessage(
            CoachMessageEntity(
                text = text,
                isUser = isUser,
                sessionId = sessionId
            )
        )
    }

    override suspend fun clearHistory() {
        coachDao.deleteAllMessages()
    }

    override suspend fun deleteSession(sessionId: String) {
        coachDao.deleteSession(sessionId)
    }

    override suspend fun getAiResponse(message: String): String {
        return try {
            val request = com.dkgs.innerpulse.network.models.GeminiRequest(
                contents = listOf(
                    com.dkgs.innerpulse.network.models.GeminiContent(
                        role = "user",
                        parts = listOf(
                            com.dkgs.innerpulse.network.models.GeminiPart(
                                text = "You are a helpful Wellness Coach. Give concise and professional advice on fitness, nutrition, and mental health.\n\nUser: $message"
                            )
                        )
                    )
                )
            )

            val response = com.dkgs.innerpulse.network.client.GeminiClient.api.generateContent(
                apiKey = com.dkgs.innerpulse.network.client.GeminiClient.getApiKey(),
                request = request
            )

            if (response.isSuccessful) {
                response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't process that. Please try again."
            } else {
                "Error: ${response.code()} - ${response.message()}"
            }
        } catch (e: Exception) {
            "An error occurred: ${e.localizedMessage}"
        }
    }
}
