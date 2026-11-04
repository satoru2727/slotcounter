package io.github.satoru2727.slotcounter.ui

import android.content.Context
import io.github.satoru2727.slotcounter.data.AppDatabase
import io.github.satoru2727.slotcounter.data.CounterEntity
import io.github.satoru2727.slotcounter.data.CounterRepository
import io.github.satoru2727.slotcounter.data.SessionEntity
import io.github.satoru2727.slotcounter.data.SettingsStore
import io.github.satoru2727.slotcounter.service.CounterEngine
import io.github.satoru2727.slotcounter.service.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Manual dependency injection: one process-scoped container owns the database,
 * the engine and the settings cache. The foreground service looks the same
 * instance up from the Application, which is what keeps UI taps and volume-key
 * presses on the same undo stack.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val repository: CounterRepository by lazy { CounterRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val haptics: Haptics by lazy { Haptics(appContext) }
    val engine: CounterEngine by lazy { CounterEngine(repository, haptics, scope) }

    /** Read by the service when the screen turns off; kept in sync below. */
    @Volatile
    var silentAudioEnabled: Boolean = true
        private set

    /** True only while the counting screen is on top; the activity consults it. */
    @Volatile
    var countingVisible: Boolean = false

    init {
        scope.launch {
            settings.flow.collect { current ->
                haptics.enabled = current.haptics
                silentAudioEnabled = current.silentAudio
            }
        }
        scope.launch {
            repository.activeSession
                .flatMapLatest { session ->
                    if (session == null) {
                        flowOf<Pair<SessionEntity?, List<CounterEntity>>>(null to emptyList())
                    } else {
                        repository.counters(session.id).map { counters -> session to counters }
                    }
                }
                .collect { (session, counters) -> engine.configure(session, counters) }
        }
    }
}
