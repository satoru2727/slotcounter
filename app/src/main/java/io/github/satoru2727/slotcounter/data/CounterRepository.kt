package io.github.satoru2727.slotcounter.data

import androidx.room.withTransaction
import io.github.satoru2727.slotcounter.core.COUNTER_COLOR_COUNT
import io.github.satoru2727.slotcounter.core.CounterPreset
import io.github.satoru2727.slotcounter.core.KeyBinding
import kotlinx.coroutines.flow.Flow

class CounterRepository(private val db: AppDatabase) {

    private val dao = db.dao()

    val activeSession: Flow<SessionEntity?> = dao.observeActiveSession()
    val history: Flow<List<SessionEntity>> = dao.observeHistory()
    val recentMachines: Flow<List<String>> = dao.observeRecentMachines()

    fun counters(sessionId: Long): Flow<List<CounterEntity>> = dao.observeCounters(sessionId)

    fun session(sessionId: Long): Flow<SessionEntity?> = dao.observeSession(sessionId)

    suspend fun getActiveSession(): SessionEntity? = dao.getActiveSession()

    suspend fun getSession(id: Long): SessionEntity? = dao.getSession(id)

    suspend fun getCounters(sessionId: Long): List<CounterEntity> = dao.getCounters(sessionId)

    /**
     * Starts a session with the chosen preset. If a session is somehow still
     * active (double tap, restored state), it is closed first so the database
     * never holds two "running" rows.
     */
    suspend fun startSession(machineName: String, preset: CounterPreset): Long = db.withTransaction {
        dao.getActiveSession()?.let { dao.endSession(it.id, System.currentTimeMillis()) }
        val sessionId = dao.insertSession(
            SessionEntity(
                machineName = machineName.trim(),
                startedAt = System.currentTimeMillis(),
            ),
        )
        dao.insertCounters(
            preset.counterNames.mapIndexed { index, name ->
                CounterEntity(
                    sessionId = sessionId,
                    name = name,
                    keyBinding = when (index) {
                        0 -> KeyBinding.UP
                        1 -> KeyBinding.DOWN
                        else -> KeyBinding.NONE
                    },
                    colorIndex = index % COUNTER_COLOR_COUNT,
                    sortOrder = index,
                )
            },
        )
        sessionId
    }

    suspend fun endSession(id: Long) {
        dao.endSession(id, System.currentTimeMillis())
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSession(id)
    }

    suspend fun setGameCount(id: Long, games: Int) {
        dao.setGameCount(id, games.coerceAtLeast(0))
    }

    suspend fun increment(counterId: Long) {
        dao.increment(counterId)
    }

    suspend fun decrement(counterId: Long) {
        dao.decrement(counterId)
    }

    suspend fun addCounter(sessionId: Long, name: String, keyBinding: Int, colorIndex: Int) {
        db.withTransaction {
            if (keyBinding != KeyBinding.NONE) dao.clearBinding(sessionId, keyBinding)
            val counters = dao.getCounters(sessionId)
            dao.insertCounter(
                CounterEntity(
                    sessionId = sessionId,
                    name = name.trim(),
                    keyBinding = keyBinding,
                    colorIndex = colorIndex,
                    sortOrder = (counters.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                ),
            )
        }
    }

    suspend fun updateCounter(counter: CounterEntity) {
        db.withTransaction {
            if (counter.keyBinding != KeyBinding.NONE) {
                dao.clearBinding(counter.sessionId, counter.keyBinding)
            }
            dao.updateCounter(counter)
        }
    }

    suspend fun setBinding(counter: CounterEntity, binding: Int) {
        db.withTransaction {
            if (binding != KeyBinding.NONE) dao.clearBinding(counter.sessionId, binding)
            dao.setBinding(counter.id, binding)
        }
    }

    suspend fun deleteCounter(counter: CounterEntity) {
        dao.deleteCounter(counter)
    }

    suspend fun moveCounter(sessionId: Long, counterId: Long, delta: Int) {
        val counters = dao.getCounters(sessionId)
        val index = counters.indexOfFirst { it.id == counterId }
        val target = index + delta
        if (index < 0 || target !in counters.indices) return
        db.withTransaction {
            dao.setSortOrder(counters[index].id, counters[target].sortOrder)
            dao.setSortOrder(counters[target].id, counters[index].sortOrder)
        }
    }
}
