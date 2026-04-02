package com.models

import kotlinx.serialization.Serializable

@Serializable
data class StatusWithUser(
    val userId: String,
    val name: String,
    val profileImage: String?,
    val statusImage: String,
    val createdAt: Long
)
