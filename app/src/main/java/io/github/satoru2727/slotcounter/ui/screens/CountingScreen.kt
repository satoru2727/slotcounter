@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.satoru2727.slotcounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.satoru2727.slotcounter.R
import io.github.satoru2727.slotcounter.core.KeyBinding
import io.github.satoru2727.slotcounter.data.CounterEntity
import io.github.satoru2727.slotcounter.data.SessionEntity
import io.github.satoru2727.slotcounter.ui.Fmt
import io.github.satoru2727.slotcounter.ui.components.CounterCard
import kotlinx.coroutines.delay

@Composable
fun CountingScreen(
    session: SessionEntity,
    counters: List<CounterEntity>,
    lastUndoName: String?,
    onCount: (CounterEntity) -> Unit,
    onUndo: () -> Unit,
    onGames: (Int) -> Unit,
    onEditCounters: () -> Unit,
    onEnd: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showEndDialog by remember { mutableStateOf(false) }
    var editGames by remember { mutableStateOf(false) }

    LaunchedEffect(session.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    val minutes = ((now - session.startedAt) / 60_000).coerceAtLeast(0)
    val elapsed = if (minutes >= 60) {
        stringResource(R.string.duration_hours, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session.machineName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.elapsed) + " " + elapsed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditCounters) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_counters))
                    }
                    TextButton(onClick = { showEndDialog = true }) {
                        Text(
                            text = stringResource(R.string.end_session),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = lastUndoName != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            text = lastUndoName?.let {
                                stringResource(R.string.undo_with_name, it)
                            } ?: stringResource(R.string.undo),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GamesCard(
                    games = session.gameCount,
                    onEdit = { editGames = true },
                    onAdjust = onGames,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                VolumeHint(counters)
            }
            if (counters.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.no_counters),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(counters, key = { it.id }) { counter ->
                    CounterCard(
                        counter = counter,
                        games = session.gameCount,
                        onClick = { onCount(counter) },
                    )
                }
            }
        }
    }

    if (editGames) {
        GameCountDialog(
            initial = session.gameCount,
            onDismiss = { editGames = false },
            onConfirm = {
                onGames(it)
                editGames = false
            },
        )
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text(stringResource(R.string.end_title)) },
            text = { Text(stringResource(R.string.end_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndDialog = false
                        onEnd()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.end_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GamesCard(
    games: Int,
    onEdit: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.games_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepBox(stringResource(R.string.minus_ten)) { onAdjust(games - 10) }
                StepBox(stringResource(R.string.minus_one)) { onAdjust(games - 1) }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEdit),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = Fmt.grouped(games),
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.games_unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
                    )
                }
                StepBox(stringResource(R.string.plus_one)) { onAdjust(games + 1) }
                StepBox(stringResource(R.string.plus_ten)) { onAdjust(games + 10) }
            }
            if (games == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.games_hint_probability),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun StepBox(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(width = 44.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun VolumeHint(counters: List<CounterEntity>) {
    val up = counters.firstOrNull { it.keyBinding == KeyBinding.UP }?.name
    val down = counters.firstOrNull { it.keyBinding == KeyBinding.DOWN }?.name

    Column(Modifier.fillMaxWidth()) {
        if (up == null && down == null) {
            Text(
                text = stringResource(R.string.volume_hint_unbound),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val parts = buildList {
                up?.let { add(stringResource(R.string.key_up) + " " + it) }
                down?.let { add(stringResource(R.string.key_down) + " " + it) }
            }
            Text(
                text = parts.joinToString(" ・ "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.double_click_undo),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GameCountDialog(
    initial: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(initial.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_games_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    if (input.length <= 7 && input.all { it.isDigit() }) text = input
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.games_label)) },
                supportingText = { Text(stringResource(R.string.games_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
