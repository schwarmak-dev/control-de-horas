package com.schwarmakdev.controldehoras.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temporizador_activo")
data class TemporizadorActivo(
    @PrimaryKey val id: String = ACTIVE_TIMER_ID,
    val proyectoId: String,
    val timestampInicio: Long, // millis
    val estado: String, // "corriendo" o "pausado"
    val tiempoAcumuladoAntesPausa: Long // en segundos
) {
    companion object {
        const val ACTIVE_TIMER_ID = "active_timer_id"
    }
}
