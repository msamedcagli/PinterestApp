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
import com.google.firebase.firestore.ListenerRegistration
import com.msamedcagli.sondeneme.databinding.FragmentSonindirilenlerBinding

class SonindirilenlerFragment : Fragment(), PopupMenu.OnMenuItemClickListener {

    private var listenerRegistration: ListenerRegistration? = null
    private var _binding: FragmentSonindirilenlerBinding? = null
    private val binding get() = _binding!!
    private var adapter: PostAdapter? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Firebase Authentication ve Firestore başlatılır
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    // ViewBinding ile layout bağlanır
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSonindirilenlerBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Arayüz hazır olduğunda işlemler başlatılır
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        getDownloadedImages()
    }

    // RecyclerView kurulumu: Grid düzeni ve adapter bağlama
    private fun setupRecyclerView() {
        adapter = PostAdapter(arrayListOf())
        binding.inRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inRecyclerView.adapter = adapter
    }

    // Firestore'dan isDownloaded = true olan veriler çekilir
    private fun getDownloadedImages() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            listenerRegistration = db.collection("Posts")
                .whereEqualTo("userEmail", currentUser.email)
                .whereEqualTo("isDownloaded", true)
                .addSnapshotListener { value, error ->
                    if (_binding == null) return@addSnapshotListener

                    if (error != null) {
                        Toast.makeText(requireContext(),
                            "Veriler yüklenirken hata oluştu: ${error.message}",
                            Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }

                    val downloadedPosts = ArrayList<Post>()
                    value?.documents?.forEach { document ->
                        val post = Post(
                            userEmail = document.getString("userEmail") ?: "",
                            comment = document.getString("comment") ?: "",
                            downloadUrl = document.getString("downloadUrl") ?: "",
                            isDownloaded = true
                        )
                        downloadedPosts.add(post)
                    }

                    // Liste boşsa bilgilendirme göster
                    if (downloadedPosts.isEmpty()) {
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.inRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyStateText.visibility = View.GONE
                        binding.inRecyclerView.visibility = View.VISIBLE
                    }

                    adapter?.updatePosts(downloadedPosts)
                }
        } else {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış", Toast.LENGTH_LONG).show()
        }
    }

    // Tıklama olayları tanımlanır
    private fun setupClickListeners() {
        // Menü açılır
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }

        // Ana sayfaya geçiş
        binding.AnaSayfaButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToMainPageFragment()
            Navigation.findNavController(it).navigate(action)
        }

        // Gönderi ekleme sayfasına geçiş
        binding.GonderiEkleButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

    // Popup menü gösterilir
    private fun showPopupMenu(view: View) {
        try {
            val popup = PopupMenu(requireContext(), binding.floatingActionButton)
            popup.menuInflater.inflate(R.menu.pop_menu, popup.menu)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Menü açılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Menüden "Çıkış Yap" seçeneği seçildiğinde oturum kapatılır
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        try {
            if (item?.itemId == R.id.cikisYap) {
                auth.signOut()
                val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToKullaniciFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Çıkış yapılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return true
    }

    // ViewBinding temizlenir, listener iptal edilir
    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
