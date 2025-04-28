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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSonindirilenlerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        getDownloadedImages()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(arrayListOf())
        binding.inRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inRecyclerView.adapter = adapter
    }

    private fun getDownloadedImages() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Posts")
                .whereEqualTo("userEmail", currentUser.email)
                .whereEqualTo("isDownloaded", true)
                .get()
                .addOnSuccessListener { documents ->
                    val downloadedPosts = ArrayList<Post>()
                    for (document in documents) {
                        val downloadUrl = document.getString("downloadUrl") ?: ""
                        val comment = document.getString("comment") ?: ""
                        val userEmail = document.getString("userEmail") ?: ""
                        val post = Post(downloadUrl, comment, userEmail)
                        downloadedPosts.add(post)
                    }
                    adapter?.updatePosts(downloadedPosts)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Veriler yüklenirken hata oluştu: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupClickListeners() {
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }
        binding.AnaSayfaButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToMainPageFragment()
            Navigation.findNavController(it).navigate(action)
        }
        binding.GonderiEkleButton.setOnClickListener {
            val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

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

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        try {
            if (item?.itemId == R.id.cikisYap) {
                val action = SonindirilenlerFragmentDirections.actionSonindirilenlerFragmentToKullaniciFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Çıkış yapılırken hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}