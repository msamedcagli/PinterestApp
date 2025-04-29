package com.msamedcagli.sondeneme.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.UUID

// Unsplash API istemcisi için singleton obje
object UnsplashApiClient {
    private const val BASE_URL = "https://api.unsplash.com/" // API'nın temel URL'si
    private const val UNSPLASH_ACCESS_KEY = "66RNYT3-IhrQUFbQ-PPKIx3Bs34uue1bLdTrVAJryJk" // Unsplash API erişim anahtarı

    // HTTP isteklerini loglamak için interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient oluşturuluyor ve yetkilendirme header'ı ekleniyor
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Client-ID $UNSPLASH_ACCESS_KEY")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor) // Log interceptor'u ekleniyor
        .build()

    // Retrofit nesnesi oluşturuluyor
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()) // JSON dönüşümleri için
        .build()
    // Retrofit üzerinden API arayüzü oluşturuluyor
    val api: UnsplashApi = retrofit.create(UnsplashApi::class.java)

    // Belirtilen URL'deki resmi indirir, Firebase'e yükler ve Firestore'a kaydeder
    suspend fun downloadAndSaveImage(imageUrl: String, comment: String, userEmail: String): String {
        return withContext(Dispatchers.IO) { // Ağ işlemleri IO thread'de yapılır
            try {
                // Resmi URL'den indir
                val url = URL(imageUrl)
                val bitmap = BitmapFactory.decodeStream(url.openStream())

                // Firebase Storage referansı oluştur
                val storage = FirebaseStorage.getInstance()
                val reference = storage.reference
                val uuid = UUID.randomUUID()
                val imageName = "$uuid.jpg"
                val imagesReference = reference.child("images").child(imageName)

                // Bitmap'i byte array'e çevir
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                // Firebase Storage'a resmi yükle
                val uploadTask = imagesReference.putBytes(data)
                val downloadUrl = uploadTask.await().storage.downloadUrl.await().toString()

                // Firestore'a gönderi bilgilerini kaydet
                val postMap = hashMapOf<String, Any>()
                postMap["downloadUrl"] = downloadUrl
                postMap["userEmail"] = userEmail
                postMap["comment"] = comment
                postMap["date"] = Timestamp.now()

                val db = FirebaseFirestore.getInstance()
                db.collection("Posts").add(postMap).await()

                // İndirilebilir URL'yi döndür
                downloadUrl
            } catch (e: Exception) {
                // Bir hata olursa yukarı fırlat
                throw e
            }
        }
    }
}
