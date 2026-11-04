package io.github.satoru2727.slotcounter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {

    // Sessions ---------------------------------------------------------------

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Query("SELECT machineName FROM sessions GROUP BY machineName ORDER BY MAX(startedAt) DESC LIMIT 8")
    fun observeRecentMachines(): Flow<List<String>>

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long)

    @Query("UPDATE sessions SET gameCount = :games WHERE id = :id")
    suspend fun setGameCount(id: Long, games: Int)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // Counters ---------------------------------------------------------------

    @Query("SELECT * FROM counters WHERE sessionId = :sessionId ORDER BY sortOrder, id")
    fun observeCounters(sessionId: Long): Flow<List<CounterEntity>>

    @Query("SELECT * FROM counters WHERE sessionId = :sessionId ORDER BY sortOrder, id")
    suspend fun getCounters(sessionId: Long): List<CounterEntity>

    @Insert
    suspend fun insertCounter(counter: CounterEntity): Long

    @Insert
    suspend fun insertCounters(counters: List<CounterEntity>)

    @Update
    suspend fun updateCounter(counter: CounterEntity)

    @Delete
    suspend fun deleteCounter(counter: CounterEntity)

    @Query("UPDATE counters SET count = count + 1 WHERE id = :id")
    suspend fun increment(id: Long)

    @Query("UPDATE counters SET count = CASE WHEN count > 0 THEN count - 1 ELSE 0 END WHERE id = :id")
    suspend fun decrement(id: Long)

    @Query("UPDATE counters SET keyBinding = :binding WHERE id = :id")
    suspend fun setBinding(id: Long, binding: Int)

    @Query("UPDATE counters SET keyBinding = 0 WHERE sessionId = :sessionId AND keyBinding = :binding")
    suspend fun clearBinding(sessionId: Long, binding: Int)

    @Query("UPDATE counters SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)
}
