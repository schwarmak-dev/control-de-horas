package com.schwarmakdev.controldehoras.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwarmakdev.controldehoras.PdfExporter
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.ui.theme.*
import com.schwarmakdev.controldehoras.ui.viewmodel.TimeTrackerViewModel

@Composable
fun ConfigurationScreen(
    projects: List<Proyecto>,
    sessions: List<SesionTiempo>,
    viewModel: TimeTrackerViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationsEnabled       by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationHoursThreshold by viewModel.notificationHoursThreshold.collectAsStateWithLifecycle()

    // Permiso real del sistema (POST_NOTIFICATIONS). En < Android 13 no se requiere.
    fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        else true

    var permissionGranted by remember { mutableStateOf(hasNotificationPermission()) }

    // Reverifica el permiso al volver de los ajustes del sistema (puede cambiar fuera de la app).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionGranted = hasNotificationPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // "Notificaciones activas" solo si el usuario las activó Y el sistema da permiso.
    val effectiveNotificationsEnabled = notificationsEnabled && permissionGranted

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        // El launcher solo se dispara al intentar ACTIVAR, así que conservamos la
        // intención del usuario. Si denegó, el aviso + botón de ajustes lo guiarán
        // (evita la trampa de la denegación permanente que deja el switch inservible).
        viewModel.setNotificationsEnabled(true)
        Toast.makeText(
            context,
            if (isGranted) "¡Permiso concedido!"
            else "Permiso denegado. Actívalo en los ajustes del sistema para recibir alertas.",
            Toast.LENGTH_LONG
        ).show()
    }

    var newProjName             by remember { mutableStateOf("") }
    var newProjDesc             by remember { mutableStateOf("") }
    var newProjGoal             by remember { mutableStateOf("40") }
    var selectedExportProjectId by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Crear proyecto ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("CREAR NUEVO PROYECTO", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppTextField(value = newProjName, label = "Nombre del Proyecto",
                        placeholder   = "Ej. Práctica Profesional Fase II",
                        modifier      = Modifier.fillMaxWidth(),
                        onValueChange = { newProjName = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = newProjDesc, label = "Descripción",
                        placeholder   = "Escribe el objetivo o área...",
                        modifier      = Modifier.fillMaxWidth(),
                        onValueChange = { newProjDesc = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(value = newProjGoal, label = "Meta de Horas Globales",
                        placeholder   = "360",
                        modifier      = Modifier.fillMaxWidth(),
                        onValueChange = { newProjGoal = it })
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth().testTag("create_project_button"),
                        onClick  = {
                            val goal = newProjGoal.toDoubleOrNull() ?: 40.0
                            if (newProjName.isNotBlank() && goal > 0) {
                                viewModel.addNewProject(newProjName, newProjDesc, goal)
                                Toast.makeText(context, "¡Proyecto '$newProjName' creado!", Toast.LENGTH_SHORT).show()
                                newProjName = ""; newProjDesc = ""; newProjGoal = "40"
                            } else {
                                Toast.makeText(context, "Asigna un nombre y meta válidos.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = ButtonContentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crear Proyecto", color = ButtonContentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Lista de proyectos ────────────────────────────────────────────────
        item {
            Text("GESTIÓN DE PROYECTOS (${projects.size})", fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = SecondaryMint,
                modifier   = Modifier.padding(top = 4.dp))
        }

        items(projects) { project ->
            val accMin   = sessions.filter { it.proyectoId == project.id }.sumOf { it.duracionMinutos }
            val progress = if (project.horasMetaGlobal > 0)
                ((accMin / 60.0) / project.horasMetaGlobal * 100).toInt().coerceAtMost(100) else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = if (project.activo) DarkSurface else DarkSurface.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.2f)),
                shape  = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(project.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                color = if (project.activo) TextCrispWhite else TextSubtleGray)
                            if (project.descripcion.isNotBlank())
                                Text(project.descripcion, fontSize = 11.sp, color = TextSubtleGray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (project.activo) "ACTIVO" else "INACTIVO",
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = if (project.activo) PrimaryEmerald else Color(0xFFEF4444))
                            Switch(
                                checked         = project.activo,
                                onCheckedChange = { viewModel.toggleProjectActive(project) },
                                modifier        = Modifier.testTag("active_project_switch_${project.id}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically) {
                        Text("Meta: ${project.horasMetaGlobal.toInt()} h  |  Progreso: $progress%",
                            fontSize = 11.sp, color = TextSubtleGray)
                        LinearProgressIndicator(
                            progress   = { progress / 100f },
                            modifier   = Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color      = if (project.activo) PrimaryEmerald else TextSubtleGray,
                            trackColor = PanelBlue
                        )
                    }
                }
            }
        }

        // ── Exportación CSV + PDF ─────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("EXPORTACIÓN DE HORAS", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Filtra por proyecto y exporta en CSV (Excel) o PDF imprimible.",
                        color = TextSubtleGray, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector proyecto
                    Text("Filtrar Proyecto", fontSize = 11.sp, color = TextSubtleGray)
                    var exportExp by remember { mutableStateOf(false) }
                    val listName  = if (selectedExportProjectId == "ALL") "Todos los Proyectos"
                                    else projects.find { it.id == selectedExportProjectId }?.nombre ?: "Sin nombre"

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { exportExp = true },
                            colors   = ButtonDefaults.buttonColors(containerColor = PanelBlue),
                            shape    = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically) {
                                Text(listName, color = TextCrispWhite)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = exportExp,
                            onDismissRequest = { exportExp = false },
                            modifier         = Modifier.background(DarkSurface)) {
                            DropdownMenuItem(
                                text    = { Text("Todos los Proyectos", color = TextCrispWhite) },
                                onClick = { selectedExportProjectId = "ALL"; exportExp = false }
                            )
                            projects.forEach { p ->
                                DropdownMenuItem(
                                    text    = { Text(p.nombre, color = TextCrispWhite) },
                                    onClick = { selectedExportProjectId = p.id; exportExp = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val toExport = if (selectedExportProjectId == "ALL") sessions
                                   else sessions.filter { it.proyectoId == selectedExportProjectId }

                    // Botón CSV
                    Button(
                        modifier = Modifier.fillMaxWidth().testTag("export_csv_button"),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape    = RoundedCornerShape(12.dp),
                        onClick  = {
                            if (toExport.isEmpty())
                                Toast.makeText(context, "No hay registros para exportar.", Toast.LENGTH_SHORT).show()
                            else
                                shareCSV(context, generateOfficialCSV(toExport, projects))
                        }
                    ) {
                        Icon(Icons.Default.Share, null, tint = TextCrispWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar CSV (Excel)", color = TextCrispWhite, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón PDF
                    Button(
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_button"),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape    = RoundedCornerShape(12.dp),
                        onClick  = {
                            if (toExport.isEmpty())
                                Toast.makeText(context, "No hay registros para exportar.", Toast.LENGTH_SHORT).show()
                            else
                                PdfExporter.generateAndShare(context, toExport, projects)
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = TextCrispWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF (Imprimible)", color = TextCrispWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Notificaciones ────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("notification_settings_card"),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("ALERTAS Y RECORDATORIOS", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Recibe notificaciones si dejas el temporizador corriendo demasiado tiempo.",
                        color = TextSubtleGray, fontSize = 12.sp, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorio anti-olvido", color = TextCrispWhite,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                when {
                                    effectiveNotificationsEnabled              -> "Habilitadas"
                                    notificationsEnabled && !permissionGranted -> "Sin permiso del sistema"
                                    else                                       -> "Desactivadas"
                                },
                                color = if (notificationsEnabled && !permissionGranted) Color(0xFFFBBF24) else TextSubtleGray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked         = effectiveNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    when {
                                        permissionGranted -> viewModel.setNotificationsEnabled(true)
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        else -> viewModel.setNotificationsEnabled(true)
                                    }
                                } else {
                                    viewModel.setNotificationsEnabled(false)
                                }
                            }
                        )
                    }

                    // Aviso cuando el usuario quiere notificaciones pero el sistema las bloquea.
                    if (notificationsEnabled && !permissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Las notificaciones están bloqueadas en los ajustes del sistema. " +
                                "No recibirás recordatorios.",
                                color = TextSubtleGray, fontSize = 11.sp, lineHeight = 15.sp,
                                modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = PanelBlue),
                            shape    = RoundedCornerShape(12.dp),
                            onClick  = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Settings, null, tint = PrimaryEmerald)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir ajustes de notificaciones", color = TextCrispWhite,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (effectiveNotificationsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ContentBorder.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Límite de Alerta: $notificationHoursThreshold horas",
                            color = TextCrispWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Te avisará si la sesión sigue activa más de este tiempo.",
                            color = TextSubtleGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, 5, 8).forEach { hrs ->
                                val selected = notificationHoursThreshold == hrs
                                Button(
                                    modifier       = Modifier.weight(1f),
                                    onClick        = { viewModel.setNotificationHoursThreshold(hrs) },
                                    colors         = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) PrimaryEmerald else PanelBlue,
                                        contentColor   = if (selected) ButtonContentColor else TextCrispWhite
                                    ),
                                    shape          = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) { Text("${hrs}h", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = ButtonDefaults.buttonColors(containerColor = PanelBlue),
                            shape    = RoundedCornerShape(12.dp),
                            onClick  = {
                                viewModel.triggerManualNotificationPreview()
                                Toast.makeText(context, "¡Notificación de prueba enviada!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, tint = PrimaryEmerald)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Alerta de Prueba", color = TextCrispWhite,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── Personalización de tema — ahora persiste ──────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = DarkSurface),
                border   = BorderStroke(1.dp, ContentBorder.copy(alpha = 0.3f)),
                shape    = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("PERSONALIZACIÓN DE TEMA", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = SecondaryMint, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tu elección se guarda automáticamente.",
                        color = TextSubtleGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    val themes = listOf(
                        Triple("Original Menta / Esmeralda", AppThemeColor.MINT_CYAN,     Color(0xFF10B981)),
                        Triple("Morado Imperial",             AppThemeColor.PURPLE,        Color(0xFF8B5CF6)),
                        Triple("Azul Real",                   AppThemeColor.BLUE,          Color(0xFF3B82F6)),
                        Triple("Verde Bosque",                AppThemeColor.FOREST_GREEN,  Color(0xFF22C55E)),
                        Triple("Amarillo Ámbar",              AppThemeColor.YELLOW,        Color(0xFFFBBF24)),
                        Triple("Azul Eléctrico",             AppThemeColor.ELECTRIC_BLUE, Color(0xFF00D2FF))
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        themes.forEach { (label, theme, previewColor) ->
                            val selected = ThemeState.currentColorTheme == theme
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) PanelBlue else Color.Transparent)
                                    .clickable { viewModel.setColorTheme(theme) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(previewColor))
                                    Text(label, color = TextCrispWhite, fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                                if (selected) Icon(Icons.Default.Check, null,
                                    tint = PrimaryEmerald, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
