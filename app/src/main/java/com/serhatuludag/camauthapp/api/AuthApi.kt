package com.serhatuludag.camauthapp.api

import com.serhatuludag.camauthapp.data.model.AccessCodeValidationRequest
import com.serhatuludag.camauthapp.data.model.AccessCodeValidationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/validate")
    suspend fun validateAccessCode(
        @Body request: AccessCodeValidationRequest
    ): Response<AccessCodeValidationResponse>
}