package com.models

import kotlinx.serialization.Serializable

@Serializable
data class FcmData(
    val uid: String,
    val token: String,
)
