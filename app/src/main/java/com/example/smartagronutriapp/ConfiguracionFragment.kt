package com.example.smartagronutriapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
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
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

class ConfiguracionFragment : Fragment() {

    private lateinit var dbRef: DatabaseReference
    private lateinit var imagenPlanta: ImageView
    private lateinit var tvDiagnostico: TextView
    private lateinit var tvConfianza: TextView
    private lateinit var cardResultado: androidx.cardview.widget.CardView
    private var interpreter: Interpreter? = null

    private val MODEL_NAME = "model_unquant.tflite"

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)

        val tvUser = view.findViewById<TextView>(R.id.tvUserConfig)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnAbrirCamara = view.findViewById<Button>(R.id.btnAbrirCamara)
        imagenPlanta = view.findViewById(R.id.imagenPlanta)

        tvDiagnostico = view.findViewById(R.id.tvDiagnostico)
        tvConfianza = view.findViewById(R.id.tvConfianza)
        cardResultado = view.findViewById(R.id.cardResultado)

        mostrarDatosUsuario(tvUser)
        setupImageClassifier()
        inicializarLaunchers()

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), InicioActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        btnAbrirCamara.setOnClickListener { verificarPermisoYCapturar() }

        tvDiagnostico.text = "Toma una foto para diagnosticar"
        tvConfianza.text = "-- %"

        return view
    }

    private fun mostrarDatosUsuario(tvUser: TextView) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            tvUser.text = "Bienvenido, Usuario"
            return
        }
        dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(user.uid)
        dbRef.get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
            val email = snapshot.child("email").getValue(String::class.java) ?: ""
            tvUser.text = "Bienvenido, $nombre\n$email"
        }.addOnFailureListener {
            tvUser.text = "Bienvenido, Usuario"
        }
    }

    private fun setupImageClassifier() {
        try {
            val modelPath = "model_unquant.tflite"
            val assetFileDescriptor = requireContext().assets.openFd(modelPath)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )

            interpreter = Interpreter(modelBuffer)

            Toast.makeText(
                requireContext(),
                "Modelo cargado ✓",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "ERROR: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun inicializarLaunchers() {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) abrirCamara() else Toast.makeText(requireContext(), "Permiso denegado", Toast.LENGTH_SHORT).show()
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    result.data?.extras?.get("data") as? Bitmap
                }
                bitmap?.let {
                    imagenPlanta.setImageBitmap(it)
                    detectarEnfermedad(it)
                }
            }
        }
    }

    private fun verificarPermisoYCapturar() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            takePictureLauncher.launch(intent)
        }
    }

    private fun detectarEnfermedad(bitmap: Bitmap) {
        if (interpreter == null) {
            Toast.makeText(requireContext(), "Modelo no cargado", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val inputSize = 224
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

            val inputBuffer = java.nio.ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
            inputBuffer.order(java.nio.ByteOrder.nativeOrder())

            val intValues = IntArray(inputSize * inputSize)
            resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

            for (pixelValue in intValues) {
                inputBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
                inputBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
                inputBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
            }

            val numClasses = 6
            val outputBuffer = Array(1) { FloatArray(numClasses) }

            interpreter!!.run(inputBuffer, outputBuffer)

            val probabilities = outputBuffer[0]
            var maxIndex = 0
            var maxProb = probabilities[0]

            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIndex = i
                }
            }

            val diagnostico = when (maxIndex) {
                0 -> "Hoja Sana"
                1 -> "Mildiú Polvoriento"
                2 -> "Roya"
                3 -> "Mancha Bacteriana"
                4 -> "Quemadura de Hoja"
                else -> "Desconocido"
            }

            val confianza = maxProb
            val color = when {
                confianza < 0.5f -> "#faf8f5"
                diagnostico.contains("Sana") -> "#e0f70a"
                else -> "#e0f70a"
            }

            tvDiagnostico.text = diagnostico
            tvConfianza.text = "${(confianza * 100).toInt()}%"
            tvConfianza.setTextColor(android.graphics.Color.parseColor(color))
            cardResultado.setCardBackgroundColor(
                android.graphics.Color.parseColor(color + "20")
            )

        } catch (e: Exception) {
            e.printStackTrace()
            tvDiagnostico.text = "Error al analizar"
            tvConfianza.text = "—"
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        interpreter?.close()
    }
}