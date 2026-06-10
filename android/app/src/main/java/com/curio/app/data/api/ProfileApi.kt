package com.curio.app.data.api

import com.curio.app.data.model.Profile
import com.curio.app.data.model.ProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ProfileApi {

    @GET("profile")
    suspend fun getProfile(
        @Query("device_id") deviceId: String
    ): Response<Profile>

    @POST("profile")
    suspend fun createOrUpdateProfile(
        @Query("device_id") deviceId: String,
        @Body request: ProfileRequest
    ): Response<Profile>
}
