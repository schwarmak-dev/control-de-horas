package com.schwarmakdev.controldehoras.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schwarmakdev.controldehoras.NetworkMonitor
import com.schwarmakdev.controldehoras.NotificationHelper
import com.schwarmakdev.controldehoras.TimerService
import com.schwarmakdev.controldehoras.data.database.AppDatabase
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo
import com.schwarmakdev.controldehoras.data.repository.PreferencesRepository
import com.schwarmakdev.controldehoras.data.repository.TimeTrackerRepository
import com.schwarmakdev.controldehoras.ui.theme.AppThemeColor
import com.schwarmakdev.controldehoras.ui.theme.ThemeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class OverlapConflict(
    val proyectoNombre: String,
    val horaInicioStr: String,
    val horaFinStr: String
)

class TimeTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TimeTrackerRepository(
        database.projectDao(),
        database.timeSessionDao(),
        database.activeTimerDao()
    )
    private val prefs = PreferencesRepository(application)

    // ── Proyectos y sesiones ────────────────────────────────────────────────────

    val projects: StateFlow<List<Proyecto>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SesionTiempo>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTimer: StateFlow<TemporizadorActivo?> = repository.activeTimerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Temporizador UI ─────────────────────────────────────────────────────────

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds: StateFlow<Long> = _timerSeconds.asStateFlow()

    private val _antiOlvidoTriggered = MutableStateFlow(false)
    val antiOlvidoTriggered: StateFlow<Boolean> = _antiOlvidoTriggered.asStateFlow()

    private val _overlapError = MutableStateFlow<String?>(null)
    val overlapError: StateFlow<String?> = _overlapError.asStateFlow()

    // ── Red — detectada automáticamente ────────────────────────────────────────

    val onlineMode: StateFlow<Boolean> = NetworkMonitor.observe(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // ── Notificaciones — persistidas en DataStore ───────────────────────────────

    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationHoursThreshold: StateFlow<Int> = prefs.notificationHoursThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // ── Sesión en edición ────────────────────────────────────────────────────────

    private val _editingSession = MutableStateFlow<SesionTiempo?>(null)
    val editingSession: StateFlow<SesionTiempo?> = _editingSession.asStateFlow()

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            // Restaurar tema desde DataStore al arrancar
            prefs.isDarkMode.first().also    { ThemeState.isDark          = it }
            prefs.colorTheme.first().also    { ThemeState.currentColorTheme = it }
            observeActiveTimer()
        }
    }

    // ── Timer ────────────────────────────────────────────────────────────────────

    private fun observeActiveTimer() {
        viewModelScope.launch {
            repository.activeTimerFlow.collect { timer ->
                if (timer != null && timer.estado == "corriendo") {
                    startTicker(timer)
                    // Mantiene el temporizador vivo en segundo plano + notificación
                    TimerService.start(getApplication())
                } else {
                    stopTicker()
                    _timerSeconds.value = timer?.tiempoAcumuladoAntesPausa ?: 0L
                    // El servicio se auto-detiene al observar la BD, pero lo paramos
                    // de inmediato para liberar la notificación sin demora.
                    TimerService.stop(getApplication())
                }
            }
        }
    }

    private fun startTicker(timer: TemporizadorActivo) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val deltaSecs = (System.currentTimeMillis() - timer.timestampInicio) / 1000
                val totalSecs = timer.tiempoAcumuladoAntesPausa + deltaSecs

                // Anti-olvido a las 12 h: muestra el diálogo (con la app abierta) y
                // pausa. La alerta de notificación y la pausa en segundo plano las
                // gestiona TimerService.
                if (totalSecs >= 43200) {
                    _antiOlvidoTriggered.value = true
                    pauseTimerInternal()
                    break
                }

                _timerSeconds.value = totalSecs
                delay(1000)
            }
        }
    }

    private fun stopTicker() { tickerJob?.cancel(); tickerJob = null }

    // ── Preferencias (persisten en DataStore) ────────────────────────────────────

    fun setDarkMode(isDark: Boolean) {
        ThemeState.isDark = isDark
        viewModelScope.launch { prefs.setDarkMode(isDark) }
    }

    fun setColorTheme(theme: AppThemeColor) {
        ThemeState.currentColorTheme = theme
        viewModelScope.launch { prefs.setColorTheme(theme) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    }

    fun setNotificationHoursThreshold(hours: Int) {
        viewModelScope.launch { prefs.setNotificationHoursThreshold(hours) }
    }

    fun triggerManualNotificationPreview() {
        viewModelScope.launch {
            val timer = repository.getActiveTimer()
            val pr    = if (timer != null) projects.value.find { it.id == timer.proyectoId } else null
            NotificationHelper.sendActiveTimerAlert(
                getApplication(),
                pr?.nombre ?: "Trabajo",
                "de prueba (más de ${notificationHoursThreshold.value} horas)"
            )
        }
    }

    fun dismissAntiOlvido()  { _antiOlvidoTriggered.value = false }
    fun clearOverlapError()  { _overlapError.value = null }

    // ── Acciones del temporizador ────────────────────────────────────────────────
    //
    // El almacenamiento es 100% local (Room), por lo que cada acción se persiste
    // directamente una sola vez. No existe cola de reproducción offline: encolar y
    // ejecutar a la vez duplicaba las sesiones al reconectar.

    fun startTimer(projectId: String) {
        viewModelScope.launch { startTimerInternal(projectId) }
    }

    private suspend fun startTimerInternal(projectId: String) {
        repository.saveActiveTimer(
            TemporizadorActivo(
                proyectoId = projectId,
                timestampInicio = System.currentTimeMillis(),
                estado = "corriendo",
                tiempoAcumuladoAntesPausa = 0L
            )
        )
    }

    fun pauseTimer() {
        viewModelScope.launch { pauseTimerInternal() }
    }

    private suspend fun pauseTimerInternal() {
        val timer = repository.getActiveTimer() ?: return
        if (timer.estado == "corriendo") {
            val delta = (System.currentTimeMillis() - timer.timestampInicio) / 1000
            repository.saveActiveTimer(
                timer.copy(
                    estado = "pausado",
                    tiempoAcumuladoAntesPausa = timer.tiempoAcumuladoAntesPausa + delta,
                    timestampInicio = System.currentTimeMillis()
                )
            )
        }
    }

    fun resumeTimer() {
        viewModelScope.launch { resumeTimerInternal() }
    }

    private suspend fun resumeTimerInternal() {
        val timer = repository.getActiveTimer() ?: return
        if (timer.estado == "pausado") {
            repository.saveActiveTimer(
                timer.copy(estado = "corriendo", timestampInicio = System.currentTimeMillis())
            )
        }
    }

    fun stopAndSaveTimer(notas: String, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onFinished(stopAndSaveTimerInternal(notas)) }
    }

    private suspend fun stopAndSaveTimerInternal(notas: String): Boolean {
        val timer = repository.getActiveTimer() ?: return false
        val delta = if (timer.estado == "corriendo")
            (System.currentTimeMillis() - timer.timestampInicio) / 1000 else 0L
        val totalSecs  = timer.tiempoAcumuladoAntesPausa + delta
        val horaFin    = System.currentTimeMillis()
        val horaInicio = horaFin - totalSecs * 1000

        val overlap = checkOverlap(horaInicio, horaFin, excludeId = null)
        if (overlap != null) {
            _overlapError.value =
                "Conflicto: El temporizador se solapa con '${overlap.proyectoNombre}' de ${overlap.horaInicioStr} a ${overlap.horaFinStr}."
            return false
        }

        repository.insertSession(
            SesionTiempo(
                id = UUID.randomUUID().toString(),
                proyectoId = timer.proyectoId,
                fecha = formatDate(horaInicio),
                horaInicio = horaInicio, horaFin = horaFin,
                duracionMinutos = (totalSecs / 60).toInt().coerceAtLeast(1),
                notas = notas.ifBlank { "Sesión del temporizador" },
                metodoRegistro = "temporizador"
            )
        )
        repository.deleteActiveTimer()
        _overlapError.value = null
        return true
    }

    fun deleteSession(session: SesionTiempo) {
        viewModelScope.launch { repository.deleteSession(session) }
    }

    fun discardTimer() {
        viewModelScope.launch { repository.deleteActiveTimer(); _timerSeconds.value = 0 }
    }

    // ── Edición de sesiones ──────────────────────────────────────────────────────

    fun startEditSession(session: SesionTiempo) { _editingSession.value = session }
    fun cancelEditSession()                      { _editingSession.value = null }

    fun saveEditedSession(
        original: SesionTiempo,
        projectId: String,
        startDateMillis: Long,
        endDateMillis: Long,
        startHour: Int, startMinute: Int,
        endHour: Int,   endMinute: Int,
        notas: String,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val startMillis = buildMillis(startDateMillis, startHour, startMinute)
            val endMillis   = buildMillis(endDateMillis, endHour, endMinute)

            if (endMillis <= startMillis) {
                onComplete("La hora de fin debe ser posterior a la de inicio.")
                return@launch
            }

            val overlap = checkOverlap(startMillis, endMillis, excludeId = original.id)
            if (overlap != null) {
                _overlapError.value =
                    "Conflicto al editar: se solapa con '${overlap.proyectoNombre}'."
                onComplete("Se solapa con '${overlap.proyectoNombre}'.")
                return@launch
            }

            val updated = original.copy(
                proyectoId       = projectId,
                horaInicio       = startMillis,
                horaFin          = endMillis,
                duracionMinutos  = ((endMillis - startMillis) / 60000).toInt().coerceAtLeast(1),
                notas            = notas.ifBlank { original.notas },
                fecha            = formatDate(startMillis)
            )
            repository.deleteSession(original)
            repository.insertSession(updated)
            _editingSession.value = null
            _overlapError.value   = null
            onComplete(null)
        }
    }

    // ── Sesión manual ────────────────────────────────────────────────────────────

    fun addManualSession(
        projectId: String,
        startDateMillis: Long,
        endDateMillis: Long,
        startHour: Int,   startMinute: Int,
        endHour: Int,     endMinute: Int,
        notas: String,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val startMillis = buildMillis(startDateMillis, startHour, startMinute)
            val endMillis   = buildMillis(endDateMillis, endHour, endMinute)

            if (endMillis <= startMillis) {
                onComplete("La hora de fin debe ser posterior a la de inicio.")
                return@launch
            }

            val overlap = checkOverlap(startMillis, endMillis, excludeId = null)
            if (overlap != null) {
                _overlapError.value =
                    "Conflicto: Ya existe actividad en '${overlap.proyectoNombre}' de ${overlap.horaInicioStr} a ${overlap.horaFinStr}."
                onComplete("Se solapa con '${overlap.proyectoNombre}'.")
                return@launch
            }

            val durationMin = ((endMillis - startMillis) / 60000).toInt()

            repository.insertSession(
                SesionTiempo(
                    id = UUID.randomUUID().toString(),
                    proyectoId = projectId,
                    fecha = formatDate(startMillis),
                    horaInicio = startMillis, horaFin = endMillis,
                    duracionMinutos = durationMin,
                    notas = notas.ifBlank { "Registro manual" },
                    metodoRegistro = "manual"
                )
            )
            _overlapError.value = null
            onComplete(null)
        }
    }

    // ── Proyectos ────────────────────────────────────────────────────────────────

    fun addNewProject(nombre: String, descripcion: String, metaHoras: Double) {
        viewModelScope.launch {
            repository.insertProject(
                Proyecto(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre, descripcion = descripcion,
                    horasMetaGlobal = metaHoras, activo = true
                )
            )
        }
    }

    fun toggleProjectActive(project: Proyecto) {
        viewModelScope.launch { repository.updateProject(project.copy(activo = !project.activo)) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * @param excludeId ID de sesión a ignorar (para edición — no comparar consigo misma).
     */
    private suspend fun checkOverlap(
        startMillis: Long,
        endMillis: Long,
        excludeId: String?
    ): OverlapConflict? {
        val projectsMap = projects.value.associateBy { it.id }
        // Leer directamente desde la fuente, sin crear flows extra
        val allSessions = repository.allSessions.first()

        for (session in allSessions) {
            if (session.id == excludeId) continue
            if (startMillis < session.horaFin && endMillis > session.horaInicio) {
                return OverlapConflict(
                    proyectoNombre = projectsMap[session.proyectoId]?.nombre ?: "Desconocido",
                    horaInicioStr  = formatTime(session.horaInicio),
                    horaFinStr     = formatTime(session.horaFin)
                )
            }
        }
        return null
    }

    private fun buildMillis(dateMillis: Long, hour: Int, minute: Int): Long =
        java.util.Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun formatDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))

    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}
