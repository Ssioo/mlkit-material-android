package com.google.firebase.ml.md.models

import com.google.gson.annotations.SerializedName

data class ImageInfo(
     @SerializedName("dots") var dots: List<ImageInfo?>?,
     @SerializedName("imageUrl") var imgUrl: String?,
     @SerializedName("imgX") var imgX: Int,
     @SerializedName("imgY") var imgY: Int,
     @SerializedName("title") var title: String?
)