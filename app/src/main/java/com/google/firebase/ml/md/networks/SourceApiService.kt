package com.google.firebase.ml.md.networks

import com.google.firebase.ml.md.models.ImageInfo
import retrofit2.Call
import retrofit2.http.GET

interface SourceApiService {
    @GET("/skku2020_capstone")
    fun getItem(no: Int) : Call<ImageInfo>
}