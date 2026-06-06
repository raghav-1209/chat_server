package com.database

import com.database.Tables.Follows.status
import com.enums.FollowStatus
import com.models.BioData
import com.models.ChatData
import com.models.FcmData
import com.models.ImageModel
import com.models.SignInData
import com.models.StatusModel
import com.models.StatusWithUser
import com.models.TokenData
import com.models.UserData
import com.models.WholeUser
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.collections.map

class DataBaseSource(val db: Database) {

    fun getUserByUid(uid: String): UserData? {
        return transaction(db) {
            Tables.users.selectAll()
                .where { Tables.users.uid eq uid }
                .singleOrNull()?.toUser()

        }
    }


    fun ResultRow.toUser(): UserData {
        return UserData(
            name = this[Tables.users.name],
            uid = this[Tables.users.uid],
            email = this[Tables.users.email],
        )

    }

    fun saveFcm(data: FcmData) {
        transaction(db) {

            val userExists = Tables.users
                .selectAll()
                .where { Tables.users.uid eq data.uid }
                .singleOrNull()

            if (userExists == null) {
                println("User not found — cannot save FCM")
                return@transaction
            }

            val existing = Tables.fcmTokens
                .selectAll()
                .where { Tables.fcmTokens.user_uid eq data.uid }
                .singleOrNull()

            if (existing == null) {
                Tables.fcmTokens.insert {
                    it[token] = data.token
                    it[user_uid] = data.uid
                }
            } else {
                Tables.fcmTokens.update({ Tables.fcmTokens.user_uid eq data.uid }) {
                    it[token] = data.token
                }
            }
        }
    }

    fun saveJwtToken(token_data: TokenData) {
        transaction(db) {
            val info = Tables.jwt_Token.selectAll()
                .where { Tables.jwt_Token.user_uid eq token_data.uid }
                .singleOrNull()?.toJwtToken()
            if (info == null) {
                // Check if a token already exists for this user
                println("Saving token for UID=${token_data.uid} token=${token_data.token}")
                // Insert new token
                Tables.jwt_Token.insert {
                    it[Tables.jwt_Token.user_uid] = token_data.uid
                    it[Tables.jwt_Token.token] = token_data.token
                }
            } else {
                Tables.jwt_Token.update({ Tables.jwt_Token.user_uid eq token_data.uid }) {
                    it[token] = token_data.token
                }

            }

        }
    }


    fun getEmail(email: String): UserData? {
        return transaction(db) {
            Tables.users
                .selectAll()
                .where { Tables.users.email eq email }
                .singleOrNull()
                ?.toUser()
        }
    }

    fun getJwtToken(refreshToken: String): TokenData? {
        return transaction(db) {
            Tables.jwt_Token
                .selectAll()
                .where { Tables.jwt_Token.token eq refreshToken }
                .singleOrNull()
                ?.toJwtToken()
        }
    }

    fun ResultRow.toJwtToken(): TokenData {
        return TokenData(
            uid = this[Tables.jwt_Token.user_uid],
            token = this[Tables.jwt_Token.token],
        )

    }

    fun saveBio(bioData: BioData) {
        transaction(db) {
            val data = Tables.bio.selectAll().where {
                Tables.bio.user_uid eq bioData.uid
            }.singleOrNull()?.toBioData()
            if (data == null) {
                Tables.bio.insert {
                    it[Tables.bio.user_uid] = bioData.uid
                    it[Tables.bio.bio_text] = bioData.bio
                }
            } else {
                Tables.bio.update({ Tables.bio.user_uid eq bioData.uid }) {
                    it[Tables.bio.bio_text] = bioData.bio
                }
            }

        }

    }

    fun getBio(uid: String): BioData? {
        return transaction(db) {
            Tables.bio
                .selectAll()
                .where { Tables.bio.user_uid eq uid }
                .singleOrNull()
                ?.toBioData()
        }
    }

    fun ResultRow.toBioData(): BioData {
        return BioData(
            uid = this[Tables.bio.user_uid],
            bio = this[Tables.bio.bio_text],
        )

    }

