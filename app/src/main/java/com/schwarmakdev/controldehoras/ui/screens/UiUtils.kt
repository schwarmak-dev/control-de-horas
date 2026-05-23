package com.schwarmakdev.controldehoras.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ──────────────────────────────────────────────
// Formatters
// ──────────────────────────────────────────────

fun formatSecondsToHMS(seconds: Long): String {
    val hrs  = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
}

fun formatMinutesToHoursMinutes(minutes: Int): String {
    val hrs  = minutes / 60
    val mins = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d h", hrs, mins)
}

fun formatTimeHM(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

fun formatDateForComparison(date: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

fun getDaysInMonth(year: Int, month: Int): List<Date?> {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayRaw = calendar.get(Calendar.DAY_OF_WEEK)
    val firstDay = if (firstDayRaw == Calendar.SUNDAY) 7 else firstDayRaw - 1

    return buildList {
        repeat(firstDay - 1) { add(null) }
        for (i in 1..maxDays) {
            add(Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, i)
            }.time)
        }
    }
}

fun generateOfficialCSV(sessionsList: List<SesionTiempo>, projectsList: List<Proyecto>): String {
    val projMap = projectsList.associateBy { it.id }
    return buildString {
        append("HOJA DE ASISTENCIA Y REPORTES DE PRÁCTICA OFICIAL\n")
        append("Generado el: ${SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        append("------------------------------------------\n\n")
        append("ID Sesión,Fecha,Proyecto,Hora Inicio,Hora Fin,Duración (Minutos),Método Registro,Notas / Actividad\n")
        for (sess in sessionsList) {
            val pName      = projMap[sess.proyectoId]?.nombre ?: "Desconocido"
            val startFmt   = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sess.horaInicio))
            val endFmt     = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sess.horaFin))
            val cleanNotes = sess.notas.replace(",", ";").replace("\"", "'")
            append("${sess.id},${sess.fecha},\"$pName\",$startFmt,$endFmt,${sess.duracionMinutos},${sess.metodoRegistro},\"$cleanNotes\"\n")
        }
        append("\n------------------------------------------\n")
        append("Firma del Estudiante / Consultor: _________________________\n")
        append("Firma del Supervisor de Práctica / Líder de Proyecto: _________________________\n")
    }
}

fun shareCSV(context: android.content.Context, csvContent: String) {
    try {
        val intent = android.content.Intent().apply {
            action  = android.content.Intent.ACTION_SEND
            type    = "text/csv"
            putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
            putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Hoja_Asistencia_Práctica_Oficial_${System.currentTimeMillis()}.csv")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Exportar Reporte de Horas"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al compartir: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ──────────────────────────────────────────────
// Shared composable — Donut chart
// ──────────────────────────────────────────────

// Paleta fija de 10 colores — se asigna por el índice del proyecto en la lista,
// no por su nombre. Así cualquier proyecto creado por el usuario tiene color único.
private val PROJECT_PALETTE = listOf(
    Color(0xFF10B981), // esmeralda
    Color(0xFF3B82F6), // azul
    Color(0xFFF59E0B), // ámbar
    Color(0xFF8B5CF6), // morado
    Color(0xFFEC4899), // rosa
    Color(0xFF14B8A6), // teal
    Color(0xFFF97316), // naranja
    Color(0xFF06B6D4), // cyan
    Color(0xFF84CC16), // lima
    Color(0xFFA855F7)  // violeta
)

fun projectColor(projectId: String, allProjects: List<com.schwarmakdev.controldehoras.data.entity.Proyecto>): Color {
    val index = allProjects.indexOfFirst { it.id == projectId }
    return PROJECT_PALETTE[if (index < 0) 0 else index % PROJECT_PALETTE.size]
}

@Composable
fun ProjectTimeDonutChart(
    projects: List<Proyecto>,
    sessions: List<SesionTiempo>
) {
    val totalMinutes     = sessions.sumOf { it.duracionMinutos }
    val minutesByProject = sessions.groupBy { it.proyectoId }
        .mapValues { (_, s) -> s.sumOf { it.duracionMinutos } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_time_donut_chart"),
        colors  = CardDefaults.cardColors(containerColor = DarkSurface),
        border  = androidx.compose.foundation.BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
        shape   = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text       = "DISTRIBUCIÓN DE TIEMPO REGISTRADO",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = SecondaryMint,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (totalMinutes == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier        = Modifier.size(100.dp).padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color      = ContentBorder.copy(alpha = 0.15f),
                                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                style      = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text("0 h", color = TextSubtleGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sin registro de horas", color = TextCrispWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completa tu primer registro para ver la distribución visual de tu tiempo acumulado.",
                            color = TextSubtleGray, fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Box(
                        modifier        = Modifier.size(110.dp).padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            minutesByProject.forEach { (projId, mins) ->
                                val name        = projects.find { it.id == projId }?.nombre ?: "Otros"
                                val sweepAngle  = (mins.toFloat() / totalMinutes) * 360f
                                if (sweepAngle > 0f) {
                                    drawArc(
                                        color      = projectColor(projId, projects),
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter  = false,
                                        style      = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = String.format(Locale.US, "%.1f h", totalMinutes / 60.0),
                                color      = TextCrispWhite,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("TOTAL", color = TextSubtleGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        minutesByProject.forEach { (projId, mins) ->
                            val name  = projects.find { it.id == projId }?.nombre ?: "Otros"
                            val color = projectColor(projId, projects)
                            val pct   = (mins.toFloat() / totalMinutes * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Column {
                                    Text(name, color = TextCrispWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text(
                                        "$pct% • ${mins / 60}h ${mins % 60}m",
                                        color = TextSubtleGray, fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
