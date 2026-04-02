package com.models

import kotlinx.serialization.Serializable

@Serializable
data class StatusModel(
    val userId: Int,
    val url: String,
    val createdAt: Long
)