package com.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Client {
     val httpclient = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true

                    isLenient = true
                    prettyPrint = false
                }
            )
        }

        install(HttpTimeout) {

            requestTimeoutMillis = 60_000     // total request time
            connectTimeoutMillis = 30_000     // connection time
            socketTimeoutMillis = 60_000
        }



    }
}