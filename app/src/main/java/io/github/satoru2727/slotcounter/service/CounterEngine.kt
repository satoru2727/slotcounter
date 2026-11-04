package io.github.satoru2727.slotcounter.service

import android.os.SystemClock
import io.github.satoru2727.slotcounter.core.KeyBinding
import io.github.satoru2727.slotcounter.core.PressGesture
import io.github.satoru2727.slotcounter.core.UNDO_STACK_SIZE
import io.github.satoru2727.slotcounter.data.CounterEntity
import io.github.satoru2727.slotcounter.data.CounterRepository
import io.github.satoru2727.slotcounter.data.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The single writer for count changes. Both the foreground service (volume
 * keys while the screen is off) and the UI (card taps) feed commands into one
 * channel, so the double-click window, the undo stack and the database writes
 * are serialised without locks.
 */
class CounterEngine(
    private val repository: CounterRepository,
    private val haptics: Haptics,
    scope: CoroutineScope,
) {
    private sealed interface Command {
        data class Configure(val session: SessionEntity?, val counters: List<CounterEntity>) : Command
        data class Press(val direction: Int, val at: Long) : Command
        data class Count(val counterId: Long, val name: String) : Command
        data object Undo : Command
    }

    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val gesture = PressGesture()

    private var sessionId: Long? = null
    private var upCounter: CounterEntity? = null
    private var downCounter: CounterEntity? = null
    private val undoStack = ArrayDeque<Pair<Long, String>>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** Name of the count that the undo action would remove, null when empty. */
    private val _lastUndoName = MutableStateFlow<String?>(null)
    val lastUndoName: StateFlow<String?> = _lastUndoName.asStateFlow()

    init {
        scope.launch {
            for (command in commands) {
                when (command) {
                    is Command.Configure -> configureNow(command.session, command.counters)
                    is Command.Press -> handlePress(command)
                    is Command.Count -> countNow(command.counterId, command.name)
                    Command.Undo -> undoNow()
                }
            }
        }
    }

    fun configure(session: SessionEntity?, counters: List<CounterEntity>) {
        commands.trySend(Command.Configure(session, counters))
    }

    fun onVolumeKey(direction: Int) {
        commands.trySend(Command.Press(direction, SystemClock.elapsedRealtime()))
    }

    fun count(counterId: Long, name: String) {
        commands.trySend(Command.Count(counterId, name))
    }

    fun undo() {
        commands.trySend(Command.Undo)
    }

    private fun configureNow(session: SessionEntity?, counters: List<CounterEntity>) {
        if (session?.id != sessionId) {
            undoStack.clear()
            gesture.reset()
            _lastUndoName.value = null
        }
        sessionId = session?.id
        upCounter = counters.firstOrNull { it.keyBinding == KeyBinding.UP }
        downCounter = counters.firstOrNull { it.keyBinding == KeyBinding.DOWN }
        _isRunning.value = session != null && counters.isNotEmpty()
    }

    private suspend fun handlePress(command: Command.Press) {
        when (gesture.onPress(command.direction, command.at)) {
            PressGesture.Action.COUNT_UP -> upCounter?.let { countNow(it.id, it.name) }
            PressGesture.Action.COUNT_DOWN -> downCounter?.let { countNow(it.id, it.name) }
            PressGesture.Action.UNDO -> undoNow()
            PressGesture.Action.IGNORE -> Unit
        }
    }

    private suspend fun countNow(counterId: Long, name: String) {
        haptics.tick()
        repository.increment(counterId)
        undoStack.addLast(counterId to name)
        while (undoStack.size > UNDO_STACK_SIZE) undoStack.removeFirst()
        _lastUndoName.value = name
    }

    private suspend fun undoNow() {
        val entry = undoStack.removeLastOrNull() ?: return
        haptics.undo()
        repository.decrement(entry.first)
        _lastUndoName.value = undoStack.lastOrNull()?.second
    }
}
