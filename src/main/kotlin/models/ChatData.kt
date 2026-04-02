package com.models

import kotlinx.serialization.Serializable


@Serializable
data class ChatData(
    val messageId: String,
    val chat_id: String,
    val senderUid: String,
    val receiverUid: String,
    val message: String,
    val time: Long
)