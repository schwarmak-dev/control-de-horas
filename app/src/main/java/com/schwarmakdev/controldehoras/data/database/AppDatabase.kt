package com.schwarmakdev.controldehoras.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.schwarmakdev.controldehoras.data.dao.ActiveTimerDao
import com.schwarmakdev.controldehoras.data.dao.OfflineActionDao
import com.schwarmakdev.controldehoras.data.dao.ProjectDao
import com.schwarmakdev.controldehoras.data.dao.TimeSessionDao
import com.schwarmakdev.controldehoras.data.entity.OfflineAction
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import com.schwarmakdev.controldehoras.data.entity.TemporizadorActivo

/**
 * Version history:
 *  1 → Base schema (proyectos, sesiones_tiempo, temporizador_activo)
 *  2 → Added offline_actions table for persistent offline queue
 */
@Database(
    entities = [
        Proyecto::class,
        SesionTiempo::class,
        TemporizadorActivo::class,
        OfflineAction::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timeSessionDao(): TimeSessionDao
    abstract fun activeTimerDao(): ActiveTimerDao
    abstract fun offlineActionDao(): OfflineActionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 1→2: adds the offline_actions table.
         * Existing user data (proyectos, sesiones) is NOT touched.
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
