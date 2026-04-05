package com.traveling.data.remote

import com.traveling.domain.model.AuthRequest
import com.traveling.domain.model.AuthResponse
import com.traveling.domain.model.ProfilePictureResponse
import com.traveling.domain.model.RegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @Multipart
    @PATCH("auth/profile-picture")
    suspend fun updateProfilePicture(
        @Part avatar: MultipartBody.Part,
        @Part("userId") userId: RequestBody
    ): ProfilePictureResponse
}
