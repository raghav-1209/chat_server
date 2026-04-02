package com.models

data class UserWithFollowState(
val uid: String,
val name: String,
val bio: String?,
val image: String?,
val followState: String // "none" "requested" "following" "friends"


)
