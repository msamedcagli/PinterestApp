package com.msamedcagli.sondeneme

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.msamedcagli.sondeneme.databinding.FragmentUploadBinding
import java.io.IOException
import java.util.UUID
import androidx.appcompat.widget.PopupMenu

class UploadFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    // Görsel seçme ve izin isteme işlemleri için launcher'lar
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    var secilenGorsel: Uri? = null  // Kullanıcının seçtiği görselin URI'si
    var secilenBitmap: Bitmap? = null  // URI'den elde edilen bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()  // Görsel seçimi ve izin için launcher'ları kaydet
        auth = Firebase.auth
        db = Firebase.firestore
        checkStoragePermission()  // Depolama izni kontrolü yap
    }

    // Depolama izni kontrolü
    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()  // Butonlara tıklama işlemleri tanımlanır
        binding.imageView.setOnClickListener { imageViewClicked(it) }  // Görsel seçimi
        binding.uploadButton.setOnClickListener { uploadClicked(it) }  // Yükleme işlemi
    }

    // Sayfa içindeki butonlara tıklama olaylarını ayarlayan fonksiyon
    private fun setupClickListeners() {
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }

        // Ana sayfaya geçiş
        binding.AnaSayfaButton.setOnClickListener {
            val action = UploadFragmentDirections.actionUploadFragmentToMainPageFragment()
            Navigation.findNavController(it).navigate(action)
        }

        // İndirilenler sayfasına geçiş
        binding.indirilenlerButton.setOnClickListener {
            val action = UploadFragmentDirections.actionUploadFragmentToSonindirilenlerFragment()
            Navigation.findNavController(it).navigate(action)
        }

        // Şu an bu sayfada olduğumuz için ekstra işlem yapılmıyor
        binding.GonderiEkleButton.setOnClickListener {}
    }

    // Menü gösterimi
    private fun showPopupMenu(view: View) {
        try {
            val popup = PopupMenu(requireContext(), binding.floatingActionButton)
            val inflater = popup.menuInflater
            inflater.inflate(R.menu.pop_menu, popup.menu)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Menü açılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Menüde "Çıkış yap" seçeneği tıklanınca ana sayfaya yönlendirme
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        try {
            if (item?.itemId == R.id.cikisYap) {
                val action = UploadFragmentDirections.actionUploadFragmentToMainPageFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Çıkış yapılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return true
    }

    // Yükleme butonuna tıklandığında çalışan fonksiyon
    fun uploadClicked(view: View) {
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val storage = Firebase.storage
        val reference = storage.reference
        val imagesReference = reference.child("images").child(imageName)

        if (secilenGorsel != null) {
            // Firebase Storage'a görsel yükleniyor
            imagesReference.putFile(secilenGorsel!!)
                .addOnSuccessListener {
                    // Yükleme başarılıysa indirme linki alınır
                    imagesReference.downloadUrl.addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            // Gönderi bilgileri Firestore'a eklenir
                            val postMap = hashMapOf<String, Any>()
                            postMap["downloadUrl"] = downloadUrl
                            postMap["userEmail"] = currentUser.email.toString()
                            postMap["comment"] = binding.editText.text.toString()
                            postMap["date"] = Timestamp.now()
                            postMap["isDownloaded"] = true

                            db.collection("Posts").add(postMap).addOnSuccessListener {
                                // Başarıyla veri eklendiyse görsel indir
                                downloadImage(requireContext(), downloadUrl)
                                val action = UploadFragmentDirections.actionUploadFragmentToMainPageFragment()
                                Navigation.findNavController(view).navigate(action)
                            }.addOnFailureListener {
                                Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Lütfen giriş yapın.", Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "Lütfen bir görsel seçin.", Toast.LENGTH_SHORT).show()
        }
    }

    // Glide kullanarak görseli indirir
    private fun downloadImage(context: Context, imageUrl: String) {
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    saveImageToGallery(context, resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    // İndirilen görseli galeriye kaydeder
    private fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        val filename = "image_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            Toast.makeText(context, "Görsel indirildi", Toast.LENGTH_SHORT).show()
        }
    }

    // Görsel seçmek için imageView'e tıklanma işlemi
    fun imageViewClicked(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                Snackbar.make(view, "Galeri erişimi için izin gerekli", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin Ver") { permissionLauncher.launch(permission) }
                    .show()
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    // Görsel seçimi ve izin işlemleri için launcher'lar burada tanımlanır
    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    secilenGorsel = intentFromResult.data
                    try {
                        secilenBitmap = if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, secilenGorsel!!)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, secilenGorsel)
                        }
                        binding.imageView.setImageBitmap(secilenBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                Toast.makeText(requireContext(), "İzin gerekli!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}