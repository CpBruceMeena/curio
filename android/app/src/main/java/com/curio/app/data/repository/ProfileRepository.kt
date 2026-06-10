package com.curio.app.data.repository

import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.Profile
import com.curio.app.data.model.ProfileRequest

class ProfileRepository {

    private val api = RetrofitClient.profileApi

    suspend fun getProfile(deviceId: String): Result<Profile> {
        return try {
            val response = api.getProfile(deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfile(deviceId: String, request: ProfileRequest): Result<Profile> {
        return try {
            val response = api.createOrUpdateProfile(deviceId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to save profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
