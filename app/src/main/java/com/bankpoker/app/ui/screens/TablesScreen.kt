package com.bankpoker.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.flow.collectLatest

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    onTableClick: (String) -> Unit,
    onNavigateToStats: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var selectedTableForDelete by remember { mutableStateOf<PokerTable?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "CLOSED"

    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val playerCounts by viewModel.playerCounts.collectAsState(initial = emptyMap())
    val lastChipValue by viewModel.lastChipValue.collectAsState(initial = null)

    val filteredTables = remember(tables, searchQuery, selectedStatusFilter) {
        tables.filter { table ->
            val matchesQuery = if (searchQuery.isBlank()) true
                else table.name.contains(searchQuery.trim(), ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "ACTIVE" -> table.status == "ACTIVE"
                "CLOSED" -> table.status == "CLOSED"
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    snackbarHostState.showSnackbar("Backup exported successfully!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!json.isNullOrBlank()) {
                        viewModel.restoreBackup(json)
                        snackbarHostState.showSnackbar("Backup restored successfully!")
                    } else {
                        snackbarHostState.showSnackbar("Failed to read backup file.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message ?: "Invalid backup file"}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bank Poker", 
                            color = Cream, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
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

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Gold
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(FeltCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Backup", color = Cream) },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("BankPoker_backup.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Backup", color = Cream) },
                                onClick = {
                                    showMenu = false
                                    showConfirmRestoreDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            CasinoWatermarks()

            Column(modifier = Modifier.fillMaxSize()) {
                // Hero Summary Strip
                if (tables.isNotEmpty()) {
                    val activeCount = remember(tables) { tables.count { it.status == "ACTIVE" } }
                    val closedCount = remember(tables) { tables.count { it.status == "CLOSED" } }
                    val totalCount = tables.size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        com.bankpoker.app.ui.components.StatCard(
                            label = "ACTIVE",
                            value = "$activeCount",
                            valueColor = WinGreen,
                            icon = "●",
                            modifier = Modifier.weight(1f)
                        )
                        com.bankpoker.app.ui.components.StatCard(
                            label = "CLOSED",
                            value = "$closedCount",
                            valueColor = Amber80,
                            icon = "✓",
                            modifier = Modifier.weight(1f)
                        )
                        com.bankpoker.app.ui.components.StatCard(
                            label = "TOTAL",
                            value = "$totalCount",
                            valueColor = Cream,
                            icon = "♠",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (tables.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search tables...", color = Cream.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Gold
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Gold
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream,
                            cursorColor = Gold,
                            focusedContainerColor = FeltCard,
                            unfocusedContainerColor = FeltCard
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabButton(
                            text = "ALL",
                            selected = selectedStatusFilter == "ALL",
                            onClick = { selectedStatusFilter = "ALL" },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "ACTIVE",
                            selected = selectedStatusFilter == "ACTIVE",
                            onClick = { selectedStatusFilter = "ACTIVE" },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "CLOSED",
                            selected = selectedStatusFilter == "CLOSED",
                            onClick = { selectedStatusFilter = "CLOSED" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }


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
                } else if (filteredTables.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tables found",
                            color = Cream.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTables) { table ->
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
    }


    if (showCreateTableDialog) {
        CreateTableBottomSheet(
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

    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            containerColor = FeltCard,
            title = { Text("Restore Backup?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This replaces ALL current data!",
                    color = Cream,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmRestoreDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = false }) {
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
    val borderColor = if (table.status == "ACTIVE") WinGreen.copy(alpha = 0.8f) else Gold.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        color = if (table.status == "ACTIVE") WinGreen.copy(alpha = 0.2f) else Gold.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .border(1.dp, if (table.status == "ACTIVE") WinGreen.copy(alpha = 0.5f) else Gold.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "♠",
                                color = if (table.status == "ACTIVE") WinGreen else Gold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
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
            
            // Created / Closed date
            Text(
                text = "Created: ${formatTimestamp(table.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Cream.copy(alpha = 0.5f)
            )
            
            if (table.closedAt != null) {
                Text(
                    text = "Closed: ${formatTimestamp(table.closedAt!!)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableBottomSheet(
    initialChipValue: Long?,
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember(initialChipValue) { 
        mutableStateOf(initialChipValue?.toString() ?: "") 
    }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val chipPresets = listOf(5L, 10L, 25L, 50L, 100L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FeltCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Gold) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            com.bankpoker.app.ui.components.SectionHeader(title = "NEW POKER TABLE", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tableName,
                onValueChange = { 
                    tableName = it
                    if (error != null) error = null
                },
                label = { Text("Table Name", color = Cream.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. High Rollers Table", color = Cream.copy(alpha = 0.4f)) },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = Gold,
                    focusedContainerColor = FeltBackground,
                    unfocusedContainerColor = FeltBackground
                )
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error!!,
                    color = LoseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chip Value Selector
            Text(
                text = "DEFAULT CHIP VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chipPresets.forEach { preset ->
                    val isSelected = chipValue == preset.toString()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Gold else Gold.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                chipValue = if (isSelected) "" else preset.toString()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Gold.copy(alpha = 0.25f) else FeltBackground
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$$preset",
                                color = if (isSelected) Gold else Cream,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = chipValue,
                onValueChange = { chipValue = it.filter { c -> c.isDigit() } },
                label = { Text("Custom Chip Value (optional)", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = Gold,
                    focusedContainerColor = FeltBackground,
                    unfocusedContainerColor = FeltBackground
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            com.bankpoker.app.ui.components.GoldGradientButton(
                text = "CREATE TABLE",
                onClick = {
                    if (tableName.isBlank()) {
                        error = "Table name is required"
                        return@GoldGradientButton
                    }
                    val chipValueLong = chipValue.toLongOrNull()
                    onCreateTable(tableName.trim(), chipValueLong)
                }
            )
        }
    }
}
