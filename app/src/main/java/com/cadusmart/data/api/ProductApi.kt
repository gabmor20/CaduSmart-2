package com.cadusmart.data.api

import com.cadusmart.data.dto.OkResponse
import com.cadusmart.data.dto.ProductRequest
import com.cadusmart.data.dto.ProductResponse
import com.cadusmart.data.dto.ProductsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProductApi {
    @GET("products")
    suspend fun list(): ProductsResponse

    @GET("products/trash")
    suspend fun trash(): ProductsResponse

    @POST("products")
    suspend fun create(@Body body: ProductRequest): ProductResponse

    @PUT("products/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: ProductRequest): ProductResponse

    @POST("products/{id}/consume")
    suspend fun consume(@Path("id") id: Int): ProductResponse

    @DELETE("products/{id}")
    suspend fun delete(@Path("id") id: Int): OkResponse

    @DELETE("products/{id}/purge")
    suspend fun purge(@Path("id") id: Int): OkResponse
}
