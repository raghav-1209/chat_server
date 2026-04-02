package com.models

import com.enums.FollowStatus
import kotlinx.serialization.Serializable

@Serializable
data class FollowInfo(
    val size: Int,
    val data:List<WholeUser>
)
