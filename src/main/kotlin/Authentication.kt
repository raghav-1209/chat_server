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

fun Application.configureAuth(config: JwtConfig) {

    install(Authentication) {

        jwt("jwt_auth") {
            realm = config.realm
            verifier(
                JWT.require(Algorithm.HMAC256(config.secret))
                    .withIssuer(config.issuer)
                    .withAudience(config.audience)
                    .build()
            )

            validate { credential ->

                val uid = credential.payload.getClaim("user_uid").asString()

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