    fun saveImage(url: String, uid: String) {
        transaction(db) {
            val data = Tables.image.selectAll().where {
                Tables.image.user_uid eq uid
            }.singleOrNull()?.toImgData()
            if (data == null) {
                Tables.image.insert {
                    it[Tables.image.user_uid] = uid
                    it[Tables.image.image] = url
                }
            } else {
                Tables.image.update({ Tables.image.user_uid eq uid }) {
                    it[Tables.image.image] = url
                }
            }
        }

    }

    fun ResultRow.toImgData(): ImageModel {
        return ImageModel(
            uid = this[Tables.image.user_uid],
            url = this[Tables.image.image]
        )
    }

    fun getImage(uid: String): ImageModel? {
        return transaction(db) {
            Tables.image.selectAll()
                .where {
                    Tables.image.user_uid eq uid
                }.singleOrNull()?.toImgData()

        }
    }

    fun getAllUsers(uid: String): List<WholeUser> {
        return transaction(db) {
            Tables.users
                .leftJoin(Tables.bio, { Tables.users.uid }, { Tables.bio.user_uid })
                .leftJoin(Tables.image, { Tables.users.uid }, { Tables.image.user_uid })
                .selectAll()
                .where {
                    Tables.users.uid neq uid
                }
                .map { row ->

                    WholeUser(
                        credentials = UserData(
                            uid = row[Tables.users.uid],
                            email = row[Tables.users.email],
                            name = row[Tables.users.name]
                        ),

                        bio = row[Tables.bio.bio_text],
                        image = row[Tables.image.image]
                    )
                }
        }
    }

    fun getFcmToken(uid: String): FcmData? {
        return transaction(db) {
            Tables.fcmTokens.selectAll()
                .where {
                    Tables.fcmTokens.user_uid eq uid
                }.singleOrNull()?.toFcmData()

        }
    }

    fun ResultRow.toFcmData(): FcmData {
        return FcmData(
            uid = this[Tables.fcmTokens.user_uid],
            token = this[Tables.fcmTokens.token],
        )
    }

    fun getInternalUserId(uid: String): Int? = transaction(db) {
        Tables.users
            .selectAll().where { Tables.users.uid eq uid }
            .map { it[Tables.users.id] }
            .singleOrNull()
    }


    fun followUser(followerId: Int, followingId: Int): String {

        return transaction(db) {

            val direct = Tables.Follows.selectAll().where {
                (Tables.Follows.followerId eq followerId) and
                        (Tables.Follows.followingId eq followingId)
            }.singleOrNull()

            val reverse = Tables.Follows.selectAll().where {
                (Tables.Follows.followerId eq followingId) and
                        (Tables.Follows.followingId eq followerId)
            }.singleOrNull()

            when {

                //  You already sent request
                direct?.get(Tables.Follows.status) == FollowStatus.PENDING.name ->
                    "REQUESTED"

                //  You already follow
                direct?.get(Tables.Follows.status) == FollowStatus.ACCEPTED.name ->
                    "FOLLOWING"

                //  Other person sent you request
                reverse?.get(Tables.Follows.status) == FollowStatus.PENDING.name ->
                    "REQUEST_RECEIVED"

                //  Fresh request
                direct == null && reverse == null -> {
                    Tables.Follows.insert {
                        it[Tables.Follows.followerId] = followerId
                        it[Tables.Follows.followingId] = followingId
                        it[status] = FollowStatus.PENDING.name
                    }
                    "REQUESTED"
                }

                else -> "NONE"
            }
        }
    }

    fun rejectFollow(senderId: Int, receiverId: Int): Boolean {
        return transaction(db) {

            Tables.Follows.deleteWhere {
                (Tables.Follows.followerId eq senderId) and
                        (Tables.Follows.followingId eq receiverId) and
                        (Tables.Follows.status eq FollowStatus.PENDING.name)
            } > 0
        }
    }

