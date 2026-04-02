package com

import com.database.DataBaseFactory
import com.database.DataBaseSource
import com.database.FirebaseInitializer
import com.models.JwtConfig
import com.services.AiChatService
import com.services.ImgBBService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>){
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configReqVal()
    val jwtSection = environment.config.config("jwt")
    FirebaseInitializer.init()
    webSockets()
    val jwtConfig = JwtConfig(
        issuer = jwtSection.property("issuer").getString(),
        audience = jwtSection.property("audience").getString(),
        realm = jwtSection.property("realm").getString(),
        secret = jwtSection.property("secret").getString()
    )
    configureAuth(jwtConfig)
    val database= DataBaseFactory()
    database.init()
    val dotenv = dotenv()
    val openAiKey = dotenv["OPENAI_API_KEY"]
        ?: throw IllegalStateException("OPENAI_API_KEY missing")

    val imageBBApiKey = dotenv["ImgBBAPi_KEy"]
        ?: throw IllegalStateException("IMAGE_BB_API_KEY missing")
    val imgService= ImgBBService(imageBBApiKey)
    val dataBaseSource= DataBaseSource(database.database)

    val aiService= AiChatService(openAiKey)

    launch {
        while (true) {
            try {
                dataBaseSource.deleteExpiredStatuses()
            } catch (e: Exception) {
                println("Error cleaning statuses: ${e.message}")
            }
            delay(60 * 60 * 1000)
        }
    }



    configureRouting(dataBaseSource,imgService,jwtConfig,aiService)

}
