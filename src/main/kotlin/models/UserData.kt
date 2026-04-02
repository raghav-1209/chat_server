package com.models

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val name: String,
    val email: String,
    val uid: String,
)
