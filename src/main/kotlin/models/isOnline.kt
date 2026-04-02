package com.models

import kotlinx.serialization.Serializable

@Serializable
data class isOnline(
    val isOnline: Boolean,
    val text: String
)
