package com.msamedcagli.sondeneme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import com.msamedcagli.sondeneme.databinding.FragmentMainPageBinding
import com.msamedcagli.sondeneme.viewmodel.MainViewModel
import com.msamedcagli.sondeneme.data.UnsplashPhoto

class MainPageFragment : Fragment(), PopupMenu.OnMenuItemClickListener {
    private lateinit var viewModel: MainViewModel
    private val binding get() = _binding!!
    private var _binding: FragmentMainPageBinding? = null
    private var adapter: PostAdapter? = null
    private var firebasePosts: List<Post> = emptyList()
    private var unsplashPosts: List<Post> = emptyList()

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
        try {
            setupRecyclerView()
            setupClickListeners()
            observeViewModel()
            loadData()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(arrayListOf())
        binding.inRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.inRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.floatingActionButton.setOnClickListener { showPopupMenu(it) }
        binding.GonderiEkleButton.setOnClickListener {
            val action = MainPageFragmentDirections.actionMainPageFragmentToUploadFragment()
            Navigation.findNavController(it).navigate(action)
        }
        binding.AnaSayfaButton.setOnClickListener {
            viewModel.refreshPhotos()
        }
        binding.indirilenlerButton.setOnClickListener {
            val action = MainPageFragmentDirections.actionMainPageFragmentToSonindirilenlerFragment()
            Navigation.findNavController(it).navigate(action)
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            firebasePosts = posts
            updateAdapter()
        }

        viewModel.unsplashPhotos.observe(viewLifecycleOwner) { photos ->
            unsplashPosts = photos.map { photo ->
                Post(
                    userEmail = "Unsplash",
                    comment = photo.description ?: photo.alt_description ?: "Unsplash Photo",
                    downloadUrl = photo.urls.regular
                )
            }
            updateAdapter()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateAdapter() {
        val allPosts = firebasePosts + unsplashPosts
        adapter?.updatePosts(allPosts)
    }

    private fun loadData() {
        viewModel.loadPosts()
        viewModel.loadUnsplashPhotos()
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
                viewModel.signOut()
                val action = MainPageFragmentDirections.actionMainPageFragmentToKullaniciFragment()
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