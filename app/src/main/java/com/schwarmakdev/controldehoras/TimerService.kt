package com.schwarmakdev.controldehoras

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.schwarmakdev.controldehoras.data.database.AppDatabase
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo
import com.schwarmakdev.controldehoras.data.repository.PreferencesRepository
import com.schwarmakdev.controldehoras.data.repository.TimeTrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service que mantiene vivo el temporizador aunque la app se cierre.
 *
 * Responsabilidades cuando el temporizador está "corriendo":
 *  - Muestra una notificación persistente con el tiempo en vivo (cronómetro nativo).
 *  - Envía la alerta de umbral (N horas) configurada por el usuario.
 *  - Aplica la regla anti-olvido a las 12 h: pausa el temporizador en la BD.
 *
 * Se auto-detiene en cuanto el temporizador deja de estar "corriendo"
 * (pausado, guardado o descartado), observando directamente la BD.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: TimeTrackerRepository
    private lateinit var prefs: PreferencesRepository

    private var tickJob: Job? = null
    private var alertSent = false
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TimeTrackerRepository(
            db.projectDao(),
            db.timeSessionDao(),
            db.activeTimerDao()
        )
        prefs = PreferencesRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!observing) {
            // Primer arranque: hay que llamar a startForeground en <5 s. Se publica
            // un marcador y el observador lo reemplaza con los datos reales.
            startForegroundCompat(
                NotificationHelper.buildRunningNotification(
                    this, "Trabajo", System.currentTimeMillis()
                )
            )
            observing = true
            observeTimer()
        }
        return START_STICKY
    }

    private fun observeTimer() {
        scope.launch {
            repository.activeTimerFlow.collect { timer ->
                if (timer != null && timer.estado == "corriendo") {
                    onRunning(timer)
                } else {
                    stopSelfSafely()
                }
            }
        }
    }

    private suspend fun onRunning(timer: TemporizadorActivo) {
        val projectName = repository.allProjects.first()
            .find { it.id == timer.proyectoId }?.nombre ?: "Trabajo"

        val elapsedSecs = timer.tiempoAcumuladoAntesPausa +
            (System.currentTimeMillis() - timer.timestampInicio) / 1000
        val baseWhen = System.currentTimeMillis() - elapsedSecs * 1000

        // Notificación con cronómetro nativo (el sistema actualiza el conteo solo).
        startForegroundCompat(
            NotificationHelper.buildRunningNotification(this, projectName, baseWhen)
        )

        startTickLoop()
    }

    /** Bucle ligero: vigila el umbral de alerta y la regla anti-olvido (12 h). */
    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val t = repository.getActiveTimer()
                if (t == null || t.estado != "corriendo") break

                val totalSecs = t.tiempoAcumuladoAntesPausa +
                    (System.currentTimeMillis() - t.timestampInicio) / 1000

                // Anti-olvido: a las 12 h se pausa automáticamente en la BD.
                if (totalSecs >= 43200) {
                    repository.saveActiveTimer(
                        t.copy(
                            estado = "pausado",
                            tiempoAcumuladoAntesPausa = totalSecs,
                            timestampInicio = System.currentTimeMillis()
                        )
                    )
                    val name = repository.allProjects.first()
                        .find { it.id == t.proyectoId }?.nombre ?: "Trabajo"
                    NotificationHelper.sendActiveTimerAlert(
                        applicationContext, name,
                        "12 horas y se detuvo automáticamente"
                    )
                    break // la BD emitirá "pausado" y el servicio se detendrá
                }

                // Alerta de umbral configurable (una sola vez por sesión).
                val enabled = prefs.notificationsEnabled.first()
                val thresholdSecs = prefs.notificationHoursThreshold.first() * 3600L
                if (enabled && totalSecs >= thresholdSecs && !alertSent) {
                    alertSent = true
                    val name = repository.allProjects.first()
                        .find { it.id == t.proyectoId }?.nombre ?: "Trabajo"
                    val hrs = (thresholdSecs / 3600).toInt()
                    NotificationHelper.sendActiveTimerAlert(
                        applicationContext, name, "$hrs horas"
                    )
                }

                delay(1000)
            }
        }
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun stopSelfSafely() {
        tickJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, TimerService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
