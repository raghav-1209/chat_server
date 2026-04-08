package com

import com.Notifications.followNotifications
import com.Notifications.messageNotification
import com.auth0.jwt.JWTVerifier
import com.database.DataBaseSource
import com.models.*
import com.services.AiChatService
import com.services.Client
import com.services.ImgBBResponse
import com.services.ImgBBService
import com.websockets.SessionManager
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*
import org.example.com.raghav.jwt.JwtService

fun Application.configureRouting(dataBaseSource: DataBaseSource, imgBBService: ImgBBService, jwtConfig: JwtConfig, aiService: AiChatService) {

    routing {
        authenticate("jwt_auth") {
            get("/check") {
                println(call.request.headers["Authorization"])

                call.respond(Info("Finally Got it"))

            }
            }
        configChats(dataBaseSource,aiService)
        connectWebSocket(dataBaseSource)
        configProfile(dataBaseSource,imgBBService)
        configAuth(dataBaseSource,imgBBService,jwtConfig)
        configActivity(dataBaseSource,jwtConfig)
        configStatus(dataBaseSource,imgBBService)
    }
}
fun Routing.configProfile(dataBaseSource: DataBaseSource,imgBBService: ImgBBService){
    authenticate("jwt_auth") {
    route("/profile") {

            post("/saveBio") {
                println("The Save Bio Fun CAlled")
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()

                val data = call.receive<BioData>()

                dataBaseSource.saveBio(
                    BioData(
                        uid = uid,
                        bio = data.bio
                    )
                )

                call.respond(BioData(data.uid,bio = data.bio))
            }
        get ("/getBio"){
            val principal = call.principal<JWTPrincipal>()
            val uid = principal!!.payload.getClaim("user_uid").asString()
            val info=dataBaseSource.getBio(uid)
            if(info==null){
                print("The Bio Doesnt Exist In Db")
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respond(BioData(
                uid,info.bio
            ))


        }
        post("/saveImage") {
            val data=call.receiveMultipart()
            val principal = call.principal<JWTPrincipal>()
            val uid = principal!!.payload.getClaim("user_uid").asString()
            var imageBytes: ByteArray? = null
            data.forEachPart { part ->

                when (part) {
                    is PartData.FileItem -> {
                        val bytes = part.streamProvider().readBytes()
                        imageBytes = bytes                    }

                    else -> {}
                }

                part.dispose()
            }

            if (imageBytes == null) {
                call.respond(HttpStatusCode.BadRequest, "No image found")
                return@post
            }
            println("The bytes i got from client is ${imageBytes}")
            val response= imgBBService.sendImage(imageBytes)
            println(response)

            response.onSuccess {
                dataBaseSource.saveImage(it.data.url,uid)
                call.respond(
                    ImgBBResponse(
                        it.data,
                        success = it.success,
                        status = it.status
                    )
                )

            }


        }
        get("/userInfo"){
            try {
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                println("The User Get FUn Called")
                val credentials=dataBaseSource.getUserByUid(uid)
                if(credentials==null){
                    print("The Credentials Doesnt Exist In Db When i Fetch for user Info")
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                val bio=dataBaseSource.getBio(uid)
                val image=dataBaseSource.getImage(uid)
                call.respond(WholeUser(
                    credentials = credentials,
                    image = image?.url,
                    bio = bio?.bio
                ))

            }catch (e:Exception){
                e.printStackTrace()
            }

        }
        get("/allUsers"){
            val principal = call.principal<JWTPrincipal>()
            val uid = principal!!.payload.getClaim("user_uid").asString()
            println("The all Users Get FUn Called")
            val allUsers=dataBaseSource.getAllUsers(uid)
            if(allUsers.isEmpty()){
                print("No Users Exist In Db")
                call.respond(HttpStatusCode.NotFound,"No Users InDb")
                return@get
            }
            println(allUsers)
            call.respond(allUsers)


        }

        get("/getUser/{following_Uid}") {
            try {
                val uid = call.parameters["following_Uid"] ?: return@get
                val data = dataBaseSource.getUserByUid(uid)
                if (data == null) {
                    call.respond(HttpStatusCode.NotFound, "user Details Doesnt Exist In Db")
                    return@get
                }
                val bio = dataBaseSource.getBio(uid)
                val image = dataBaseSource.getImage(uid)
                call.respond(
                    WholeUser(
                        credentials = data,
                        image = image?.url,
                        bio = bio?.bio
                    )
                )

            }catch (e: Exception){
                println(e)
            }
        }


        }


    }
}
fun Routing.configChats(dataBaseSource: DataBaseSource,aiService: AiChatService) {
    authenticate("jwt_auth") {
        route("/chats") {
            get("/getMessages/{chat_id}") {
                val principal=call.principal<JWTPrincipal>()?:return@get
                val uid=principal.payload.getClaim("user_uid").asString()
                val chat_id = call.parameters["chat_id"] ?: return@get
                val messages = dataBaseSource.getMessages(
                    chat_id,
                    uid
                )
                call.respond(messages)
            }
            post("/aiChat") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val uid = principal!!.payload.getClaim("user_uid").asString()
                    val data = call.receive<MessageData>() ?: return@post
                    val response = aiService.askOpenAI(uid, data.message)
                    response.onSuccess {
                        call.respond(
                            MessageData(
                                it
                            )
                        )
                    }
                    response.onFailure {
                        print(it)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to (it.message ?: "Unknown error"))
                        )
                        return@post
                    }


                } catch (e: Exception) {
                    println(e.message)
                }

            }
            delete("/deleteChat/{chat_id}") {
                val principal=call.principal<JWTPrincipal>()?:return@delete
                val uid=principal.payload.getClaim("user_uid").asString()?:return@delete
                val chat_id=call.parameters["chat_id"]?:""
                dataBaseSource.deleteChatForUser(chat_id,uid)
                call.respond(response(success = true, text = "Success"))
            }
            delete("/deleteMessage/{message_id}") {
                val principal=call.principal<JWTPrincipal>()?:return@delete
                val uid=principal.payload.getClaim("user_uid").asString()?:return@delete
                val message_id=call.parameters["message_id"]?:""
                dataBaseSource.deleteMessageForUser(message_id,uid)
                call.respond(response(success = true, text = "Success"))
            }

        }
    }
}
fun Route.connectWebSocket(dataBaseSource: DataBaseSource){
    authenticate("jwt_auth") {
        webSocket("/connect") {
            val principal = call.principal<JWTPrincipal>()
            if (principal == null) {
                println("JWT FAILED")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }
            val uid = principal.payload.getClaim("user_uid").asString()
            try {
                SessionManager.connect(uid, this)

                for (frame in incoming) {
                    if(frame is Frame.Text){
                       val text=frame.readText()
                        try {
                            val chatData = Json.decodeFromString<ChatData>(text)
                            val conversationId = dataBaseSource.getOrCreateConversation(chatData.chat_id)

                            dataBaseSource.saveMessage(
                                conversationId = conversationId,
                                messageId = chatData.messageId,
                                senderUid = chatData.senderUid,
                                receiverUid = chatData.receiverUid,
                                message = chatData.message,
                                time = chatData.time
                            )
                            val isOnline=SessionManager.isOnline(chatData.receiverUid)
                            if(isOnline)
                            SessionManager.sendToUser(chatData.receiverUid, text)
                            else{
                                val fcm = dataBaseSource.getFcmToken(chatData.receiverUid) ?: return@webSocket
                                val user=dataBaseSource.getUserByUid(chatData.senderUid)?:return@webSocket
                                messageNotification(chatData,fcm.token, userName = user.name)

                            }

                        } catch (e: Exception) {
                            println("JSON ERROR: ${e.message}")
                        }


                    }

                }

            } catch (e: Exception) {
                println("WebSocket error: ${e.message}")
            } finally {
                println("User disconnected: $uid")
                SessionManager.disConnect(uid)
            }
        }
        get ("/isOnline/{following_Uid}"){
            println(" The isOnline Fun called")
            val principal = call.principal<JWTPrincipal>()
            val uid = principal!!.payload.getClaim("user_uid").asString()
            val following_Uid=call.parameters["following_Uid"]?:return@get
            val isOnline=SessionManager.isOnline(if(uid==following_Uid)uid else following_Uid)
            if(isOnline){
                call.respond(
                    isOnline(
                        true,"Online"
                    )
                )
            }else call.respond(isOnline(false,"Offline"))

        }

        }

}
fun Routing.configStatus(dataBaseSource: DataBaseSource,imgBBService: ImgBBService){
    authenticate("jwt_auth") {
        route("/status") {
            post("/uploadStatus") {
                val data = call.receiveMultipart()
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                var imageBytes: ByteArray? = null
                data.forEachPart { part ->

                    when (part) {
                        is PartData.FileItem -> {
                            val bytes = part.streamProvider().readBytes()
                            imageBytes = bytes
                        }

                        else -> {}
                    }

                    part.dispose()
                }

                if (imageBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, "No image found")
                    return@post
                }
                println("The bytes i got from client is ${imageBytes}")
                val response = imgBBService.sendImage(imageBytes)
                println(response)
                response.onSuccess {

                    try {
                        val id = dataBaseSource.getInternalUserId(uid)

                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, "User not found")
                            return@post
                        }

                        dataBaseSource.uploadStatus(it.data.url, id)

                        call.respond(
                            ImgBBResponse(
                                it.data,
                                success = it.success,
                                status = it.status
                            )
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Server error")
                    }
                }
            }
            get("/getStatus") {

                println("The Get Status Fun Called")

                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, "Invalid token")

                val uid = principal.payload.getClaim("user_uid").asString()

                val id = dataBaseSource.getInternalUserId(uid)
                if (id == null) {
                    call.respond(HttpStatusCode.NotFound, "User id not found")
                    return@get
                }

                val response = dataBaseSource.getUserStatus(id)

                if (response.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "No status found")
                    return@get
                }

                call.respond(response)
            }
            get("/getFollowingStatus") {

                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val uid = principal.payload.getClaim("user_uid").asString()

                val myId = dataBaseSource.getInternalUserId(uid)
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                val followingIds = dataBaseSource.getFollowingIds(myId)

                val statuses = dataBaseSource.getStatusesForUsers(followingIds)

                call.respond(statuses)
            }
            delete("/deleteStatus") {
                val principal=call.principal<JWTPrincipal>()?:return@delete
                val uid=principal.payload.getClaim("user_uid").asString()
                val id=dataBaseSource.getInternalUserId(uid)
                if(id==null){
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }
                val delete=dataBaseSource.deleteUserStatus(id)
                call.respond(response(
                    delete,
                    text = if(delete==true) "Successfull" else "failed to Delete"
                ))

            }




        }
    }

}
fun Routing.configAuth(dataBaseSource: DataBaseSource, imgBBService: ImgBBService, jwtConfig: JwtConfig) {
    route("/auth") {
        post("/signIn") {
            println("The SiginIn Fun Called")
            val data=call.receive<SignInData>()?:return@post
          val response = Client.httpclient.post("http://localhost:8082/auth/signIn") {
                contentType(ContentType.Application.Json)
                setBody(data)
            }
            val resp=response.body<UserSession>()
            println(resp)
            call.respond(resp)

        }

        post ("/fcmToken"){
            try {
                println("The Fun Called fcm Token fun")
                val data = call.receive<TokenData>()
              val response= Client.httpclient.post {

              }
                println("the Token Of Device ${data.token}")
                call.respond(HttpStatusCode.OK)
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
        post("/login") {
            val data=call.receive<LoginData>()
            val info=dataBaseSource.getEmail(data.email)
            if(info==null){
                call.respond(HttpStatusCode.NotFound,"The User Not Found")
                return@post
            }
            val randomToken= UUID.randomUUID().toString()
            val accessToken=generateToken(info.uid,jwtConfig)
            dataBaseSource.saveJwtToken(TokenData(
                info.uid,
                token = randomToken,
            ))
            println("The server sends jwttoen to client ${accessToken}")
            call.respond(UserSession(randomToken,accessToken))
        }
        post("/refreshToken"){
            try {
                println("The RefreshToken Called")
                val data = call.receive<Info>()
                println("The Token I get ${data.token}")
                val info = dataBaseSource.getJwtToken(data.token)
                if (info == null) {
                    println("The refreshToken wasnt In Db")
                    call.respond(HttpStatusCode.NotFound, "The User Not Found")
                    return@post
                }
                val randomToken = UUID.randomUUID().toString()
                val accessToken = generateToken(info.uid, jwtConfig)
                dataBaseSource.saveJwtToken(TokenData(info.uid, token = randomToken))
                call.respond(UserSession(randomToken, accessToken))
            }catch (e:Exception){
                e.printStackTrace()
            }
        }



    }
}
fun Routing.configActivity(dataBaseSource: DataBaseSource,jwtConfig: JwtConfig) {
    authenticate("jwt_auth") {
        route(path = "/activity") {
            post("/follow/{following_uid}") {

                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                val targetUid = call.parameters["following_uid"] ?: return@post
                val followerId = dataBaseSource.getInternalUserId(uid) ?: return@post
                val followingId = dataBaseSource.getInternalUserId(targetUid) ?: return@post

                val state = dataBaseSource.toggleFollow(followerId, followingId)

                call.respond(FollowState(true, state))

                if (state == "REQUESTED") {
                    val fcmToken = dataBaseSource.getFcmToken(targetUid) ?: return@post
                    val userInfo = dataBaseSource.getUserByUid(uid) ?: return@post

                    followNotifications(
                        fcmToken.token,
                        followerId,
                        followingId,
                        userInfo.name,
                        uid,
                        targetUid
                    )
                }


            }
            post("/accept/{follower_Uid}") {
                println("The Accept Fun called")
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                val follower_Uid = call.parameters["follower_Uid"] ?: return@post
                val receiver_Id = dataBaseSource.getInternalUserId(uid) ?: return@post
                val sender_Id = dataBaseSource.getInternalUserId(follower_Uid) ?: return@post
                println("Accepting follow: senderId=$sender_Id, receiverId=$receiver_Id")
                val success = dataBaseSource.acceptFollow(sender_Id, receiver_Id)

                if (!success) {
                    call.respond(HttpStatusCode.NotFound, "No Pending Request")
                    return@post
                }

                call.respond(HttpStatusCode.OK)


            }
            post("/reject/{follower_Uid}") {
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                val follower_Uid = call.parameters["follower_Uid"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "The User Not Found"
                )
                val receiver_Id = dataBaseSource.getInternalUserId(uid) ?: return@post
                val sender_Id = dataBaseSource.getInternalUserId(follower_Uid) ?: return@post
                val failure = dataBaseSource.rejectFollow(sender_Id, receiver_Id)

                if (!failure) {
                    call.respond(HttpStatusCode.NotFound, "No Pending Request")
                    return@post
                }

                call.respond(HttpStatusCode.OK)

            }
            get("/followState/{following_Uid}") {
                val principal = call.principal<JWTPrincipal>()
                val uid = principal!!.payload.getClaim("user_uid").asString()
                val targetUid = call.parameters["following_Uid"] ?: return@get

                val user1 = dataBaseSource.getInternalUserId(uid) ?: return@get
                val user2 = dataBaseSource.getInternalUserId(targetUid) ?: return@get
                val response = dataBaseSource.getFollowState(user1, user2)
                call.respond(FollowState(true, response))


            }
            get("/getFollowers") {
                try {
                    println("The Get followers Fun called")
                    val principal = call.principal<JWTPrincipal>()
                    val uid = principal!!.payload.getClaim("user_uid").asString()
                    val id = dataBaseSource.getInternalUserId(uid) ?: return@get
                    val followCount = dataBaseSource.getFollowersWithDetails(id)
                    call.respond(FollowInfo(followCount.size, followCount))
                } catch (e: Exception) {
                    println("${e.message}")
                }
            }
            get("/getFollowing") {
                try {
                    println("The Get Following Fun called")
                    val principal = call.principal<JWTPrincipal>()
                    val uid = principal!!.payload.getClaim("user_uid").asString()
                    val id = dataBaseSource.getInternalUserId(uid) ?: return@get
                    val info = dataBaseSource.getFollowingWithDetails(id)
                    call.respond(FollowInfo(info.size, info))
                }catch (e:Exception){
                    println("${e.message}")
                }
            }


        }
    }
}
@Serializable
data class MessageData(val message: String)
@Serializable
data class Info(
    val token: String
)

@Serializable
data class response(
    val success: Boolean,
    val text: String
)