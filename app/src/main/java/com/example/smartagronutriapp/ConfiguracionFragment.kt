package com.example.smartagronutriapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ConfiguracionFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var imagenPlanta: ImageView
    private var imageClassifier: ImageClassifier? = null
    private var imageBitmap: Bitmap? = null

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)

        val tvUser = view.findViewById<TextView>(R.id.tvUserConfig)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnAbrirCamara = view.findViewById<Button>(R.id.btnAbrirCamara)
        imagenPlanta = view.findViewById(R.id.imagenPlanta)

        mostrarDatosUsuario(tvUser)
        inicializarModelo()
        inicializarLaunchers()

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), InicioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnAbrirCamara.setOnClickListener {
            verificarPermisoYCapturar()
        }

        return view
    }

    private fun mostrarDatosUsuario(tvUser: TextView) {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("nombre").value as? String ?: "Usuario"
                    val email = snapshot.child("email").value as? String ?: ""
                    tvUser.text = "Bienvenido, $nombre\n$email"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error al obtener datos: ${error.message}")
                    tvUser.text = "Bienvenido, Usuario"
                }
            })
        } else {
            tvUser.text = "Bienvenido, Usuario"
        }
    }

    // 🔧 Carga correcta del modelo TFLite desde assets
    private fun inicializarModelo() {
        try {
            val modelName = "model.tflite" // asegúrate que el nombre coincida

            // TensorFlow Lite Task API requiere que el archivo esté en un File, no directamente desde assets
            val file = File(requireContext().filesDir, modelName)
            if (!file.exists()) {
                requireContext().assets.open(modelName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)
                .build()

            imageClassifier = ImageClassifier.createFromFileAndOptions(file, options)

            Toast.makeText(requireContext(), "✅ Modelo cargado correctamente", Toast.LENGTH_SHORT).show()
            Log.i("TFLite", "Modelo cargado desde ${file.absolutePath}")

        } catch (e: IOException) {
            Log.e("TFLite", "Error al cargar modelo: ${e.message}", e)
            Toast.makeText(requireContext(), "❌ No se pudo cargar el modelo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("TFLite", "Error inesperado al inicializar modelo: ${e.message}", e)
            Toast.makeText(requireContext(), "⚠️ Error inesperado al inicializar modelo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inicializarLaunchers() {
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) abrirCamara()
                else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val bitmap = result.data?.extras?.getParcelable("data", Bitmap::class.java)
                    if (bitmap != null) {
                        imageBitmap = bitmap
                        imagenPlanta.setImageBitmap(bitmap)
                        detectarEnfermedad(bitmap)
                    } else {
                        Toast.makeText(requireContext(), "No se pudo obtener la imagen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No se tomó ninguna foto", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun verificarPermisoYCapturar() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> abrirCamara()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(requireContext(), "Se necesita permiso para usar la cámara", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            takePictureLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun detectarEnfermedad(bitmap: Bitmap) {
        val clasificador = imageClassifier ?: run {
            Toast.makeText(requireContext(), "Modelo no cargado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val image = TensorImage.fromBitmap(bitmap)
            val resultados: List<Classifications> = clasificador.classify(image)

            if (resultados.isEmpty() || resultados[0].categories.isEmpty()) {
                Toast.makeText(requireContext(), "No se detectó ninguna enfermedad", Toast.LENGTH_SHORT).show()
                return
            }

            val mejorCategoria = resultados[0].categories.maxByOrNull { it.score }
            val nombre = mejorCategoria?.label ?: "Desconocido"
            val confianza = mejorCategoria?.score ?: 0f

            Toast.makeText(
                requireContext(),
                "🌿 Enfermedad detectada: $nombre\nConfianza: ${String.format("%.2f", confianza * 100)}%",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("TFLite", "Error al analizar imagen: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al analizar la imagen", Toast.LENGTH_SHORT).show()
        }
    }
}
