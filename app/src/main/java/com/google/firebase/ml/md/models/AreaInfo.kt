package com.google.firebase.ml.md.models

import com.google.gson.annotations.SerializedName

data class AreaInfo(
        @SerializedName("imgUrl") var imgUrl: String?,
        @SerializedName("title") var title: String?,
        @SerializedName("positionX") var positionX: Int,
        @SerializedName("positionY") var positionY: Int,
        @SerializedName("soundUrl") var soundUrl: String?
)