package com.schwarmakdev.controldehoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.ui.theme.*
import com.schwarmakdev.controldehoras.ui.viewmodel.TimeTrackerViewModel
import java.util.*

@Composable
fun CalendarScreen(
    sessions: List<SesionTiempo>,
    projects: List<Proyecto>,
    viewModel: TimeTrackerViewModel
) {
    val monthNames = listOf(
        "ENERO","FEBRERO","MARZO","ABRIL","MAYO","JUNIO",
        "JULIO","AGOSTO","SEPTIEMBRE","OCTUBRE","NOVIEMBRE","DICIEMBRE"
    )

    val now = Calendar.getInstance()
    var currentYear  by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }

    val daysList = remember(currentYear, currentMonth) { getDaysInMonth(currentYear, currentMonth) }

    var selectedDate          by remember { mutableStateOf<String?>(null) }
    var sessionsForDate        by remember { mutableStateOf<List<SesionTiempo>>(emptyList()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("CALENDARIO MENSUAL DE ACTIVIDAD", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = SecondaryMint, letterSpacing = 1.sp)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Month navigator
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = PrimaryEmerald) }

                        Text("${monthNames[currentMonth]}  $currentYear", fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = TextCrispWhite)

                        IconButton(onClick = {
                            if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
                        }) { Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente", tint = PrimaryEmerald) }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("LUN","MAR","MIÉ","JUE","VIE","SÁ","DOM").forEach { d ->
                            Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSubtleGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Day grid
                    val rowsCount = (daysList.size + 6) / 7
                    for (row in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (col in 0 until 7) {
                                val index = row * 7 + col
                                if (index < daysList.size) {
                                    val dateObj = daysList[index]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f).aspectRatio(1f).padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (dateObj != null) PanelBlue else androidx.compose.ui.graphics.Color.Transparent)
                                            .clickable(enabled = dateObj != null) {
                                                if (dateObj != null) {
                                                    val formatted = formatDateForComparison(dateObj)
                                                    selectedDate      = formatted
                                                    sessionsForDate   = sessions.filter { it.fecha == formatted }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dateObj != null) {
                                            val cal = Calendar.getInstance().apply { time = dateObj }
                                            val formatted = formatDateForComparison(dateObj)
                                            val hasSessions = sessions.any { it.fecha == formatted }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(cal.get(Calendar.DAY_OF_MONTH).toString(),
                                                    color = TextCrispWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                if (hasSessions) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PrimaryEmerald))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(PrimaryEmerald))
                    Text("Los días guardados están aquí", fontSize = 12.sp, color = TextSubtleGray)
                }
            }
        }
    }

    // Day detail dialog
    if (selectedDate != null) {
        Dialog(onDismissRequest = { selectedDate = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sesiones: $selectedDate", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SecondaryMint)
                        IconButton(onClick = { selectedDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextCrispWhite)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (sessionsForDate.isEmpty()) {
                        Text("No se registraron horas en esta fecha.", color = TextSubtleGray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        Box(modifier = Modifier.heightIn(max = 320.dp)) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(sessionsForDate) { session ->
                                    val projName = projects.find { it.id == session.proyectoId }?.nombre ?: "Proyecto"
                                    Card(colors = CardDefaults.cardColors(containerColor = PanelBlue)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(projName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextCrispWhite)
                                                Text("${session.duracionMinutos} min", fontWeight = FontWeight.Bold, color = SecondaryMint, fontSize = 12.sp)
                                            }
                                            Text("Hora: ${formatTimeHM(session.horaInicio)} - ${formatTimeHM(session.horaFin)}", fontSize = 11.sp, color = TextSubtleGray)
                                            if (session.notas.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Nota: ${session.notas}", fontSize = 11.sp, color = TextSubtleGray)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                                                TextButton(
                                                    onClick = { viewModel.startEditSession(session) },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("Editar", color = SecondaryMint, fontSize = 11.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        viewModel.deleteSession(session)
                                                        sessionsForDate = sessionsForDate.filter { it.id != session.id }
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("Eliminar", color = androidx.compose.ui.graphics.Color(0xFFEF4444), fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
