package com.cadusmart.data.api

import com.cadusmart.data.dto.CreateUserRequest
import com.cadusmart.data.dto.DeleteUserResponse
import com.cadusmart.data.dto.ManagedUserResponse
import com.cadusmart.data.dto.ManagedUsersResponse
import com.cadusmart.data.dto.OkResponse
import com.cadusmart.data.dto.ResetPasswordRequest
import com.cadusmart.data.dto.UpdateUserRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("users")
    suspend fun list(): ManagedUsersResponse

    @POST("users")
    suspend fun create(@Body body: CreateUserRequest): ManagedUserResponse

    @PUT("users/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: UpdateUserRequest): ManagedUserResponse

    @POST("users/{id}/reset-password")
    suspend fun resetPassword(@Path("id") id: Int, @Body body: ResetPasswordRequest): OkResponse

    @DELETE("users/{id}")
    suspend fun delete(@Path("id") id: Int): DeleteUserResponse
}
