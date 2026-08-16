package com.bankpoker.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.ui.theme.Gold
import com.bankpoker.app.ui.theme.FeltBackground
import com.bankpoker.app.viewmodel.TablesViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    onTableClick: (String) -> Unit,
    onNavigateToStats: () -> Unit
) {
    var showCreateTableDialog by remember { mutableStateOf(false) }
    val tables by viewModel.tables.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("♠ Bank Poker", color = Gold, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground,
                    titleContentColor = Gold
                ),
                actions = {
                    TextButton(onClick = onNavigateToStats) {
                        Text(
                            text = "Stats",
                            color = Gold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTableDialog = true },
                containerColor = Gold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Table")
            }
        }
    ) { paddingValues ->
        if (tables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tables yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap + to create a new table",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tables) { table ->
                    TableCard(
                        table = table,
                        onClick = { onTableClick(table.id) }
                    )
                }
            }
        }
    }

    if (showCreateTableDialog) {
        CreateTableDialog(
            onDismiss = { showCreateTableDialog = false },
            onCreateTable = { name, chipValue ->
                viewModel.createTable(name, chipValue)
                showCreateTableDialog = false
            }
        )
    }
}

@Composable
fun TableCard(
    table: PokerTable,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusBadge(status = table.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Players: ${table.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (table.chipValue != null) {
                Text(
                    text = "Chip Value: $${table.chipValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        "ACTIVE" -> Green80
        "CLOSED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun CreateTableDialog(
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Table", color = Gold) },
        text = {
            Column {
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("Table Name") },
                    singleLine = true,
                    isError = error != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.5f)
                    )
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = chipValue,
                    onValueChange = { chipValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Chip Value (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tableName.isBlank()) {
                        error = "Table name is required"
                        return@TextButton
                    }
                    val chipValueLong = chipValue.toLongOrNull()
                    onCreateTable(tableName.trim(), chipValueLong)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Gold
                )
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gold)
            }
        }
    )
}
