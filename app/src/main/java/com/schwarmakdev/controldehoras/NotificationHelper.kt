package com.schwarmakdev.controldehoras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationHelper {
    private const val CHANNEL_ID   = "time_tracker_alerts"
    private const val CHANNEL_NAME = "Recordatorios de Registro"
    private const val NOTIFICATION_ID = 5005

    // Canal y notificación persistente del temporizador en ejecución (foreground service)
    const val RUNNING_CHANNEL_ID  = "time_tracker_running"
    private const val RUNNING_CHANNEL_NAME = "Temporizador en ejecución"
    const val ONGOING_NOTIFICATION_ID = 5006

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerta silenciosa para control o prevención de olvido de temporizadores"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    /** Canal silencioso (IMPORTANCE_LOW) para la notificación continua del cronómetro. */
    fun createRunningChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RUNNING_CHANNEL_ID,
                RUNNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el tiempo del temporizador mientras está activo"
                setShowBadge(false)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(context: Context): PendingIntent {
        // minSdk 26 ≥ M, por lo que FLAG_IMMUTABLE siempre está disponible.
        return PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Notificación persistente con cronómetro nativo. El sistema actualiza el conteo
     * automáticamente a partir de [baseTimeMillis] (= instante en que el contador
     * marcaría 00:00), sin necesidad de refrescarla cada segundo.
     */
    fun buildRunningNotification(
        context: Context,
        projectName: String,
        baseTimeMillis: Long
    ): android.app.Notification {
        createRunningChannel(context)
        return androidx.core.app.NotificationCompat.Builder(context, RUNNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cronometrando: $projectName")
            .setContentText("Temporizador activo. Toca para abrir la app.")
            .setWhen(baseTimeMillis)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    fun sendActiveTimerAlert(context: Context, projectName: String, hoursText: String) {
        createNotificationChannel(context)

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Sigues trabajando en $projectName?")
            .setContentText("Tu temporizador lleva activo $hoursText. Recuerda apagarlo si ya terminaste.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }
}
