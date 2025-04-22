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
    private val binding get() = _binding!!
    //kullanıcı girişi senkronizasyounu için auth tanımlandı
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKullaniciBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.kayitButton.setOnClickListener { kayitOl(it) }
        binding.girisButton.setOnClickListener { girisYap(it) }

        //Daha önce giriş yapıldıysa kullanıcıyı anasayfaya yönlendiriyor
        val guncelKullanici = auth.currentUser
        if(guncelKullanici != null){
            //kullanıcı daha önce giriş yapmışsa buraya girer
            val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }

    fun kayitOl(view: View) {
        //email ve şifre bilgilerini binding ile alarak createuser fonksiyonu için arg getirildi
        val email = binding.emailText.text.toString()
        val sifre = binding.sifreText.text.toString()
        //interneti yormamak için boş olma kontrolü yapıldı
        if(email.isNotEmpty() && sifre.isNotEmpty()){
            //kullanıcı oluşturma tamamlandıysa aşağıdaki işlemler yapılacak
            auth.createUserWithEmailAndPassword(email,sifre).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //aşağıda butona tıkladığımda bir sonraki fragmenta geçecek atamalar yapıldı
                    val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
                    Navigation.findNavController(view).navigate(action)
                }

            }.addOnFailureListener { exception ->
                //hata mesajı gösterildi
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }


    }

    fun girisYap(view: View) {
        val email = binding.emailText.text.toString()
        val sifre = binding.sifreText.text.toString()
        if(email.isNotEmpty() && sifre.isNotEmpty())
        {
            auth.signInWithEmailAndPassword(email,sifre).addOnSuccessListener {
                task ->
                val action = KullaniciFragmentDirections.actionKullaniciFragmentToMainPageFragment()
                Navigation.findNavController(view).navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(),exception.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}