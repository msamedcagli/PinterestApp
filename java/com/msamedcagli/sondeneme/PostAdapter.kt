package com.msamedcagli.sondeneme

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import android.provider.MediaStore
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

// PostAdapter, RecyclerView için bir adapter sınıfıdır ve Post verilerini listeler
class PostAdapter(private val postList: ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostHolder>() {

    private val auth = Firebase.auth // Firebase authentication örneği
    private val db = Firebase.firestore // Firestore veritabanı referansı

    // ViewHolder sınıfı: RecyclerView satırının bileşenlerini tutar
    class PostHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.recyclerviewImageview) // Görsel
        val downloadButton: ImageView = itemView.findViewById(R.id.downloadButton) // İndirme butonu
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostHolder {
        // Yeni bir RecyclerView satırı oluşturulduğunda bu metot çağrılır
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_row, parent, false)
        return PostHolder(view)
    }

    override fun onBindViewHolder(holder: PostHolder, position: Int) {
        val post = postList[position]
        // Glide ile görsel URL’den yüklenir ve ImageView'a aktarılır
        Glide.with(holder.itemView.context).load(post.downloadUrl).into(holder.imageView)

        // İndirme butonuna tıklandığında görsel indirme işlemi başlatılır
        holder.downloadButton.setOnClickListener {
            downloadImage(holder.itemView.context, post)
        }
    }

    override fun getItemCount(): Int = postList.size // Liste eleman sayısını döndürür

    // Yeni veriler geldiğinde RecyclerView’ı güncellemek için kullanılan metot
    fun updatePosts(newPosts: List<Post>) {
        postList.clear()
        postList.addAll(newPosts)
        notifyDataSetChanged()
    }

    // Glide ile görsel indirildikten sonra bitmap formatında kaydedilir
    private fun downloadImage(context: Context, post: Post) {
        Glide.with(context).asBitmap().load(post.downloadUrl).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    saveImageToGallery(context, resource) // İndirilmiş bitmap’i kaydet
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Glide için gerekli, ama burada kullanılmıyor
                }
            })
    }

    // Bitmap’i cihazın galerisinde belirli bir klasöre kaydeder
    private fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        val filename = "image_${System.currentTimeMillis()}.jpg" // Dosya adı oluştur
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp") // "MyApp" klasörü
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            Toast.makeText(context, "Görsel galeriye kaydedildi", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(context, "Görsel kaydedilemedi", Toast.LENGTH_SHORT).show()
        }
    }
}
