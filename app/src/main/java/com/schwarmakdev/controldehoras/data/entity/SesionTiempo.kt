package com.schwarmakdev.controldehoras.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sesiones_tiempo")
data class SesionTiempo(
    @PrimaryKey val id: String,
    val proyectoId: String,
    val fecha: String, // Formato "yyyy-MM-dd"
    val horaInicio: Long, // millis
    val horaFin: Long, // millis
    val duracionMinutos: Int, // Calculado
    val notas: String,
    val metodoRegistro: String // 'manual' o 'temporizador'
)
