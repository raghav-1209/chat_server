package com.models

import kotlinx.serialization.Serializable

@Serializable
data class BioData(
    val uid: String,
    val bio: String
)
