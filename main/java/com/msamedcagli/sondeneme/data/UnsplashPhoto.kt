package com.msamedcagli.sondeneme.data

data class UnsplashPhoto(
    val id: String,
    val urls: PhotoUrls,
    val description: String?,
    val alt_description: String?
)

data class PhotoUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
) 