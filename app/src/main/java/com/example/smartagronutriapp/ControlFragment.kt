package com.example.smartagronutriapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ControlFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_control, container, false)
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                    tvWelcome.text = "Bienvenido, $nombre"
                }

                override fun onCancelled(error: DatabaseError) {
                    tvWelcome.text = "Bienvenido, Usuario"
                }
            })
        } else {
            tvWelcome.text = "Bienvenido, Usuario"
        }

        return view
    }
}
