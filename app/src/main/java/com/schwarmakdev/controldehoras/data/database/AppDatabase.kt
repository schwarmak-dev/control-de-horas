package com.schwarmakdev.controldehoras.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.schwarmakdev.controldehoras.data.dao.ActiveTimerDao
import com.schwarmakdev.controldehoras.data.dao.ProjectDao
import com.schwarmakdev.controldehoras.data.dao.TimeSessionDao
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo

/**
 * Version history:
 *  1 → Base schema (proyectos, sesiones_tiempo, temporizador_activo)
 *  2 → Added offline_actions table for persistent offline queue
 *  3 → Removed offline_actions (la cola offline duplicaba sesiones; el almacenamiento
 *      es 100% local, así que no se necesita reproducción de acciones)
 */
@Database(
    entities = [
        Proyecto::class,
        SesionTiempo::class,
        TemporizadorActivo::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timeSessionDao(): TimeSessionDao
    abstract fun activeTimerDao(): ActiveTimerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 1→2: añadía la tabla offline_actions.
         * Los datos de usuario (proyectos, sesiones) no se tocan.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `offline_actions` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `type` TEXT NOT NULL,
                        `dataJson` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 2→3: elimina la tabla offline_actions (cola offline retirada).
         * Los datos de usuario (proyectos, sesiones) no se tocan.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `offline_actions`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
