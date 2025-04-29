package com.msamedcagli.sondeneme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.msamedcagli.sondeneme.Post
import com.msamedcagli.sondeneme.api.UnsplashApiClient
import com.msamedcagli.sondeneme.data.UnsplashPhoto
import kotlinx.coroutines.launch

// ViewModel: UI'dan bağımsız veri işlemlerini yönetir.
class MainViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance() // Firebase Authentication referansı
    private val db = FirebaseFirestore.getInstance() // Firestore referansı
    // Gönderiler için LiveData
    private val _posts = MutableLiveData<List<Post>>() // Değiştirilebilir iç veri
    val posts: LiveData<List<Post>> = _posts // Dışa salt okunur olarak sunulur
    // Unsplash fotoğrafları için LiveData
    private val _unsplashPhotos = MutableLiveData<List<UnsplashPhoto>>()
    val unsplashPhotos: LiveData<List<UnsplashPhoto>> = _unsplashPhotos
    // Hatalar için LiveData
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Firestore'dan gönderileri yükler
    fun loadPosts() {
        db.collection("Posts")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // Tarihe göre sıralar
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _error.value = "Veri alınırken hata oluştu: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {
                    val postsList = mutableListOf<Post>()
                    for (document in value.documents) {
                        try {
                            // Her belgeyi Post nesnesine dönüştür
                            val comment = document.get("comment") as String
                            val userEmail = document.get("userEmail") as String
                            val downloadUrl = document.get("downloadUrl") as String
                            postsList.add(Post(userEmail, comment, downloadUrl))
                        } catch (e: Exception) {
                            _error.value = "Veri dönüştürme hatası: ${e.message}"
                        }
                    }
                    _posts.value = postsList // UI'ya güncel gönderi listesini bildir
                }
            }
    }

    // Unsplash API'den rastgele fotoğrafları çeker
    fun loadUnsplashPhotos() {
        viewModelScope.launch {
            try {
                val photos = UnsplashApiClient.api.getRandomPhotos()
                _unsplashPhotos.value = photos
            } catch (e: Exception) {
                _error.value = "Unsplash fotoğrafları yüklenirken hata oluştu: ${e.message}"
            }
        }
    }

    // Fotoğrafları yenilemek için tekrar yükleme fonksiyonu
    fun refreshPhotos() {
        loadUnsplashPhotos()
    }

    // Unsplash API üzerinden arama yapar
    fun searchUnsplashPhotos(query: String) {
        viewModelScope.launch {
            try {
                val photos = UnsplashApiClient.api.getRandomPhotos(count = 30, query = query)
                _unsplashPhotos.value = photos
            } catch (e: Exception) {
                _error.value = "Arama sırasında hata oluştu: ${e.message}"
            }
        }
    }

    // Kullanıcı çıkış işlemi
    fun signOut() {
        auth.signOut()
    }
}
