package com.company.shtopor.vzvod.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class OutboxStatus {
    PENDING,
    IN_FLIGHT,
    FAILED,
    DONE
}

enum class OutboxOpType {
    CREATE_SHIFT,
    UPDATE_SHIFT,
    END_SHIFT,
    ADJUST_IBDR,
    ADJUST_MIGRANT,
    ADJUST_MIGRANT_AND_IBDR,
    ADJUST_STATEMENTS,
    ADJUST_CLAIMS,
    CREATE_ADMIN_VIOLATION,
    CREATE_CRIMINAL_VIOLATION,
    UPDATE_PROFILE,
    CREATE_VOCATION,
    DELETE_VOCATION,
    UPDATE_EDUCATION,
    CREATE_VEHICLE,
    UPDATE_VEHICLE,
    DELETE_VEHICLE
}

@Entity(tableName = "outbox_ops")
data class OutboxOpEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val createdAt: Long,
    val status: String = OutboxStatus.PENDING.name,
    val attempts: Int = 0,
    val lastError: String? = null,
    val clientShiftId: String? = null
)

@Entity(tableName = "cached_shifts")
data class CachedShiftEntity(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: Long,
    val isOpen: Boolean
)

@Entity(tableName = "cached_profile")
data class CachedProfileEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "cached_events")
data class CachedEventEntity(
    @PrimaryKey val id: String,
    val json: String
)

@Entity(tableName = "cached_vacations")
data class CachedVacationsEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
    val updatedAt: Long
)

@Entity(tableName = "local_metrics")
data class LocalMetricsEntity(
    @PrimaryKey val shiftId: String,
    val migrant: Int = 0,
    val ibdr: Int = 0,
    val statements: Int = 0,
    val claims: Int = 0,
    val adminCount: Int = 0,
    val criminalCount: Int = 0,
    val updatedAt: Long
)

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: OutboxOpEntity)

    @Update
    suspend fun update(op: OutboxOpEntity)

    @Query("SELECT * FROM outbox_ops WHERE status IN ('PENDING','FAILED') ORDER BY createdAt ASC")
    suspend fun pending(): List<OutboxOpEntity>

    @Query("SELECT COUNT(*) FROM outbox_ops WHERE status IN ('PENDING','FAILED','IN_FLIGHT')")
    fun pendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_ops WHERE status IN ('PENDING','FAILED','IN_FLIGHT')")
    suspend fun pendingCount(): Int

    @Query(
        "SELECT COUNT(*) FROM outbox_ops WHERE status IN ('PENDING','FAILED','IN_FLIGHT') " +
            "AND clientShiftId = :shiftId"
    )
    suspend fun pendingCountForShift(shiftId: String): Int

    @Query("UPDATE outbox_ops SET status = 'PENDING' WHERE status = 'IN_FLIGHT'")
    suspend fun recoverInFlight()

    @Query(
        "UPDATE outbox_ops SET status = 'PENDING' WHERE status = 'IN_FLIGHT' AND createdAt < :staleBefore"
    )
    suspend fun recoverStaleInFlight(staleBefore: Long)

    @Query("DELETE FROM outbox_ops WHERE status = 'DONE'")
    suspend fun clearDone()

    @Query("UPDATE outbox_ops SET status = 'DONE' WHERE id IN (:ids)")
    suspend fun markDone(ids: List<String>)

    @Query(
        "DELETE FROM outbox_ops WHERE clientShiftId = :shiftId " +
            "AND status IN ('PENDING','FAILED','IN_FLIGHT')"
    )
    suspend fun discardForShift(shiftId: String)

    @Query("SELECT * FROM outbox_ops WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): OutboxOpEntity?
}

@Dao
interface CacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(shift: CachedShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShifts(shifts: List<CachedShiftEntity>)

    @Query("SELECT * FROM cached_shifts ORDER BY updatedAt DESC")
    suspend fun allShifts(): List<CachedShiftEntity>

    @Query("SELECT * FROM cached_shifts WHERE isOpen = 1 LIMIT 1")
    suspend fun openShift(): CachedShiftEntity?

    @Query("SELECT * FROM cached_shifts WHERE id = :id LIMIT 1")
    suspend fun shiftById(id: String): CachedShiftEntity?

    @Query("DELETE FROM cached_shifts WHERE id = :id")
    suspend fun deleteShift(id: String)

    @Query("DELETE FROM local_metrics WHERE shiftId = :shiftId")
    suspend fun deleteLocalMetrics(shiftId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CachedProfileEntity)

    @Query("SELECT * FROM cached_profile WHERE id = 1 LIMIT 1")
    suspend fun profile(): CachedProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<CachedEventEntity>)

    @Query("DELETE FROM cached_events")
    suspend fun clearEvents()

    @Query("SELECT * FROM cached_events")
    suspend fun events(): List<CachedEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVacations(v: CachedVacationsEntity)

    @Query("SELECT * FROM cached_vacations WHERE id = 1 LIMIT 1")
    suspend fun vacations(): CachedVacationsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalMetrics(m: LocalMetricsEntity)

    @Query("SELECT * FROM local_metrics WHERE shiftId = :shiftId LIMIT 1")
    suspend fun localMetrics(shiftId: String): LocalMetricsEntity?
}

@Database(
    entities = [
        OutboxOpEntity::class,
        CachedShiftEntity::class,
        CachedProfileEntity::class,
        CachedEventEntity::class,
        CachedVacationsEntity::class,
        LocalMetricsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun cacheDao(): CacheDao
}
