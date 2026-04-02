package com.services


import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.request.forms.*

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class ImgBBService(private val apiKey: String) {
    val httpclient=Client.httpclient

    suspend fun sendImage(bytes: ByteArray?): Result<ImgBBResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes == null) {
                    return@withContext Result.failure(kotlinx.io.IOException("Empty bytes"))
                }
                val base64Image = Base64.encode(bytes)
                val response: HttpResponse =
                    httpclient.submitFormWithBinaryData(
                        url = "https://api.imgbb.com/1/upload",
                        formData = formData {
                            append("key", apiKey)
                            append("image", base64Image)
                        }
                    )

                Result.success(response.body())
            } catch (e: Exception) {
                Result.failure(e)

            }
        }


    }
}





@Serializable
data class ImgBBResponse(
    val data: ImgData,
    val success: Boolean,
    val status: Int
)

@Serializable
data class ImgData(
    val url: String
)



