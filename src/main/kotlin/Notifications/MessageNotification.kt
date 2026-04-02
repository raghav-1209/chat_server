package com.Notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.models.ChatData

fun messageNotification(chatData: ChatData,token: String,userName: String){
    val message= Message.builder()
        .putData("type", "chat")
        .putData("receiverUid",chatData.receiverUid)
        .putData("senderUid", chatData.senderUid)
        .putData("text", chatData.message)
        .putData("senderName",userName)
        .setToken(token)
        .build()
    FirebaseMessaging.getInstance().send(message)


}