package com.database

import com.UserService
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import java.util.UUID

object Tables {
    object users : Table("users") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 255)
        val email = varchar("email", 255)
        val uid = varchar("uid", 255).uniqueIndex()
        override val primaryKey = PrimaryKey(id)
    }
    object fcmTokens : Table("fcm_tokens") {
        val id = integer("id").autoIncrement()
        val user_uid=reference("user_id", Tables.users.uid).uniqueIndex()
        val token=text("fcm_token")
    }
    object jwt_Token :Table("jwt_token") {
        val id = integer("id").autoIncrement()
        val user_uid=reference("user_id", Tables.users.uid).uniqueIndex()
        val token = text("refresh_token")
    }
    object bio: Table("bio") {
        val id = integer("id").autoIncrement()
        val user_uid=reference("user_id", Tables.users.uid).uniqueIndex()
        val bio_text=text("bio_text")

    }
    object image:Table("image"){
        val id=integer("id").autoIncrement()
        val user_uid=reference("user_id", Tables.users.uid).uniqueIndex()
        val image = text("image")
    }

    object Follows : Table("follows") {

        val followerId = integer("follower_id")
            .references(Tables.users.id, onDelete = ReferenceOption.CASCADE)

        val followingId = integer("following_id")
            .references(Tables.users.id, onDelete = ReferenceOption.CASCADE)

        val createdAt = long("createdAt")
            .clientDefault { System.currentTimeMillis() }
        val status=text("status")

        override val primaryKey = PrimaryKey(followerId, followingId)
    }
    object Conversations : Table("conversations") {
        val id = integer("id").autoIncrement()
        val chatId = varchar("chat_id", 100).uniqueIndex()
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(id)
    }
    object Messages : Table("messages") {
        val id = integer("id").autoIncrement()

        val conversationId = integer("conversation_id")
            .references(Conversations.id, onDelete = ReferenceOption.CASCADE)

        val messageId = varchar("message_id", 100).uniqueIndex()

        val senderUid = varchar("sender_uid", 100)
        val receiverUid = varchar("receiver_uid", 100)
        val message = text("message")
        val time = long("time")
       val  deletedForSender = bool("deleted_for_sender").default(false)
       val deletedForReceiver = bool("deleted_for_receiver").default(false)

        override val primaryKey = PrimaryKey(id)
    }
    object  Status:Table("Status"){
        val id=integer("id").autoIncrement()
        val user_id=reference("user_id",Tables.users.id)
        val statusImage=text("url")
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(id)


    }

}