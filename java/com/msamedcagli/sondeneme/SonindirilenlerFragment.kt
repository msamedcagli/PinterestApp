package com.msamedcagli.sondeneme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.msamedcagli.sondeneme.databinding.FragmentSonindirilenlerBinding

class SonindirilenlerFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentSonindirilenlerBinding? = null
    private val binding get() = _binding!!
    private var adapter: PostAdapter? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Firebase Authentication ve Firestore nesnelerini başlatıyoruz
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    // Fragment için view (görünüm) oluşturuluyor, ViewBinding ile layout bağlanıyor
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSonindirilenlerBinding.inflate(inflater, container, false)
        return binding.root
    }

    // View hazır olduğunda yapılacak işlemler
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()      // RecyclerView kurulumu
        setupClickListeners()    // Buton tıklamaları ayarlanıyor
        getDownloadedImages()    // Firestore'dan indirilen görseller çekiliyor
    }

    // RecyclerView kurulumu: Grid düzeni, adapter bağlama
    private fun setupRecyclerView() {
        adapter = PostAdapter(arrayListOf()) // Boş liste ile adapter başlatılıyor
        binding.inRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inRecyclerView.adapter = adapter
    }

    // Firestore'dan giriş yapan kullanıcının isDownloaded = true olan verileri çekiliyor
    private fun getDownloadedImages() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Posts")
                .whereEqualTo("userEmail", currentUser.email)          // Sadece bu kullanıcıya ait
                .whereEqualTo("isDownloaded", true)                   // İndirilen postlar
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->

                    // Hata varsa göster
                    if (error != null) {
                        Toast.makeText(requireContext(), "Veriler yüklenirken hata oluştu: ${error.message}", Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }

                    // Başarıyla gelen veriler işleniyor
                    val downloadedPosts = ArrayList<Post>()
                    value?.documents?.forEach { document ->
                        val downloadUrl = document.getString("downloadUrl") ?: ""
                        val comment = document.getString("comment") ?: ""
                        val userEmail = document.getString("userEmail") ?: ""
                        val post = Post(
                            userEmail = userEmail,
                            comment = comment,
                            downloadUrl = downloadUrl,
                            isDownloaded = true
                        )
                        downloadedPosts.add(post)
                    }

                    // Liste boşsa "Boş içerik" mesajı göster, değilse listeyi göster
                    if (downloadedPosts.isEmpty()) {
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.inRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyStateText.visibility = View.GONE
                        binding.inRecyclerView.visibility = View.VISIBLE
                    }

                    // Adapter’a yeni veri seti veriliyor
                    adapter?.updatePosts(downloadedPosts)
                }
        } else {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış", Toast.LENGTH_LONG).show()
        }
    }

    // Sayfadaki butonlara tıklanma olayları burada tanımlanır
    private fun setupClickListeners() {
        // Menü (popup) gösteren buton
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }

        // Ana sayfaya dön
        binding.AnaSayfaButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToMainPageFragment()
            Navigation.findNavController(it).navigate(action)
        }

        // Gönderi ekleme sayfasına git
        binding.GonderiEkleButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

    // Sayfada üst menüyü (PopupMenu) gösterir
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

    // Menüdeki "Çıkış Yap" seçeneğine tıklanırsa yapılacak işlemler
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        try {
            if (item?.itemId == R.id.cikisYap) {
                auth.signOut() // Firebase oturum kapatma
                val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToKullaniciFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Çıkış yapılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return true
    }

    // Bellek sızıntısını önlemek için ViewBinding null’a çekiliyor
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
