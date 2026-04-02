package com.models

import kotlinx.serialization.Serializable

@Serializable
data class WholeUser(
    val credentials: UserData,
    val image: String?=null,
    val bio: String?=null
)