    fun acceptFollow(senderId: Int, receiverId: Int): Boolean {
        return transaction(db) {

            val updated = Tables.Follows.update({
                (Tables.Follows.followerId eq senderId) and
                        (Tables.Follows.followingId eq receiverId) and
                        (Tables.Follows.status eq FollowStatus.PENDING.name)
            }) {
                it[status] = FollowStatus.ACCEPTED.name
            }

            if (updated > 0) {
                Tables.Follows.insertIgnore {
                    it[followerId] = receiverId
                    it[followingId] = senderId
                    it[status] = FollowStatus.ACCEPTED.name
                }
            }

            updated > 0
        }
    }

    fun getFollowState(user1: Int, user2: Int): String = transaction(db) {

        val direct = Tables.Follows.selectAll().where {
            (Tables.Follows.followerId eq user1) and
                    (Tables.Follows.followingId eq user2)
        }.singleOrNull()

        val reverse = Tables.Follows.selectAll().where {
            (Tables.Follows.followerId eq user2) and
                    (Tables.Follows.followingId eq user1)
        }.singleOrNull()

        when {
            direct == null && reverse == null -> "NONE"
            direct?.get(status) == "PENDING" -> "REQUESTED"
            reverse?.get(status) == "PENDING" -> "REQUEST_RECEIVED"
            direct?.get(status) == "ACCEPTED" -> "FOLLOWING"
            reverse?.get(status) == "ACCEPTED" -> "FOLLOWED_BY"
            else -> "NONE"
        }

    }

    fun toggleFollow(user1: Int, user2: Int): String {
        return transaction(db) {

            val direct = Tables.Follows.selectAll().where {
                (Tables.Follows.followerId eq user1) and
                        (Tables.Follows.followingId eq user2)
            }.singleOrNull()

            val reverse = Tables.Follows.selectAll().where {
                (Tables.Follows.followerId eq user2) and
                        (Tables.Follows.followingId eq user1)
            }.singleOrNull()

            when {

                //  Case 1: They already sent you a request → ACCEPT IT
                reverse?.get(Tables.Follows.status) == FollowStatus.PENDING.name -> {

                    // Update their request to ACCEPTED
                    Tables.Follows.update({
                        (Tables.Follows.followerId eq user2) and
                                (Tables.Follows.followingId eq user1)
                    }) {
                        it[status] = FollowStatus.ACCEPTED.name
                    }

                    // Insert mutual follow if not exists
                    if (direct == null) {
                        Tables.Follows.insertIgnore {
                            it[followerId] = user1
                            it[followingId] = user2
                            it[status] = FollowStatus.ACCEPTED.name
                        }
                    }

                    "FOLLOWING"
                }

                //  Case 2: Already following → UNFOLLOW
                direct?.get(Tables.Follows.status) == FollowStatus.ACCEPTED.name -> {

                    Tables.Follows.deleteWhere {
                        (Tables.Follows.followerId eq user1) and
                                (Tables.Follows.followingId eq user2)
                    }

                    "NONE"
                }

                //  Case 3: You already sent request → CANCEL IT
                direct?.get(Tables.Follows.status) == FollowStatus.PENDING.name -> {

                    Tables.Follows.deleteWhere {
                        (Tables.Follows.followerId eq user1) and
                                (Tables.Follows.followingId eq user2)
                    }

                    "NONE"
                }

                //  Case 4: They follow you (ACCEPTED reverse) → Follow back
                reverse?.get(Tables.Follows.status) == FollowStatus.ACCEPTED.name -> {

                    Tables.Follows.insertIgnore {
                        it[followerId] = user1
                        it[followingId] = user2
                        it[status] = FollowStatus.ACCEPTED.name
                    }

                    "FOLLOWING"
                }

                //  Case 5: No relation → Send follow request
                else -> {

                    Tables.Follows.insertIgnore {
                        it[followerId] = user1
                        it[followingId] = user2
                        it[status] = FollowStatus.PENDING.name
                    }

                    "REQUESTED"
                }
            }
        }
    }

