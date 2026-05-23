package com.schwarmakdev.controldehoras.data.repository

import com.schwarmakdev.controldehoras.data.dao.ActiveTimerDao
import com.schwarmakdev.controldehoras.data.dao.OfflineActionDao
import com.schwarmakdev.controldehoras.data.dao.ProjectDao
import com.schwarmakdev.controldehoras.data.dao.TimeSessionDao
import com.schwarmakdev.controldehoras.data.entity.OfflineAction
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class TimeTrackerRepository(
    private val projectDao: ProjectDao,
    private val timeSessionDao: TimeSessionDao,
    private val activeTimerDao: ActiveTimerDao,
    private val offlineActionDao: OfflineActionDao
) {
    val allProjects: Flow<List<Proyecto>> = projectDao.getAllProjects()
    val allSessions: Flow<List<SesionTiempo>> = timeSessionDao.getAllSessions()
    val activeTimerFlow: Flow<TemporizadorActivo?> = activeTimerDao.getActiveTimerFlow()

    // ──────────────────────────────────────────────
    // Default data bootstrap
    // ──────────────────────────────────────────────
    suspend fun checkAndPrepopulate() {
        val current = projectDao.getAllProjects().first()
        if (current.isEmpty()) {
            listOf(
                Proyecto(
                    id = UUID.randomUUID().toString(),
                    nombre = "Trabajo",
                    descripcion = "Control de horas de mi jornada laboral, reuniones y desarrollo profesional.",
                    horasMetaGlobal = 160.0
                ),
                Proyecto(
                    id = UUID.randomUUID().toString(),
                    nombre = "Universidad",
                    descripcion = "Tiempo dedicado a clases, estudios, exámenes y proyectos académicos.",
                    horasMetaGlobal = 80.0
                ),
                Proyecto(
                    id = UUID.randomUUID().toString(),
                    nombre = "Practica Profesional",
                    descripcion = "Práctica Profesional de Ingeniería en Informática.",
                    horasMetaGlobal = 360.0
                ),
                Proyecto(
                    id = UUID.randomUUID().toString(),
                    nombre = "Otros",
                    descripcion = "Control de tiempo para cualquier otra actividad o tareas complementarias.",
                    horasMetaGlobal = 40.0
                )
            ).forEach { projectDao.insertProject(it) }
        }
    }

    // ──────────────────────────────────────────────
    // Projects
    // ──────────────────────────────────────────────
    suspend fun insertProject(proyecto: Proyecto) = projectDao.insertProject(proyecto)
    suspend fun updateProject(proyecto: Proyecto) = projectDao.updateProject(proyecto)
    suspend fun deleteProject(proyecto: Proyecto) = projectDao.deleteProject(proyecto)

    // ──────────────────────────────────────────────
    // Sessions
    // ──────────────────────────────────────────────
    suspend fun insertSession(session: SesionTiempo) = timeSessionDao.insertSession(session)
    suspend fun deleteSession(session: SesionTiempo) = timeSessionDao.deleteSession(session)
    fun getSessionsForProject(projectId: String): Flow<List<SesionTiempo>> =
        timeSessionDao.getSessionsForProject(projectId)

    // ──────────────────────────────────────────────
    // Active timer
    // ──────────────────────────────────────────────
    suspend fun getActiveTimer(): TemporizadorActivo? = activeTimerDao.getActiveTimer()
    suspend fun saveActiveTimer(timer: TemporizadorActivo) = activeTimerDao.saveActiveTimer(timer)
    suspend fun deleteActiveTimer() = activeTimerDao.deleteActiveTimer()

    // ──────────────────────────────────────────────
    // Offline queue — persisted in Room
    // ──────────────────────────────────────────────
    val offlineActionsFlow: Flow<List<OfflineAction>> = offlineActionDao.getAllActions()

    suspend fun enqueueOfflineAction(action: OfflineAction) =
        offlineActionDao.insertAction(action)

    suspend fun getAllOfflineActions(): List<OfflineAction> =
        offlineActionDao.getAllActionsOnce()

    suspend fun deleteOfflineAction(action: OfflineAction) =
        offlineActionDao.deleteAction(action)

    suspend fun clearOfflineQueue() = offlineActionDao.clearAll()
}
