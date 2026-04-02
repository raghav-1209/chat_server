package com

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.util.reflect.instanceOf
import kotlin.time.Duration.Companion.seconds

fun Application.webSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout=15.seconds
    }
}