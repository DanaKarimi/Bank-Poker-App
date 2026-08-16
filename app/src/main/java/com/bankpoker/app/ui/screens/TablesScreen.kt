package com.bankpoker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.TablesViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    onTableClick: (String) -> Unit,
    onNavigateToStats: () -> Unit
) {
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var selectedTableForDelete by remember { mutableStateOf<PokerTable?>(null) }
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val playerCounts by viewModel.playerCounts.collectAsState(initial = emptyMap())
    val lastChipValue by viewModel.lastChipValue.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "♠ Bank Poker", 
                        color = Gold, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ) 
                },
                actions = {
                    TextButton(
                        onClick = onNavigateToStats,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Gold
                        )
                    ) {
                        Text(
                            text = "STATS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTableDialog = true },
                modifier = Modifier.shadow(12.dp, CircleShape),
                containerColor = Gold,
                contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Create Table",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = FeltBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF186349), FeltBackground),
                        radius = 1500f
                    )
                )
                .padding(paddingValues)
        ) {
            if (tables.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "♠",
                        fontSize = 96.sp,
                        color = Gold.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO TABLES YET",
                        style = MaterialTheme.typography.titleLarge,
                        color = Cream,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to create a new table",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cream.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tables) { table ->
                        TableCard(
                            table = table,
                            onClick = { onTableClick(table.id) },
                            onLongClick = { selectedTableForDelete = table },
                            playerCount = playerCounts[table.id] ?: 0
                        )
                    }
                }
            }
        }
    }

    if (showCreateTableDialog) {
        CreateTableDialog(
            initialChipValue = lastChipValue,
            onDismiss = { showCreateTableDialog = false },
            onCreateTable = { name, chipValue ->
                viewModel.createTable(name, chipValue)
                showCreateTableDialog = false
            }
        )
    }

    if (selectedTableForDelete != null) {
        AlertDialog(
            onDismissRequest = { selectedTableForDelete = null },
            title = { Text("Delete Table?", color = Gold) },
            text = { 
                Text(
                    "This will permanently delete '${selectedTableForDelete!!.name}' and ALL its players, buy-ins and exits. This cannot be undone.", 
                    color = Cream 
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTable(selectedTableForDelete!!.id)
                        selectedTableForDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTableForDelete = null }) {
                    Text("Cancel", color = Gold)
                }
            }
        )
    }
}

@Composable
fun TableCard(
    table: PokerTable,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    playerCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Gold.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.TableBar,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Cream,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(status = table.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chip value
            if (table.chipValue != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Chip Value",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$${table.chipValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Active players count
            if (playerCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Players",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$playerCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WinGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Status hint
            val statusHint = when (table.status) {
                "ACTIVE" -> "● Tap to manage"
                "CLOSED" -> "● Table closed"
                else -> ""
            }
            val statusColor = when (table.status) {
                "ACTIVE" -> WinGreen
                "CLOSED" -> LoseRed.copy(alpha = 0.7f)
                else -> Cream
            }
            Text(
                text = statusHint,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                letterSpacing = 1.sp
            )

            // Long-press hint for active tables
            if (table.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Long-press to delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.4f),
                    fontStyle = FontStyle.Italic
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
        "ACTIVE" -> WinGreen.copy(alpha = 0.15f)
        "CLOSED" -> LoseRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    }
    val textColor = when (status) {
        "ACTIVE" -> WinGreen
        "CLOSED" -> LoseRed
        else -> MaterialTheme.colorScheme.onSecondary
    }
    val borderColor = when (status) {
        "ACTIVE" -> WinGreen
        "CLOSED" -> LoseRed
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(6.dp)
        ),
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CreateTableDialog(
    initialChipValue: Long?,
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember(initialChipValue) { 
        mutableStateOf(initialChipValue?.toString() ?: "") 
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Table", color = Gold, fontWeight = FontWeight.Bold) },
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
                        unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                        focusedLabelColor = Gold,
                        cursorColor = Gold
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
                        unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                        focusedLabelColor = Gold,
                        cursorColor = Gold
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
                colors = ButtonDefaults.textButtonColors(contentColor = Gold)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Cream.copy(alpha = 0.7f))
            }
        }
    )
}
