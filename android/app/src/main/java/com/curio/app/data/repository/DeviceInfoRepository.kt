package com.curio.app.data.repository

import com.curio.app.data.api.RetrofitClient
import com.curio.app.data.model.DeviceInfo
import com.curio.app.data.model.DeviceInfoRequest

class DeviceInfoRepository {

    private val api = RetrofitClient.deviceInfoApi

    suspend fun getDeviceInfo(deviceId: String): Result<DeviceInfo> {
        return try {
            val response = api.getDeviceInfo(deviceId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch device info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDeviceInfo(deviceId: String, request: DeviceInfoRequest): Result<DeviceInfo> {
        return try {
            val response = api.submitDeviceInfo(deviceId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to save device info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
