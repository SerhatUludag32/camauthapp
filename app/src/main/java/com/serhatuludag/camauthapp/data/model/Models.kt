package com.serhatuludag.camauthapp.data.model

import com.google.gson.annotations.SerializedName

data class AccessCodeValidationRequest(
    val code: String,
    @SerializedName("device_info")
    val deviceInfo: Map<String, String>
)

data class AccessCodeValidationResponse(
    val token: String,
    val sessionId: String
)

data class OverlayMessage(
    val type: String,
    val data: OverlayData
)

data class OverlayData(
    val type: String,
    val content: String,
    val width: Int,
    val height: Int,
    @SerializedName("position_x")
    val positionX: Int,
    @SerializedName("position_y")
    val positionY: Int
)