package com.curio.app.data.model

import com.google.gson.annotations.SerializedName

data class Profile(
    val id: Long = 0,
    @SerializedName("device_id")
    val deviceId: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val interests: String = "",
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName("updated_at")
    val updatedAt: String = ""
)

data class ProfileRequest(
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val likes: String = "",
    val dislikes: String = ""
)
