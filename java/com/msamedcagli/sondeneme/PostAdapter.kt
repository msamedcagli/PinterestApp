package com.msamedcagli.sondeneme

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// RecyclerView için adapter: Post listesini gösterir
class PostAdapter(private val postList: ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val TAG = "PostAdapter"

    // RecyclerView satırının bileşenlerini tutar
    class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.recyclerviewImageview)
        val downloadButton: ImageView = itemView.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_row, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        Log.d(TAG, "onBindViewHolder: Post URL: ${post.downloadUrl}")

        // Görseli Glide ile yükle
        Glide.with(holder.itemView.context).load(post.downloadUrl).into(holder.imageView)

        // İndirme butonuna tıklanınca görseli indir
        holder.downloadButton.setOnClickListener {
            Log.d(TAG, "Download button clicked for URL: ${post.downloadUrl}")
            downloadImage(holder.itemView.context, post)
        }
    }

    override fun getItemCount(): Int = postList.size

    // Liste güncellendiğinde RecyclerView'ı yeniler
    fun updatePosts(newPosts: List<Post>) {
        postList.clear()
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    // Glide ile görsel indirildikten sonra Firestore ve galeriye kaydedilir
    private fun downloadImage(context: Context, post: Post) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(context, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show()
                return
            }
            Glide.with(context).asBitmap().load(post.downloadUrl).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    try {
                        // Galeriye kaydet
                        saveImageToGallery(context, resource)

                        // Firestore'a kaydet veya güncelle
                        db.collection("Posts")
                            .whereEqualTo("downloadUrl", post.downloadUrl)
                            .whereEqualTo("userEmail", currentUser.email)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    // Yeni kayıt oluştur
                                    val newPost = hashMapOf(
                                        "downloadUrl" to post.downloadUrl,
                                        "userEmail" to currentUser.email,
                                        "comment" to "",
                                        "isDownloaded" to true,
                                        "date" to com.google.firebase.Timestamp.now()
                                    )
                                    db.collection("Posts")
                                        .add(newPost)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Görsel indirildi ve kaydedildi", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Görsel kaydedilirken hata oluştu", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // Var olan kaydı güncelle
                                    for (document in documents) {
                                        db.collection("Posts").document(document.id)
                                            .update("isDownloaded", true)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Görsel indirildi ve kaydedildi", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Görsel kaydedilirken hata oluştu", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Veritabanı bağlantısında hata oluştu", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Görsel işlenirken hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // Gerekiyorsa temizlik işlemleri burada yapılabilir
                }
            })
        } catch (e: Exception) {
            Toast.makeText(context, "İndirme işlemi sırasında hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    // Bitmap görseli galeriye kaydeder
    private fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        Log.d(TAG, "Saving image to gallery")
        val filename = "image_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Log.d(TAG, "Image saved to gallery successfully")
            }
            Toast.makeText(context, "Görsel galeriye kaydedildi", Toast.LENGTH_SHORT).show()
        } ?: run {
            Log.e(TAG, "Failed to save image to gallery")
            Toast.makeText(context, "Görsel kaydedilemedi", Toast.LENGTH_SHORT).show()
        }
    }
}