    fun getFollowingWithDetails(myId: Int): List<WholeUser> {
       return transaction(db) {

            val join = Tables.Follows
                .join(Tables.users, JoinType.INNER) {
                    Tables.Follows.followingId eq Tables.users.id
                }
                .join(Tables.bio, JoinType.LEFT) {
                    Tables.bio.user_uid eq Tables.users.uid
                }
                .join(Tables.image, JoinType.LEFT) {
                    Tables.image.user_uid eq Tables.users.uid
                }

             join.selectAll()
                .where {
                    (Tables.Follows.followerId eq myId) and
                            (Tables.Follows.status eq FollowStatus.ACCEPTED.name)
                }.map {

                    WholeUser(
                        credentials = UserData(
                            name = it[Tables.users.name],
                            email = it[Tables.users.email],
                            uid = it[Tables.users.uid]
                        ),
                        bio = it[Tables.bio.bio_text],
                        image = it[Tables.image.image]
                    )
                }
        }

    }
    fun getFollowingIds(myId: Int): List<Int> {
        return transaction(db) {
            Tables.Follows
                .selectAll()
                .where {
                    (Tables.Follows.followerId eq myId) and
                            (Tables.Follows.status eq FollowStatus.ACCEPTED.name)
                }
                .map { it[Tables.Follows.followingId] }
        }
    }

    fun getFollowersWithDetails(id:Int):List<WholeUser> {
       return  transaction(db) {
            val join = Tables.Follows
                .join(Tables.users, JoinType.INNER) {
                    Tables.Follows.followerId eq Tables.users.id
                }
                .join(Tables.bio, JoinType.LEFT) {
                    Tables.bio.user_uid eq Tables.users.uid
                }
                .join(Tables.image, JoinType.LEFT) {
                    Tables.image.user_uid eq Tables.users.uid
                }

             join.selectAll()
                .where {
                    (Tables.Follows.followingId eq id) and
                            (Tables.Follows.status eq FollowStatus.ACCEPTED.name)
                }.map {

                    WholeUser(
                        credentials = UserData(
                            name = it[Tables.users.name],
                            email = it[Tables.users.email],
                            uid = it[Tables.users.uid]
                        ),
                        bio = it[Tables.bio.bio_text],
                        image = it[Tables.image.image]
                    )
                }

       }
    }

    fun getOrCreateConversation(chatId: String): Int {
        return transaction {

            val existing = Tables.Conversations
                .selectAll().where { Tables.Conversations.chatId eq chatId }
                .singleOrNull()

            if (existing != null) {
                existing[Tables.Conversations.id]
            } else {
                Tables.Conversations.insert {
                    it[Tables.Conversations.chatId] = chatId
                    it[createdAt] = System.currentTimeMillis()
                } get Tables.Conversations.id
            }
        }
    }


    fun saveMessage(
        conversationId: Int,
        messageId: String,
        senderUid: String,
        receiverUid: String,
        message: String,
        time: Long
    ) {
        transaction(db) {
            Tables.Messages.insert {
                it[Tables.Messages.conversationId] = conversationId
                it[Tables.Messages.messageId] = messageId
                it[Tables.Messages.senderUid] = senderUid
                it[Tables.Messages.receiverUid] = receiverUid
                it[Tables.Messages.message] = message
                it[Tables.Messages.time] = time
            }
        }
    }
    fun getMessages(chatId: String, currentUid: String): List<ChatData> {
        return transaction {

            val conversation = Tables.Conversations
                .selectAll().where { Tables.Conversations.chatId eq chatId }
                .singleOrNull() ?: return@transaction emptyList()

            val conversationId = conversation[Tables.Conversations.id]

            Tables.Messages
                .selectAll()
                .where {
                    (Tables.Messages.conversationId eq conversationId) and
                            (
                                    ((Tables.Messages.senderUid eq currentUid) and (Tables.Messages.deletedForSender eq false)) or
                                            ((Tables.Messages.receiverUid eq currentUid) and (Tables.Messages.deletedForReceiver eq false))
                                    )
                }
                .orderBy(Tables.Messages.time to SortOrder.ASC)
                .map {
                    ChatData(
                        messageId = it[Tables.Messages.messageId],
                        chat_id = chatId,
                        senderUid = it[Tables.Messages.senderUid],
                        receiverUid = it[Tables.Messages.receiverUid],
                        message = it[Tables.Messages.message],
                        time = it[Tables.Messages.time]
                    )
                }
        }
    }


