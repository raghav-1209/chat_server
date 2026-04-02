package com.Notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message

fun followNotifications(
    token: String,
    senderId: Int,
    receiverId: Int,
    followerName: String,
    senderUid: String,
    receiverUid: String,
) {

    val message = Message.builder()
        .setToken(token)
        .putData("type", "follow_request")
        .putData("title", "Follow Request")
        .putData("body", "$followerName wants to follow you")
        .putData("senderId", senderId.toString())
        .putData("receiverId", receiverId.toString())
        .putData("senderUid",senderUid)
        .putData("receiverUid", receiverUid)
        .build()

    FirebaseMessaging.getInstance().send(message)
}
