package com.example.smartagronutriapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConfiguracionFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)

        val tvUser = view.findViewById<TextView>(R.id.tvUserConfig) // Asegúrate de tener este TextView en tu layout
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Leer nombre desde Realtime Database
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    tvUser.text = "Bienvenido, $nombre\n$email"
                }

                override fun onCancelled(error: DatabaseError) {
                    tvUser.text = "Bienvenido, Usuario"
                }
            })
        } else {
            tvUser.text = "Bienvenido, Usuario"
        }

        // Cerrar sesión
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
