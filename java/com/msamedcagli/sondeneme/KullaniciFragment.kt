package com.msamedcagli.sondeneme

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.msamedcagli.sondeneme.databinding.FragmentKullaniciBinding

class KullaniciFragment : Fragment() {
    private var _binding: FragmentKullaniciBinding? = null
    private val binding get() = _binding!! // ViewBinding nesnesine güvenli erişim

    // Kullanıcı girişi senkronizasyonu için auth tanımlandı
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth // Firebase auth örneği alınır
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // ViewBinding kullanılarak layout bağlanır
        _binding = FragmentKullaniciBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Kayıt ve giriş butonlarına tıklama olayları atanır
        binding.kayitButton.setOnClickListener { kayitOl(it) }
        binding.girisButton.setOnClickListener { girisYap(it) }

        // Eğer daha önce giriş yapılmışsa kullanıcı doğrudan anasayfaya yönlendirilir
        val guncelKullanici = auth.currentUser
        if (guncelKullanici != null) {
            val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    // Kullanıcı kayıt fonksiyonu
    fun kayitOl(view: View) {
        val email = binding.emailText.text.toString()
        val sifre = binding.sifreText.text.toString()

        // Email ve şifre boş değilse işleme devam edilir
        if (email.isNotEmpty() && sifre.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, sifre).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Kayıt başarılıysa anasayfaya geçilir
                    val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
                    Navigation.findNavController(view).navigate(action)
                }
            }.addOnFailureListener { exception ->
                // Hata oluşursa kullanıcıya bildirilir
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Kullanıcı giriş fonksiyonu
    fun girisYap(view: View) {
        val email = binding.emailText.text.toString()
        val sifre = binding.sifreText.text.toString()

        if (email.isNotEmpty() && sifre.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, sifre).addOnSuccessListener {
                val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
                Navigation.findNavController(view).navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Bellek sızıntısını önlemek için binding null yapılır
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
