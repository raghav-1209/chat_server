package com.services

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class AiChatService(private val openAiKey: String) {

    private val httpclient = Client.httpclient

    private val conversations = mutableMapOf<String, MutableList<OpenAIMessage>>()

    private fun createSystemPrompt(): MutableList<OpenAIMessage> {
        return mutableListOf(
            OpenAIMessage(
                role = "system",
                content = """
You are a conversational AI assistant.

Your purpose is to help users by answering questions, explaining topics, solving problems, and having natural conversations.

Rules:
- Respond clearly and naturally as if speaking to a person.
- Keep answers concise unless the user asks for details.
- Ask for clarification if needed.
- If unsure, say you don’t know.
"""
            )
        )
    }

    //  Main function
    suspend fun askOpenAI(userId: String, question: String): Result<String> {
        return try {

            // 1 Get or create conversation for user
            val conversation = conversations.getOrPut(userId) {
                createSystemPrompt()
            }

            // 2 Add user message
            conversation.add(OpenAIMessage("user", question))

            // 3️⃣ Prepare request
            val requestBody = OpenAIRequest(
                model = "gpt-4o-mini",
                messages = conversation
            )

            // 4 Call OpenAI
            val response: OpenAIResponse =
                httpclient.post("https://api.openai.com/v1/chat/completions") {
                    header("Authorization", "Bearer $openAiKey")
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()

            val answer = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response"))

            // Add assistant reply
            conversation.add(OpenAIMessage("assistant", answer))

            // Limit memory (keep last ~20 messages)
            while (conversation.size > 20) {
                conversation.removeAt(1) // keep system prompt at index 0
            }

            Result.success(answer)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
@Serializable
data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>
)

@Serializable
data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int,
    val message: OpenAIMessage,
    val finish_reason: String? = null
)