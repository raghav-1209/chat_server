package com.models

import kotlinx.serialization.Serializable

@Serializable
data class FollowState(
    val isFollowing: Boolean,
  val state: String
)



