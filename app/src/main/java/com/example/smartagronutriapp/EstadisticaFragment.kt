package com.example.smartagronutriapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*

class EstadisticaFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_estadistica, container, false)

        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)

        // 🔹 Oculta el texto por defecto de "No chart data available"
        barChart.setNoDataText("")
        pieChart.setNoDataText("")

        // 🔹 Referencia a la base de datos correcta
        database = FirebaseDatabase.getInstance()
            .getReference("datos") // 👈 Nodo real

        // 🔹 Escucha cambios en los datos
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // 🔹 Obtiene los valores del nodo "datos"
                    val ce = snapshot.child("conductivity").getValue(Float::class.java) ?: 0f
                    val temperatura = snapshot.child("temperature").getValue(Float::class.java) ?: 0f
                    val luz = snapshot.child("porcentajeLuz").getValue(Float::class.java) ?: 0f

                    // Solo actualiza si hay datos válidos (> 0)
                    if (ce != 0f || temperatura != 0f || luz != 0f) {
                        actualizarGraficos(ce, temperatura, luz)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Error al leer datos
            }
        })

        return view
    }

    private fun actualizarGraficos(ce: Float, temperatura: Float, luz: Float) {
        // 🔹 Gráfico de barras
        val barEntries = ArrayList<BarEntry>().apply {
            add(BarEntry(1f, ce))
            add(BarEntry(2f, temperatura))
            add(BarEntry(3f, luz))
        }

        val barDataSet = BarDataSet(barEntries, "Datos Reales")
        barDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 12f

        val barData = BarData(barDataSet)
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.xAxis.textColor = Color.WHITE
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.textColor = Color.WHITE
        barChart.legend.textColor = Color.WHITE
        barChart.invalidate() // Refresca

        // ---------- 🥧 GRÁFICO CIRCULAR MEJORADO ----------
        val pieEntries = ArrayList<PieEntry>().apply {
            add(PieEntry(ce, "CE"))
            add(PieEntry(temperatura, "Temperatura"))
            add(PieEntry(luz, "Luz"))
        }

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.sliceSpace = 3f
        pieDataSet.selectionShift = 10f
        pieDataSet.colors = listOf(
            Color.rgb(0, 191, 255),   // Azul claro
            Color.rgb(255, 99, 71),   // Rojo coral
            Color.rgb(144, 238, 144)  // Verde suave
        )
        pieDataSet.valueTextColor = Color.WHITE
        pieDataSet.valueTextSize = 14f

        val pieData = PieData(pieDataSet)
        pieData.setValueTextColor(Color.WHITE)
        pieData.setValueTextSize(14f)

        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Datos de Sensores"
        pieChart.setCenterTextColor(Color.WHITE)
        pieChart.setCenterTextSize(16f)
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.holeRadius = 45f
        pieChart.transparentCircleRadius = 50f
        pieChart.description.isEnabled = false
        pieChart.legend.textColor = Color.WHITE
        pieChart.isRotationEnabled = true // ✅ Interactivo
        pieChart.animateY(1500, Easing.EaseInOutQuad)
        pieChart.invalidate()
    }
}
