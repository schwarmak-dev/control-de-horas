package com.schwarmakdev.controldehoras.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proyectos")
data class Proyecto(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val horasMetaGlobal: Double,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
)
