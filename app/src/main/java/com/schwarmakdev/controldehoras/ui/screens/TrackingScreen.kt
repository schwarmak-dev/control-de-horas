package com.schwarmakdev.controldehoras.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo
import com.schwarmakdev.controldehoras.ui.theme.*
import com.schwarmakdev.controldehoras.ui.viewmodel.TimeTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    projects: List<Proyecto>,
    activeTimer: TemporizadorActivo?,
    timerSeconds: Long,
    viewModel: TimeTrackerViewModel,
    onStopClick: () -> Unit
) {
    val context        = LocalContext.current
    val activeProjects = projects.filter { it.activo }

    var selectedProjectForTimerId by remember { mutableStateOf("") }
    if (selectedProjectForTimerId.isEmpty() && activeProjects.isNotEmpty())
        selectedProjectForTimerId = activeProjects.first().id

    var manualProjectSelectedId by remember { mutableStateOf("") }
    if (manualProjectSelectedId.isEmpty() && activeProjects.isNotEmpty())
        manualProjectSelectedId = activeProjects.first().id

    // ── Estado fecha/hora con pickers ─────────────────────────────────────────
    val now = Calendar.getInstance()
    var selectedStartDateMillis by remember { mutableLongStateOf(now.timeInMillis) }
    var selectedEndDateMillis   by remember { mutableLongStateOf(now.timeInMillis) }
    var startHour  by remember { mutableIntStateOf(9)  }
    var startMin   by remember { mutableIntStateOf(0)  }
    var endHour    by remember { mutableIntStateOf(17) }
    var endMin     by remember { mutableIntStateOf(0)  }
    var manualNotes by remember { mutableStateOf("") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }
    var isSecurityAlertVisible by remember { mutableStateOf(true) }

    // Formatters para mostrar la selección
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm",      Locale.getDefault())
    fun fmtTime(h: Int, m: Int) = String.format(Locale.getDefault(), "%02d:%02d", h, m)

    // ── DatePicker inicio ─────────────────────────────────────────────────────
    if (showStartDatePicker) {
        AppDatePickerDialog(
            selectedDateMillis = selectedStartDateMillis,
            onDateSelected = { startMillis ->
                selectedStartDateMillis = startMillis
                selectedEndDateMillis = startMillis
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // ── DatePicker fin ────────────────────────────────────────────────────────
    if (showEndDatePicker) {
        AppDatePickerDialog(
            selectedDateMillis = selectedEndDateMillis,
            onDateSelected = { endMillis ->
                selectedEndDateMillis = endMillis
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    // ── TimePicker inicio ─────────────────────────────────────────────────────
    if (showStartTimePicker) {
        val tpState = rememberTimePickerState(initialHour = startHour, initialMinute = startMin)
        TimePickerDialog(
            title  = "Hora de inicio",
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startHour = tpState.hour
                startMin = tpState.minute
                showStartTimePicker = false
                showEndTimePicker = true
            }
        ) { TimePicker(state = tpState) }
    }

    // ── TimePicker fin ────────────────────────────────────────────────────────
    if (showEndTimePicker) {
        val tpState = rememberTimePickerState(initialHour = endHour, initialMinute = endMin)
        TimePickerDialog(
            title  = "Hora de fin",
            onDismiss = { showEndTimePicker = false },
            onConfirm = { endHour = tpState.hour; endMin = tpState.minute; showEndTimePicker = false }
        ) { TimePicker(state = tpState) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Timer card ────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TEMPORIZADOR ACTIVO", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeTimer == null) {
                        Text("Selecciona un proyecto para iniciar",
                            color = TextSubtleGray, fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp))

                        if (activeProjects.isEmpty()) {
                            Text("No hay proyectos activos. Crea uno en la pestaña Proyectos.",
                                color = Color(0xFFF9A825), fontSize = 12.sp,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
                        } else {
                            ProjectDropdown(
                                projects   = activeProjects,
                                selectedId = selectedProjectForTimerId,
                                onSelect   = { selectedProjectForTimerId = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(
                            modifier = Modifier.size(84.dp).clip(CircleShape)
                                .background(PrimaryEmerald).testTag("start_timer_button"),
                            onClick  = {
                                if (selectedProjectForTimerId.isNotEmpty()) {
                                    viewModel.startTimer(selectedProjectForTimerId)
                                } else {
                                    Toast.makeText(context, "Selecciona un proyecto.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = activeProjects.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar",
                                tint = ButtonContentColor, modifier = Modifier.size(44.dp))
                        }
                    } else {
                        val projectName = projects.find { it.id == activeTimer.proyectoId }?.nombre ?: "Sin nombre"
                        Text(projectName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextCrispWhite)

                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isRunning = activeTimer.estado == "corriendo"
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                .background(if (isRunning) PrimaryEmerald else Color(0xFFFBBF24)))
                            Text(activeTimer.estado.uppercase(Locale.getDefault()),
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (isRunning) PrimaryEmerald else Color(0xFFFBBF24))
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(formatSecondsToHMS(timerSeconds), fontSize = 44.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                            color = SecondaryMint)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (activeTimer.estado == "corriendo") {
                                Button(onClick = { viewModel.pauseTimer() },
                                    modifier = Modifier.testTag("pause_timer_button"),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                                    shape    = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.Pause, null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pausar", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(onClick = { viewModel.resumeTimer() },
                                    modifier = Modifier.testTag("resume_timer_button"),
                                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                    shape    = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.PlayArrow, null, tint = ButtonContentColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reanudar", color = ButtonContentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(onClick = onStopClick,
                                modifier = Modifier.testTag("stop_timer_button"),
                                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                                shape    = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Save, null, tint = ButtonContentColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar", color = ButtonContentColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.discardTimer() }) {
                            Text("Descartar sesión", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Anti-Olvido alert ─────────────────────────────────────────────────
        if (isSecurityAlertVisible) {
            item {
                Card(modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightAlertBackground),
                    border = BorderStroke(1.dp, AlertBorder), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top) {
                        Text("⚠️", fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Precaución:", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (ThemeState.isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Los temporizadores que superen las 12 horas se suspenderán automáticamente.",
                                fontSize = 11.sp, lineHeight = 15.sp,
                                color = if (ThemeState.isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B))
                        }
                        IconButton(onClick = { isSecurityAlertVisible = false },
                            modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null,
                                tint = if (ThemeState.isDark) Color(0xFFFECACA) else Color(0xFF7F1D1D),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ── Manual session card ───────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape  = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("AÑADIR SESIÓN RETROACTIVA (MANUAL)", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeProjects.isEmpty()) {
                        Text("Por favor, define proyectos activos en la pestaña Proyectos.",
                            color = TextSubtleGray, fontSize = 13.sp)
                    } else {
                        Text("Proyecto", fontSize = 12.sp, color = TextSubtleGray)
                        ProjectDropdown(
                            projects   = activeProjects,
                            selectedId = manualProjectSelectedId,
                            onSelect   = { manualProjectSelectedId = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Fecha inicio / fin ──────────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fecha inicio", fontSize = 12.sp, color = TextSubtleGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick  = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextCrispWhite)
                                ) {
                                    Icon(Icons.Default.CalendarToday, null,
                                        tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dateFmt.format(Date(selectedStartDateMillis)),
                                        fontWeight = FontWeight.SemiBold, color = TextCrispWhite, fontSize = 12.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fecha fin", fontSize = 12.sp, color = TextSubtleGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick  = { showEndDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextCrispWhite)
                                ) {
                                    Icon(Icons.Default.CalendarToday, null,
                                        tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(dateFmt.format(Date(selectedEndDateMillis)),
                                        fontWeight = FontWeight.SemiBold, color = TextCrispWhite, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Hora inicio / fin ──────────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora inicio", fontSize = 12.sp, color = TextSubtleGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick  = { showStartTimePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextCrispWhite)
                                ) {
                                    Icon(Icons.Default.Schedule, null,
                                        tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fmtTime(startHour, startMin),
                                        fontWeight = FontWeight.Bold, color = SecondaryMint)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hora fin", fontSize = 12.sp, color = TextSubtleGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick  = { showEndTimePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextCrispWhite)
                                ) {
                                    Icon(Icons.Default.Schedule, null,
                                        tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fmtTime(endHour, endMin),
                                        fontWeight = FontWeight.Bold, color = SecondaryMint)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value         = manualNotes,
                            label         = "Actividad desarrollada / Notas",
                            placeholder   = "Describa brevemente la tarea...",
                            modifier      = Modifier.fillMaxWidth(),
                            onValueChange = { manualNotes = it }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth().testTag("save_manual_button"),
                            onClick  = {
                                viewModel.addManualSession(
                                    projectId         = manualProjectSelectedId,
                                    startDateMillis   = selectedStartDateMillis,
                                    endDateMillis     = selectedEndDateMillis,
                                    startHour         = startHour,
                                    startMinute       = startMin,
                                    endHour           = endHour,
                                    endMinute         = endMin,
                                    notas             = manualNotes
                                ) { error ->
                                    if (error != null)
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    else {
                                        Toast.makeText(context, "¡Sesión guardada!", Toast.LENGTH_SHORT).show()
                                        manualNotes = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                        ) {
                            Icon(Icons.Default.Add, null, tint = ButtonContentColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Guardar Sesión Manual",
                                color = ButtonContentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── TimePickerDialog wrapper (Material 3 no lo incluye por defecto) ──────────

@Composable
fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title, color = TextCrispWhite, fontWeight = FontWeight.Bold) },
        text             = { content() },
        confirmButton    = {
            TextButton(onClick = onConfirm) {
                Text("Aceptar", color = PrimaryEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSubtleGray)
            }
        },
        containerColor = DarkSurface
    )
}

// ── DatePickerDialog wrapper ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialDateUtc = remember(selectedDateMillis) {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(java.util.Calendar.YEAR, cal.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH, cal.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, cal.get(java.util.Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { utcMillis ->
                    val calUtc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                    val localCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, calUtc.get(java.util.Calendar.YEAR))
                        set(java.util.Calendar.MONTH, calUtc.get(java.util.Calendar.MONTH))
                        set(java.util.Calendar.DAY_OF_MONTH, calUtc.get(java.util.Calendar.DAY_OF_MONTH))
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onDateSelected(localCal.timeInMillis)
                }
                onDismiss()
            }) { Text("Aceptar", color = PrimaryEmerald) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSubtleGray)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = DarkSurface)
    ) {
        DatePicker(state = datePickerState)
    }
}

// ── Edit session dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionDialog(
    session: SesionTiempo,
    projects: List<Proyecto>,
    viewModel: TimeTrackerViewModel
) {
    val context    = LocalContext.current

    val initCal = Calendar.getInstance().apply { timeInMillis = session.horaInicio }
    val endCal  = Calendar.getInstance().apply { timeInMillis = session.horaFin   }

    var editProjectId       by remember { mutableStateOf(session.proyectoId) }
    var editStartDateMillis by remember { mutableLongStateOf(session.horaInicio) }
    var editEndDateMillis   by remember { mutableLongStateOf(session.horaFin) }
    var editHourStart       by remember { mutableIntStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var editMinStart        by remember { mutableIntStateOf(initCal.get(Calendar.MINUTE)) }
    var editHourEnd         by remember { mutableIntStateOf(endCal.get(Calendar.HOUR_OF_DAY)) }
    var editMinEnd          by remember { mutableIntStateOf(endCal.get(Calendar.MINUTE)) }
    var editNotes           by remember { mutableStateOf(session.notas) }
    
    var showStartDP         by remember { mutableStateOf(false) }
    var showEndDP           by remember { mutableStateOf(false) }
    var showStartTP         by remember { mutableStateOf(false) }
    var showEndTP           by remember { mutableStateOf(false) }

    if (showStartDP) {
        AppDatePickerDialog(
            selectedDateMillis = editStartDateMillis,
            onDateSelected = { startMillis ->
                editStartDateMillis = startMillis
                editEndDateMillis = startMillis
            },
            onDismiss = { showStartDP = false }
        )
    }
    if (showEndDP) {
        AppDatePickerDialog(
            selectedDateMillis = editEndDateMillis,
            onDateSelected = { endMillis ->
                editEndDateMillis = endMillis
            },
            onDismiss = { showEndDP = false }
        )
    }
    if (showStartTP) {
        val tpState = rememberTimePickerState(editHourStart, editMinStart)
        TimePickerDialog("Hora inicio", { showStartTP = false },
            {
                editHourStart = tpState.hour
                editMinStart = tpState.minute
                showStartTP = false
                showEndTP = true
            }
        ) { TimePicker(state = tpState) }
    }
    if (showEndTP) {
        val tpState = rememberTimePickerState(editHourEnd, editMinEnd)
        TimePickerDialog("Hora fin", { showEndTP = false },
            { editHourEnd = tpState.hour; editMinEnd = tpState.minute; showEndTP = false }
        ) { TimePicker(state = tpState) }
    }

    AlertDialog(
        onDismissRequest = { viewModel.cancelEditSession() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, null, tint = PrimaryEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Sesión", color = TextCrispWhite, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Proyecto", fontSize = 12.sp, color = TextSubtleGray)
                ProjectDropdown(
                    projects   = projects,
                    selectedId = editProjectId,
                    onSelect   = { editProjectId = it }
                )

                Spacer(modifier = Modifier.height(2.dp))

                val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fecha inicio", fontSize = 11.sp, color = TextSubtleGray)
                        OutlinedButton(onClick = { showStartDP = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors()) {
                            Text(dateFmt.format(Date(editStartDateMillis)),
                                color = TextCrispWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fecha fin", fontSize = 11.sp, color = TextSubtleGray)
                        OutlinedButton(onClick = { showEndDP = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors()) {
                            Text(dateFmt.format(Date(editEndDateMillis)),
                                color = TextCrispWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hora inicio", fontSize = 11.sp, color = TextSubtleGray)
                        OutlinedButton(onClick = { showStartTP = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors()) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", editHourStart, editMinStart),
                                color = SecondaryMint, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hora fin", fontSize = 11.sp, color = TextSubtleGray)
                        OutlinedButton(onClick = { showEndTP = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors()) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", editHourEnd, editMinEnd),
                                color = SecondaryMint, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value         = editNotes,
                    onValueChange = { editNotes = it },
                    label         = { Text("Notas") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryEmerald,
                        unfocusedBorderColor = ContentBorder.copy(alpha = 0.5f),
                        focusedTextColor     = TextCrispWhite,
                        unfocusedTextColor   = TextCrispWhite,
                        focusedLabelColor    = PrimaryEmerald,
                        unfocusedLabelColor  = TextSubtleGray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveEditedSession(
                        original         = session,
                        projectId        = editProjectId,
                        startDateMillis  = editStartDateMillis,
                        endDateMillis    = editEndDateMillis,
                        startHour        = editHourStart, startMinute = editMinStart,
                        endHour          = editHourEnd,   endMinute   = editMinEnd,
                        notas            = editNotes
                    ) { error ->
                        if (error != null)
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(context, "Sesión actualizada", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape  = RoundedCornerShape(10.dp)
            ) { Text("Guardar cambios", color = ButtonContentColor, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.cancelEditSession() }) {
                Text("Cancelar", color = TextSubtleGray)
            }
        },
        containerColor = DarkSurface
    )
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
fun ProjectDropdown(projects: List<Proyecto>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = projects.find { it.id == selectedId }?.nombre ?: "Seleccionar proyecto"
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true },
            colors   = ButtonDefaults.buttonColors(containerColor = PanelBlue),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = TextCrispWhite)
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            projects.forEach { p ->
                DropdownMenuItem(
                    text    = { Text(p.nombre, color = TextCrispWhite) },
                    onClick = { onSelect(p.id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String, label: String,
    modifier: Modifier = Modifier, placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label       = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder) }) else null,
        modifier    = modifier, shape = RoundedCornerShape(12.dp),
        colors      = OutlinedTextFieldDefaults.colors(
            focusedBorderColor        = PrimaryEmerald,
            unfocusedBorderColor      = ContentBorder.copy(alpha = 0.5f),
            focusedTextColor          = TextCrispWhite,
            unfocusedTextColor        = TextCrispWhite,
            focusedLabelColor         = PrimaryEmerald,
            unfocusedLabelColor       = TextSubtleGray,
            focusedPlaceholderColor   = TextSubtleGray,
            unfocusedPlaceholderColor = TextSubtleGray
        )
    )
}
