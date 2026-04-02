package com.models

import kotlinx.serialization.Serializable

@Serializable
data class TokenData(
    val uid: String,
    val token: String
)
