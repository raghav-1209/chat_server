package com

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.constants.Times
import com.models.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import java.util.Date
import  org.example.com.raghav.jwt.JwtVerifier


fun Application.configureAuth() {

    install(Authentication) {

        jwt("jwt_auth") {
            verifier(JwtVerifier.verifier)

            validate { credential ->

                val uid = credential.payload.getClaim("userId").asString()

                if (!uid.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid Token")
            }
        }
    }
}

fun generateToken(userId: String, config: JwtConfig): String {
    return JWT.create()
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withClaim("user_uid", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 15* 60 * 1000)) // 2 min
        .sign(Algorithm.HMAC256(config.secret))
}
