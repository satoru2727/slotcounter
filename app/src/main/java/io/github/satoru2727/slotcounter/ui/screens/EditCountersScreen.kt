@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.satoru2727.slotcounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.satoru2727.slotcounter.R
import io.github.satoru2727.slotcounter.core.COUNTER_COLOR_COUNT
import io.github.satoru2727.slotcounter.core.KeyBinding
import io.github.satoru2727.slotcounter.data.CounterEntity
import io.github.satoru2727.slotcounter.ui.components.KeyBadge
import io.github.satoru2727.slotcounter.ui.theme.counterColor

@Composable
fun EditCountersScreen(
    counters: List<CounterEntity>,
    onBack: () -> Unit,
    onAdd: (name: String, keyBinding: Int, colorIndex: Int) -> Unit,
    onUpdate: (CounterEntity) -> Unit,
    onDelete: (CounterEntity) -> Unit,
    onMove: (CounterEntity, Int) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CounterEntity?>(null) }
    var deleting by remember { mutableStateOf<CounterEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.counter_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (counters.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_counters),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(counters, key = { it.id }) { counter ->
                        CounterEditRow(
                            counter = counter,
                            canMoveUp = counters.firstOrNull()?.id != counter.id,
                            canMoveDown = counters.lastOrNull()?.id != counter.id,
                            onEdit = { editing = counter },
                            onMove = { delta -> onMove(counter, delta) },
                            onDelete = { deleting = counter },
                        )
                    }
                }
            }

            Button(
                onClick = { showAdd = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.add_counter))
            }
        }
    }

    if (showAdd) {
        CounterDialog(
            initial = null,
            defaultColor = counters.size % COUNTER_COLOR_COUNT,
            onDismiss = { showAdd = false },
            onSave = { name, binding, color ->
                onAdd(name, binding, color)
                showAdd = false
            },
        )
    }

    editing?.let { target ->
        CounterDialog(
            initial = target,
            defaultColor = target.colorIndex,
            onDismiss = { editing = null },
            onSave = { name, binding, color ->
                onUpdate(target.copy(name = name, keyBinding = binding, colorIndex = color))
                editing = null
            },
        )
    }

    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_counter_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_counter_message,
                        target.name,
                        target.count,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        deleting = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CounterEditRow(
    counter: CounterEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(counterColor(counter.colorIndex)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = counter.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.counts_suffix, counter.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            KeyBadge(counter.keyBinding)
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CounterDialog(
    initial: CounterEntity?,
    defaultColor: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, keyBinding: Int, colorIndex: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var binding by remember { mutableIntStateOf(initial?.keyBinding ?: KeyBinding.NONE) }
    var color by remember { mutableIntStateOf(initial?.colorIndex ?: defaultColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.add_counter else R.string.edit_counter_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.counter_name)) },
                    placeholder = { Text(stringResource(R.string.counter_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.key_binding),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(KeyBinding.NONE, KeyBinding.UP, KeyBinding.DOWN).forEach { option ->
                        FilterChip(
                            selected = binding == option,
                            onClick = { binding = option },
                            label = { Text(bindingLabel(option)) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.key_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = stringResource(R.string.color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(COUNTER_COLOR_COUNT) { index ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(counterColor(index))
                                .border(
                                    width = if (index == color) 3.dp else 1.dp,
                                    color = if (index == color) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape,
                                )
                                .clickable { color = index },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), binding, color) },
                enabled = name.isNotBlank(),
            ) {
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

@Composable
private fun bindingLabel(binding: Int): String = when (binding) {
    KeyBinding.UP -> stringResource(R.string.key_up)
    KeyBinding.DOWN -> stringResource(R.string.key_down)
    else -> stringResource(R.string.key_none)
}
