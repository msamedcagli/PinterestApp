package com.msamedcagli.sondeneme.api

import com.msamedcagli.sondeneme.data.UnsplashPhoto
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApi {
    @GET("photos")
    suspend fun getPhotos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<UnsplashPhoto>

    @GET("photos/random")
    suspend fun getRandomPhotos(
        @Query("count") count: Int = 30,
        @Query("orientation") orientation: String = "landscape",
        @Query("query") query: String = ""
    ): List<UnsplashPhoto>
} 