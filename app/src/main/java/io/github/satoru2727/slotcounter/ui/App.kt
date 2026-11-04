package io.github.satoru2727.slotcounter.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.satoru2727.slotcounter.R
import io.github.satoru2727.slotcounter.data.AppSettings
import io.github.satoru2727.slotcounter.service.CountingService
import io.github.satoru2727.slotcounter.ui.screens.CountingScreen
import io.github.satoru2727.slotcounter.ui.screens.EditCountersScreen
import io.github.satoru2727.slotcounter.ui.screens.HistoryDetailScreen
import io.github.satoru2727.slotcounter.ui.screens.HistoryScreen
import io.github.satoru2727.slotcounter.ui.screens.SettingsScreen
import io.github.satoru2727.slotcounter.ui.screens.StartScreen
import io.github.satoru2727.slotcounter.ui.theme.SlotCounterTheme
import kotlinx.coroutines.launch

@Composable
fun SlotCounterApp(container: AppContainer) {
    SlotCounterTheme {
        val active by container.repository.activeSession.collectAsState(initial = null)
        val history by container.repository.history.collectAsState(initial = emptyList())
        val recentMachines by container.repository.recentMachines.collectAsState(initial = emptyList())
        val settings by container.settings.flow.collectAsState(initial = AppSettings())
        val lastUndoName by container.engine.lastUndoName.collectAsState()

        var stackText by rememberSaveable { mutableStateOf("home") }
        val stack = remember(stackText) { stackText.split("|") }
        val current = stack.last()

        fun push(route: String) {
            stackText = "$stackText|$route"
        }

        fun pop() {
            if (stack.size > 1) stackText = stack.dropLast(1).joinToString("|")
        }

        BackHandler(enabled = stack.size > 1) { pop() }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(current, active?.id) {
            container.countingVisible = current == "home" && active != null
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                current == "edit" -> {
                    val session = active
                    if (session == null) {
                        LaunchedEffect(Unit) { pop() }
                    } else {
                        val counters by container.repository.counters(session.id)
                            .collectAsState(initial = emptyList())
                        EditCountersScreen(
                            counters = counters,
                            onBack = { pop() },
                            onAdd = { name, binding, color ->
                                scope.launch {
                                    container.repository.addCounter(session.id, name, binding, color)
                                }
                            },
                            onUpdate = { counter ->
                                scope.launch { container.repository.updateCounter(counter) }
                            },
                            onDelete = { counter ->
                                scope.launch { container.repository.deleteCounter(counter) }
                            },
                            onMove = { counter, delta ->
                                scope.launch {
                                    container.repository.moveCounter(session.id, counter.id, delta)
                                }
                            },
                        )
                    }
                }

                current == "history" -> {
                    HistoryScreen(
                        sessions = history,
                        onOpen = { push("detail:$it") },
                        onBack = { pop() },
                    )
                }

                current.startsWith("detail:") -> {
                    val id = current.substringAfter(':').toLongOrNull()
                    val session = history.firstOrNull { it.id == id }
                    if (id == null || session == null) {
                        LaunchedEffect(id) { pop() }
                    } else {
                        val counters by container.repository.counters(id)
                            .collectAsState(initial = emptyList())
                        HistoryDetailScreen(
                            session = session,
                            counters = counters,
                            onBack = { pop() },
                            onDelete = {
                                scope.launch {
                                    container.repository.deleteSession(id)
                                    pop()
                                }
                            },
                        )
                    }
                }

                current == "settings" -> {
                    SettingsScreen(
                        settings = settings,
                        notificationGranted = hasNotificationPermission(context),
                        onHaptics = { enabled -> scope.launch { container.settings.setHaptics(enabled) } },
                        onSilentAudio = { enabled -> scope.launch { container.settings.setSilentAudio(enabled) } },
                        onBack = { pop() },
                    )
                }

                else -> {
                    val session = active
                    if (session == null) {
                        StartScreen(
                            recentMachines = recentMachines,
                            onStart = { name, preset ->
                                scope.launch {
                                    container.repository.startSession(name, preset)
                                    CountingService.start(context)
                                }
                            },
                            onHistory = { push("history") },
                            onSettings = { push("settings") },
                        )
                    } else {
                        val counters by container.repository.counters(session.id)
                            .collectAsState(initial = emptyList())
                        CountingScreen(
                            session = session,
                            counters = counters,
                            lastUndoName = lastUndoName,
                            onCount = { counter -> container.engine.count(counter.id, counter.name) },
                            onUndo = { container.engine.undo() },
                            onGames = { games ->
                                scope.launch { container.repository.setGameCount(session.id, games) }
                            },
                            onEditCounters = { push("edit") },
                            onEnd = {
                                scope.launch {
                                    container.repository.endSession(session.id)
                                    CountingService.stop(context)
                                    Toast.makeText(
                                        context,
                                        R.string.session_saved,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
