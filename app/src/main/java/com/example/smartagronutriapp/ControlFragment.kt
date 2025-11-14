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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_control, container, false)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvTemperatura = view.findViewById<TextView>(R.id.tvTemperatura)
        val tvLuzAmbiental = view.findViewById<TextView>(R.id.tvLuzAmbiental)
        val tvConductividad = view.findViewById<TextView>(R.id.tvConductividad)
        val tvModoOperacion = view.findViewById<TextView>(R.id.tvModoOperacion)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {

            // 🔹 Referencia al nodo del usuario
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

            // 🔹 Lectura en tiempo real del nodo "datos"
            dbRefDatos = FirebaseDatabase.getInstance().getReference("datos")
            dbRefDatos.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val conductivity = snapshot.child("conductivity").getValue(Double::class.java) ?: 0.0
                    val estadoLuz = snapshot.child("estadoLuz").getValue(String::class.java) ?: ""
                    val porcentajeLuz = snapshot.child("porcentajeLuz").getValue(Int::class.java) ?: 0
                    val temperature = snapshot.child("temperature").getValue(Double::class.java) ?: 0.0

                    // 🔹 Mostrar los datos en los cuadros individuales
                    tvTemperatura.text = "$temperature °C"
                    tvLuzAmbiental.text = "$porcentajeLuz %"
                    tvConductividad.text = "$conductivity mS/cm"
                    tvModoOperacion.text = estadoLuz
                }

                override fun onCancelled(error: DatabaseError) {
                    tvTemperatura.text = "-"
                    tvLuzAmbiental.text = "-"
                    tvConductividad.text = "-"
                    tvModoOperacion.text = "Error al obtener datos"
                }
            })

        } else {
            // 🔹 Si no hay usuario autenticado
            tvWelcome.text = "Bienvenido, Usuario"
            tvTemperatura.text = "-"
            tvLuzAmbiental.text = "-"
            tvConductividad.text = "-"
            tvModoOperacion.text = "-"
        }

        return view
    }
}
