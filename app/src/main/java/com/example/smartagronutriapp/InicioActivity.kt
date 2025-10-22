package com.example.smartagronutriapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.firebase.FirebaseApp

class InicioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        FirebaseApp.initializeApp(this)

        val btnIniciar = findViewById<Button>(R.id.btnIniciar)
        btnIniciar.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
