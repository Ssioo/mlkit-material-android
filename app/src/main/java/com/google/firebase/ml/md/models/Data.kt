package com.google.firebase.ml.md.models

import com.google.gson.annotations.SerializedName

data class Data(
        @SerializedName("images") var images : List<Image>?
) {
    constructor() : this(null)
}



data class Image(
        @SerializedName("width") var width: Int,
        @SerializedName("height") var height: Int,
        @SerializedName("id") var id: Int,
        @SerializedName("imgUrl")  var imgUrl: String?
) {
    constructor() : this(0, 0, 0, null)
}