    fun uploadStatus(url: String, id: Int) {
        transaction(db) {
            Tables.Status.insert {
                it[user_id] = id
                it[statusImage] = url
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }
    fun ResultRow.toStatusModel(): StatusModel {
        return StatusModel(
            userId = this[Tables.Status.user_id],
            url = this[Tables.Status.statusImage],
            createdAt = this[Tables.Status.createdAt],
        )
    }
    fun getUserStatus(id: Int): List<StatusModel> {
        return transaction(db) {

            val now = System.currentTimeMillis()
            val expiry = now - 86400000

            Tables.Status
                .selectAll()
                .where {
                    (Tables.Status.user_id eq id) and
                            (Tables.Status.createdAt greaterEq expiry)
                }
                .map { it.toStatusModel() }
        }
    }
    fun getStatusesForUsers(ids: List<Int>): List<StatusWithUser> {
        return transaction(db) {

            if (ids.isEmpty()) return@transaction emptyList()

            val now = System.currentTimeMillis()
            val expiry = now - 86400000

            val join = Tables.Status
                .join(Tables.users, JoinType.INNER) {
                    Tables.Status.user_id eq Tables.users.id
                }
                .join(Tables.image, JoinType.LEFT) {
                    Tables.image.user_uid eq Tables.users.uid
                }

            join.selectAll()
                .where {
                    (Tables.Status.user_id inList ids) and
                            (Tables.Status.createdAt greaterEq expiry)
                }
                .orderBy(Tables.Status.createdAt to SortOrder.DESC)
                .map {

                    StatusWithUser(
                        userId = it[Tables.users.uid],
                        name = it[Tables.users.name],
                        profileImage = it[Tables.image.image],
                        statusImage = it[Tables.Status.statusImage],
                        createdAt = it[Tables.Status.createdAt]
                    )
                }
        }
    }
    fun deleteExpiredStatuses() {
        transaction(db) {

            val expiry = System.currentTimeMillis() - 86400000

            Tables.Status.deleteWhere {
                Tables.Status.createdAt lessEq expiry
            }
        }
    }
    fun deleteUserStatus(userId: Int): Boolean {
        return transaction(db) {

            val deletedRows = Tables.Status.deleteWhere {
                Tables.Status.user_id eq userId
            }

            deletedRows > 0
        }
    }
    fun deleteChatForUser(chatId: String, currentUid: String) {
        transaction {

            val conversation = Tables.Conversations
                .selectAll()
                .where { Tables.Conversations.chatId eq chatId }
                .singleOrNull() ?: return@transaction

            val conversationId = conversation[Tables.Conversations.id]

            // mark sender messages
            Tables.Messages.update({
                (Tables.Messages.conversationId eq conversationId) and
                        (Tables.Messages.senderUid eq currentUid)
            }) {
                it[deletedForSender] = true
            }

            // mark received messages
            Tables.Messages.update({
                (Tables.Messages.conversationId eq conversationId) and
                        (Tables.Messages.receiverUid eq currentUid)
            }) {
                it[deletedForReceiver] = true
            }
        }
    }
    fun deleteMessageForUser(messageId: String, currentUid: String) {
        transaction {

            // If I sent the message
            Tables.Messages.update({
                (Tables.Messages.messageId eq messageId) and
                        (Tables.Messages.senderUid eq currentUid)
            }) {
                it[deletedForSender] = true
            }

            // If I received the message
            Tables.Messages.update({
                (Tables.Messages.messageId eq messageId) and
                        (Tables.Messages.receiverUid eq currentUid)
            }) {
                it[deletedForReceiver] = true
            }
        }
    }
}











