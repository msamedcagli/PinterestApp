package com.msamedcagli.sondeneme

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.msamedcagli.sondeneme.databinding.FragmentMainPageBinding
import com.msamedcagli.sondeneme.viewmodel.MainViewModel

class MainPageFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentMainPageBinding? = null
    private val binding get() = _binding!!
    private var adapter: PostAdapter? = null
    private var firebasePosts: List<Post> = emptyList()
    private var unsplashPosts: List<Post> = emptyList()

    private var searchHandler: Handler? = null
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadData()
        setupSwipeRefresh()
        setupSearchListener()
    }

    // RecyclerView ayarlamaları
    private fun setupRecyclerView() {
        adapter = PostAdapter(arrayListOf())
        binding.inRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inRecyclerView.adapter = adapter
    }

    // Sayfadaki butonlara tıklama ayarları
    private fun setupClickListeners() {
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }
        binding.GonderiEkleButton.setOnClickListener {
            val action = MainPageFragmentDirections.actionMainPageFragmentToUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }
        binding.indirilenlerButton.setOnClickListener {
            val action = MainPageFragmentDirections.actionMainPageFragmentToSonindirilenlerFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

    // ViewModel'den gelen verileri gözlemleme
    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            firebasePosts = posts
        }
        viewModel.unsplashPhotos.observe(viewLifecycleOwner) { photos ->
            unsplashPosts = photos.map { photo ->
                Post(
                    userEmail = "Unsplash",
                    comment = photo.description ?: photo.alt_description ?: "Unsplash Photo",
                    downloadUrl = photo.urls.regular
                )
            }
            // Arama aktifse ona göre göster, değilse tüm veriyi göster
            val currentQuery = binding.postFilterText.text.toString().trim()
            filterAllPosts(currentQuery)
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }
    }

    // Firebase ve Unsplash verilerini çekme
    private fun loadData() {
        viewModel.loadPosts()
        viewModel.loadUnsplashPhotos()
    }

    // Swipe Refresh ile sayfayı yenileme
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPhotos()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun filterAllPosts(query: String) {
        val filteredFirebasePosts = if (query.isNotEmpty()) {
            firebasePosts.filter { post ->
                post.comment.contains(query, ignoreCase = true)
            }
        } else {
            emptyList()
        }
        val allFilteredPosts = filteredFirebasePosts + unsplashPosts
        adapter?.updatePosts(allFilteredPosts)
    }

    // Arama kutusundaki değişiklikleri dinleyip API'ye sorgu atıp sonuçları filtreleme
    private fun setupSearchListener() {
        searchHandler = Handler(Looper.getMainLooper())
        binding.postFilterText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler?.removeCallbacks(it) }
                searchRunnable = Runnable {
                    val query = s.toString().trim()
                    if (query.isNotEmpty()) {
                        viewModel.searchUnsplashPhotos(query)  // Unsplash API'den yeni resimler çek
                        filterAllPosts(query)                  // + Firebase içinden filtrele ve göster
                    } else {
                        loadData()  // Arama boşsa her şeyi yeniden yükle
                    }
                }
                searchHandler?.postDelayed(searchRunnable!!, 300)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Sayfa üzerindeki popup menüyü gösterir (çıkış vs. işlemleri için)
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), binding.floatingActionButton)
        val inflater = popup.menuInflater
        popup.menuInflater.inflate(R.menu.pop_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    // Popup menüdeki item'lara tıklama işlemleri
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.cikisYap) {
            viewModel.signOut()
            val action = MainPageFragmentDirections.actionMainPageFragmentToKullaniciFragment()
            Navigation.findNavController(requireView()).navigate(action)
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
