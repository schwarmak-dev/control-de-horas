package com.schwarmakdev.controldehoras.data.dao

import androidx.room.*
import com.schwarmakdev.controldehoras.data.entity.OfflineAction
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo.Companion.ACTIVE_TIMER_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM proyectos ORDER BY fechaCreacion DESC")
    fun getAllProjects(): Flow<List<Proyecto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(proyecto: Proyecto)

    @Update
    suspend fun updateProject(proyecto: Proyecto)

    @Delete
    suspend fun deleteProject(proyecto: Proyecto)
}

@Dao
interface TimeSessionDao {
    @Query("SELECT * FROM sesiones_tiempo ORDER BY horaInicio DESC")
    fun getAllSessions(): Flow<List<SesionTiempo>>

    @Query("SELECT * FROM sesiones_tiempo WHERE proyectoId = :proyectoId ORDER BY horaInicio DESC")
    fun getSessionsForProject(proyectoId: String): Flow<List<SesionTiempo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SesionTiempo)

    @Delete
    suspend fun deleteSession(session: SesionTiempo)
}

@Dao
interface ActiveTimerDao {
    @Query("SELECT * FROM temporizador_activo WHERE id = '$ACTIVE_TIMER_ID' LIMIT 1")
    fun getActiveTimerFlow(): Flow<TemporizadorActivo?>

    @Query("SELECT * FROM temporizador_activo WHERE id = '$ACTIVE_TIMER_ID' LIMIT 1")
    suspend fun getActiveTimer(): TemporizadorActivo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveTimer(timer: TemporizadorActivo)

    @Query("DELETE FROM temporizador_activo WHERE id = '$ACTIVE_TIMER_ID'")
    suspend fun deleteActiveTimer()
}

@Dao
interface OfflineActionDao {
    @Query("SELECT * FROM offline_actions ORDER BY timestamp ASC")
    fun getAllActions(): Flow<List<OfflineAction>>

    @Query("SELECT * FROM offline_actions ORDER BY timestamp ASC")
    suspend fun getAllActionsOnce(): List<OfflineAction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: OfflineAction)

    @Delete
    suspend fun deleteAction(action: OfflineAction)

    @Query("DELETE FROM offline_actions")
    suspend fun clearAll()
}
