package com.schwarmakdev.controldehoras

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.schwarmakdev.controldehoras.ui.screens.*
import com.schwarmakdev.controldehoras.ui.theme.*
import com.schwarmakdev.controldehoras.ui.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier             = Modifier.fillMaxSize(),
                    containerColor       = DarkBackground,
                    contentWindowInsets  = WindowInsets(0)
                ) { innerPadding ->
                    TimeTrackerApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TimeTrackerApp(
    modifier: Modifier = Modifier,
    viewModel: TimeTrackerViewModel = viewModel()
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    val projects         by viewModel.projects.collectAsStateWithLifecycle()
    val sessions         by viewModel.sessions.collectAsStateWithLifecycle()
    val activeTimer      by viewModel.activeTimer.collectAsStateWithLifecycle()
    val timerSeconds     by viewModel.timerSeconds.collectAsStateWithLifecycle()
    val onlineMode       by viewModel.onlineMode.collectAsStateWithLifecycle()   // automático
    val antiOlvido       by viewModel.antiOlvidoTriggered.collectAsStateWithLifecycle()
    val overlapError     by viewModel.overlapError.collectAsStateWithLifecycle()
    val editingSession   by viewModel.editingSession.collectAsStateWithLifecycle()

    var showStopDialog by remember { mutableStateOf(false) }
    var stopNotes      by remember { mutableStateOf("") }

    // Banner de conexión:
    //  - Sin conexión  → visible de forma permanente.
    //  - Reconexión    → muestra "En línea ✓" durante 3,5 s y se oculta solo.
    var showBanner by remember { mutableStateOf(false) }
    var prevOnline by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(onlineMode) {
        val changed = prevOnline != null && prevOnline != onlineMode
        prevOnline = onlineMode
        when {
            !onlineMode -> showBanner = true            // offline: siempre visible
            changed     -> {                            // volvió la conexión
                showBanner = true
                delay(3500)
                showBanner = false
            }
        }
    }

    Column(modifier = modifier
        .fillMaxSize()
        .background(DarkBackground)
        .statusBarsPadding()
    ) {

        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (selectedTab) {
                        0 -> "Seguimiento"; 1 -> "Estadísticas"; 2 -> "Calendario"; else -> "Proyectos"
                    },
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Black,
                    color      = TextCrispWhite
                )

                // Banner automático de estado de red
                if (showBanner) {
                    val msg   = if (onlineMode) "En línea ✓" else "Sin conexión"
                    val bg    = if (onlineMode) Color(0xFF0D9488).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                    val color = if (onlineMode) SecondaryMint else Color(0xFFFCA5A5)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(msg, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = color)
                    }
                }
            }

            // Toggle modo oscuro — persiste automáticamente
            IconButton(onClick = { viewModel.setDarkMode(!ThemeState.isDark) }) {
                Icon(
                    imageVector        = if (ThemeState.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar tema",
                    tint               = if (ThemeState.isDark) Color(0xFFFFD54F) else PrimaryEmerald,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }

        // ── Tab bar ────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = DarkBackground,
            contentColor     = PrimaryEmerald,
            indicator        = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color    = PrimaryEmerald
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("Seguimiento", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Timer, null) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("Dashboard",    fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Dashboard, null) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                text = { Text("Calendario",   fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.CalendarMonth, null) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                text = { Text("Proyectos",    fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Settings, null) })
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ── Contenido ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState  = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label        = "TabContent"
            ) { tab ->
                when (tab) {
                    0 -> TrackingScreen(
                        projects     = projects,
                        activeTimer  = activeTimer,
                        timerSeconds = timerSeconds,
                        viewModel    = viewModel,
                        onStopClick  = { stopNotes = ""; showStopDialog = true }
                    )
                    1 -> DashboardScreen(projects = projects, sessions = sessions, viewModel = viewModel)
                    2 -> CalendarScreen(sessions = sessions, projects = projects, viewModel = viewModel)
                    3 -> ConfigurationScreen(projects = projects, sessions = sessions, viewModel = viewModel)
                }
            }
        }
    }

    // ── Stop-timer dialog ──────────────────────────────────────────────────────
    if (showStopDialog) {
        StopTimerDialog(
            projectName  = projects.find { it.id == activeTimer?.proyectoId }?.nombre ?: "Desconocido",
            timerSeconds = timerSeconds,
            notes        = stopNotes,
            onNotesChange = { stopNotes = it },
            onConfirm    = {
                viewModel.stopAndSaveTimer(stopNotes) { success ->
                    if (success) showStopDialog = false
                }
            },
            onDismiss = { showStopDialog = false }
        )
    }

    // ── Edit session dialog ───────────────────────────────────────────────────
    if (editingSession != null) {
        EditSessionDialog(
            session  = editingSession!!,
            projects = projects,
            viewModel = viewModel
        )
    }

    // ── Overlap error dialog ───────────────────────────────────────────────────
    if (!overlapError.isNullOrEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearOverlapError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conflicto de horario", color = Color(0xFFEF4444))
                }
            },
            text           = { Text(overlapError ?: "", color = TextCrispWhite, fontSize = 14.sp) },
            confirmButton  = {
                Button(onClick = { viewModel.clearOverlapError() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                    Text("Entendido", color = TextCrispWhite)
                }
            },
            containerColor = DarkSurface
        )
    }

    // ── Anti-Olvido dialog ─────────────────────────────────────────────────────
    if (antiOlvido) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAntiOlvido() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFBBF24))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regla Anti-Olvido (12h+)", color = Color(0xFFFBBF24))
                }
            },
            text = {
                Text(
                    "El temporizador superó las 12 horas activo sin interrupción. " +
                    "Se detuvo automáticamente para mantener la fidelidad del registro.\n\n" +
                    "Por favor, guarda o descarta la sesión.",
                    color = TextCrispWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAntiOlvido(); showStopDialog = true },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24))
                ) { Text("Verificar y Guardar", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.dismissAntiOlvido()
                    viewModel.discardTimer()
                    Toast.makeText(context, "Temporizador descartado", Toast.LENGTH_SHORT).show()
                }) { Text("Descartar", color = Color(0xFFEF4444)) }
            },
            containerColor = DarkSurface
        )
    }
}

// ── Stop-timer dialog ──────────────────────────────────────────────────────────
@Composable
private fun StopTimerDialog(
    projectName: String,
    timerSeconds: Long,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text("Guardar Sesión", color = TextCrispWhite) },
        text   = {
            Column {
                Text("Proyecto: $projectName", color = SecondaryMint, fontWeight = FontWeight.Bold)
                Text("Tiempo: ${formatSecondsToHMS(timerSeconds)}",
                    fontSize = 14.sp, color = TextSubtleGray,
                    modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value         = notes,
                    onValueChange = onNotesChange,
                    label         = { Text("Notas de Actividad") },
                    placeholder   = { Text("¿Qué hiciste en esta sesión?") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor        = PrimaryEmerald,
                        unfocusedBorderColor      = ContentBorder,
                        focusedTextColor          = TextCrispWhite,
                        unfocusedTextColor        = TextCrispWhite,
                        focusedLabelColor         = PrimaryEmerald,
                        unfocusedLabelColor       = TextSubtleGray,
                        focusedPlaceholderColor   = TextSubtleGray,
                        unfocusedPlaceholderColor = TextSubtleGray
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                shape  = RoundedCornerShape(12.dp)) {
                Text("Detener y Guardar", color = ButtonContentColor)
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
