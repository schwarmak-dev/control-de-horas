package com.schwarmakdev.controldehoras.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.ui.theme.*
import com.schwarmakdev.controldehoras.ui.viewmodel.TimeTrackerViewModel

@Composable
fun DashboardScreen(
    projects: List<Proyecto>,
    sessions: List<SesionTiempo>,
    viewModel: TimeTrackerViewModel
) {
    var selectedReportProjectId by remember { mutableStateOf("ALL") }

    val filteredSessions = if (selectedReportProjectId == "ALL") sessions
    else sessions.filter { it.proyectoId == selectedReportProjectId }

    val totalMinutes   = filteredSessions.sumOf { it.duracionMinutos }
    val activeDays     = filteredSessions.map { it.fecha }.distinct().size
    val totalGoalHours = if (selectedReportProjectId == "ALL") projects.sumOf { it.horasMetaGlobal }
                         else projects.find { it.id == selectedReportProjectId }?.horasMetaGlobal ?: 0.0

    val progressPercentage = if (totalGoalHours > 0.0)
        ((totalMinutes / 60.0) / totalGoalHours * 100).toInt().coerceAtMost(100) else 0

    val remainingMinutes = if (totalGoalHours > 0.0)
        ((totalGoalHours * 60).toInt() - totalMinutes).coerceAtLeast(0) else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Filter selector ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("FILTRAR DASHBOARD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryMint)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val currentName = if (selectedReportProjectId == "ALL") "Todos los Proyectos"
                    else projects.find { it.id == selectedReportProjectId }?.nombre ?: "Proyecto Desconocido"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick  = { expanded = true },
                            colors   = ButtonDefaults.buttonColors(containerColor = PanelBlue),
                            shape    = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(currentName, color = TextCrispWhite)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(DarkSurface)) {
                            DropdownMenuItem(text = { Text("Todos los Proyectos", color = TextCrispWhite) },
                                onClick = { selectedReportProjectId = "ALL"; expanded = false })
                            projects.forEach { p ->
                                DropdownMenuItem(text = { Text(p.nombre, color = TextCrispWhite) },
                                    onClick = { selectedReportProjectId = p.id; expanded = false })
                            }
                        }
                    }
                }
            }
        }

        // ── KPI row ──
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard("TOTAL ACUMULADO", formatMinutesToHoursMinutes(totalMinutes), PrimaryEmerald, Modifier.weight(1.2f))
                KpiCard("DÍAS ACTIVOS", "$activeDays días", SecondaryMint, Modifier.weight(1f))
            }
        }

        // ── Progress bar ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progreso hacia tu meta", fontSize = 12.sp, color = TextSubtleGray)
                        Text("$progressPercentage%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress    = { progressPercentage / 100f },
                        modifier    = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(4.dp)),
                        color       = PrimaryEmerald,
                        trackColor  = PanelBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Meta: ${totalGoalHours.toInt()} h", fontSize = 11.sp, color = TextSubtleGray)
                        Text(
                            text       = if (remainingMinutes > 0) "Restante: ${formatMinutesToHoursMinutes(remainingMinutes)}" else "¡Meta Cumplida! 🎉",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (remainingMinutes > 0) SecondaryMint else Color(0xFFFFD700)
                        )
                    }
                }
            }
        }

        // ── Donut chart ──
        item { ProjectTimeDonutChart(projects = projects, sessions = sessions) }

        // ── Session history ──
        item {
            Text("HISTORIAL DE SESIONES (${filteredSessions.size})", fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = SecondaryMint, modifier = Modifier.padding(vertical = 4.dp))
        }

        if (filteredSessions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Text("Aún no se registran sesiones.", textAlign = TextAlign.Center, color = TextSubtleGray,
                        modifier = Modifier.fillMaxWidth().padding(24.dp), fontSize = 13.sp)
                }
            }
        } else {
            items(filteredSessions) { session ->
                val pName = projects.find { it.id == session.proyectoId }?.nombre ?: "Proyecto Desconocido"
                SessionCard(
                    session     = session,
                    projectName = pName,
                    onDelete    = { viewModel.deleteSession(session) },
                    onEdit      = { viewModel.startEditSession(session) }
                )
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = DarkSurface),
        border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.2f)),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = TextSubtleGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun SessionCard(
    session: SesionTiempo,
    projectName: String,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = DarkSurface),
        border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(projectName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextCrispWhite)
                    Text("${session.fecha}  |  ${formatTimeHM(session.horaInicio)} - ${formatTimeHM(session.horaFin)}",
                        fontSize = 11.sp, color = TextSubtleGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(PanelBlue).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${session.duracionMinutos} min", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryMint)
                    }
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = SecondaryMint, modifier = Modifier.size(17.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (session.notas.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Nota: ${session.notas}", fontSize = 12.sp, color = TextSubtleGray, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (session.metodoRegistro == "temporizador") Icons.Default.Timer else Icons.Default.Edit,
                    contentDescription = session.metodoRegistro, tint = PrimaryEmerald, modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (session.metodoRegistro == "temporizador") "Por Temporizador síncrono" else "Registro Manual",
                    fontSize = 9.sp, color = PrimaryEmerald, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
