package com.example.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interface Retrofit que define os contratos de comunicação HTTP com a API maxsegapiahy-hub/api.
 */
interface MaxSegApiService {

    @GET("partners")
    suspend fun getPartners(): Response<List<ApiPartnerDto>>

    @GET("partners/{id}")
    suspend fun getPartnerById(@Path("id") id: String): Response<ApiPartnerDto>

    @POST("partners")
    suspend fun createPartner(@Body partner: ApiPartnerDto): Response<ApiPartnerDto>

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, String>>
}
