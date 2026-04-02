package com.database

import com.UserService
import io.ktor.http.Url
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DataBaseFactory{
    lateinit var database: Database
    fun init() {
        database = Database.connect(
            url = "jdbc:postgresql://localhost:5432/myDb",
            driver = "org.postgresql.Driver",
            user = "user",
            password = "shh"

        )
        transaction (database){
            SchemaUtils.create(
                Tables.users,
                Tables.fcmTokens,
                Tables.jwt_Token,
                Tables.bio,
                Tables.image,
                Tables.Follows,
                Tables.Messages,
                Tables.Status
            )

        }

    }
}