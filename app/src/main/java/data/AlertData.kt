package com.example.tpfinal_pablovelazquez.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val date: String,
    val latitude: Double,
    val longitude: Double,
    val location: String,
    val photoPath: String,
    val comment: String = ""
)

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: Alert): Long

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    suspend fun getAllAlerts(): List<Alert>

    @Query("SELECT * FROM alerts WHERE id = :alertId")
    suspend fun getAlertById(alertId: Long): Alert?

    @Update
    suspend fun update(alert: Alert)
}

@Database(entities = [Alert::class], version = 2)
abstract class AlertDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: AlertDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AlertDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlertDatabase::class.java,
                    "alert_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}