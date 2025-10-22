package com.example.smartagronutriapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ControlActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        auth = FirebaseAuth.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val name = intent.getStringExtra("nombre") ?: "Usuario"
        tvWelcome.text = "Bienvenido, $name"

        // Botón Cerrar Sesión
        btnLogout.setOnClickListener {
            auth.signOut() // Cierra sesión en Firebase
            // Volver a InicioActivity
            val intent = Intent(this, InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
