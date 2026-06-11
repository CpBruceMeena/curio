package com.curio.app.data.api

import com.curio.app.data.model.DeviceInfo
import com.curio.app.data.model.DeviceInfoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DeviceInfoApi {

    @GET("device-info")
    suspend fun getDeviceInfo(
        @Query("device_id") deviceId: String
    ): Response<DeviceInfo>

    @POST("device-info")
    suspend fun submitDeviceInfo(
        @Query("device_id") deviceId: String,
        @Body request: DeviceInfoRequest
    ): Response<DeviceInfo>
}
