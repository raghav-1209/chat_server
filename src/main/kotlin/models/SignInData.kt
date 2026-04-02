package com.models

import kotlinx.serialization.Serializable

@Serializable
data class SignInData(
    val email: String,
    val name: String,
    val uid: String
)

