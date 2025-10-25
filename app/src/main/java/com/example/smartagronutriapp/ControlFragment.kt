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
    private lateinit var dbRefDatos: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_control, container, false)
        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvDatos = view.findViewById<TextView>(R.id.tvDatos) // Nuevo TextView para datos en tiempo real

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            // Lectura del usuario (igual que antes)
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

            // Lectura en tiempo real del nodo "datos"
            dbRefDatos = FirebaseDatabase.getInstance().getReference("datos")
            dbRefDatos.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conductivity = snapshot.child("conductivity").getValue(Double::class.java) ?: 0.0
                    val estadoLuz = snapshot.child("estadoLuz").getValue(String::class.java) ?: ""
                    val lightA0 = snapshot.child("lightA0").getValue(Int::class.java) ?: 0
                    val lightDO = snapshot.child("lightDO").getValue(Int::class.java) ?: 0
                    val porcentajeLuz = snapshot.child("porcentajeLuz").getValue(Int::class.java) ?: 0
                    val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    tvDatos.text = """
                        Conductividad: $conductivity
                        Estado Luz: $estadoLuz
                        Light A0: $lightA0
                        Light DO: $lightDO
                        Porcentaje de Luz: $porcentajeLuz
                        Temperatura: $temperature °C
                        Tiempo de uso: $timestamp
                    """.trimIndent()
                }

                override fun onCancelled(error: DatabaseError) {
                    tvDatos.text = "Error al obtener datos: ${error.message}"
                }
            })

        } else {
            tvWelcome.text = "Bienvenido, Usuario"
            tvDatos.text = "No se pudieron cargar datos."
        }

        return view
    }
}
