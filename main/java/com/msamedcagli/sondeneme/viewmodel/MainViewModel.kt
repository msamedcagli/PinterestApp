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

class MainViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    private val _unsplashPhotos = MutableLiveData<List<UnsplashPhoto>>()
    val unsplashPhotos: LiveData<List<UnsplashPhoto>> = _unsplashPhotos
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadPosts() {
        db.collection("Posts")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _error.value = "Veri alınırken hata oluştu: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                if (value != null && !value.isEmpty) {
                    val postsList = mutableListOf<Post>()
                    for (document in value.documents) {
                        try {
                            val comment = document.get("comment") as String
                            val userEmail = document.get("userEmail") as String
                            val downloadUrl = document.get("downloadUrl") as String
                            postsList.add(Post(userEmail, comment, downloadUrl))
                        } catch (e: Exception) {
                            _error.value = "Veri dönüştürme hatası: ${e.message}"
                        }
                    }
                    _posts.value = postsList
                }
            }
    }
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
    fun refreshPhotos() {
        loadUnsplashPhotos()
    }
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
    fun signOut() {
        auth.signOut()
    }
}
