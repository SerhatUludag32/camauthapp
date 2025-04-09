package com.serhatuludag.camauthapp.data.model

import android.os.Parcel
import android.os.Parcelable
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
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type)
        parcel.writeString(content)
        parcel.writeInt(width)
        parcel.writeInt(height)
        parcel.writeInt(positionX)
        parcel.writeInt(positionY)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OverlayData> {
        override fun createFromParcel(parcel: Parcel): OverlayData {
            return OverlayData(parcel)
        }

        override fun newArray(size: Int): Array<OverlayData?> {
            return arrayOfNulls(size)
        }
    }
}