package com.bankpoker.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.ui.components.*
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.TablesViewModel
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.ExperimentalFoundationApi

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
    var showCreateTableSheet by remember { mutableStateOf(false) }
    var selectedTableForAction by remember { mutableStateOf<PokerTable?>(null) }
    var selectedTableForEdit by remember { mutableStateOf<PokerTable?>(null) }
    var selectedTableForDelete by remember { mutableStateOf<PokerTable?>(null) }
    var tableDeleteDetails by remember { mutableStateOf(Pair(0, 0)) }
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
                onClick = { showCreateTableSheet = true },
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
                        StatCard(
                            label = "ACTIVE",
                            value = "$activeCount",
                            valueColor = WinGreen,
                            icon = "●",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "CLOSED",
                            value = "$closedCount",
                            valueColor = Amber80,
                            icon = "✓",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
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

                    // Discoverability hint
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Hold a table for options",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cream.copy(alpha = 0.5f)
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
                        items(filteredTables, key = { it.id }) { table ->
                            TableCard(
                                table = table,
                                onClick = { onTableClick(table.id) },
                                onLongClick = { selectedTableForAction = table },
                                playerCount = playerCounts[table.id] ?: 0
                            )
                        }
                    }
                }
            }
        }
    }


    if (showCreateTableSheet) {
        CreateTableBottomSheet(
            initialChipValue = lastChipValue,
            onDismiss = { showCreateTableSheet = false },
            onCreateTable = { name, chipValue, hasEntryFee, entryFee ->
                viewModel.createTable(name, chipValue, hasEntryFee, entryFee)
                showCreateTableSheet = false
            }
        )
    }

    selectedTableForAction?.let { table ->
        TableActionBottomSheet(
            table = table,
            onDismiss = { selectedTableForAction = null },
            onEditClick = {
                val tableToEdit = table
                selectedTableForAction = null
                selectedTableForEdit = tableToEdit
            },
            onDeleteClick = {
                val tableToDelete = table
                coroutineScope.launch {
                    tableDeleteDetails = viewModel.getTableDetailsCount(tableToDelete.id)
                    selectedTableForAction = null
                    selectedTableForDelete = tableToDelete
                }
            }
        )
    }

    selectedTableForEdit?.let { table ->
        EditTableBottomSheet(
            table = table,
            onDismiss = { selectedTableForEdit = null },
            onConfirm = { name, chipValue, hasEntryFee, entryFee ->
                viewModel.updateTable(table.id, name, chipValue, hasEntryFee, entryFee)
                selectedTableForEdit = null
            }
        )
    }

    selectedTableForDelete?.let { table ->
        DeleteTableConfirmDialog(
            tableName = table.name,
            playersCount = tableDeleteDetails.first,
            recordsCount = tableDeleteDetails.second,
            onDismiss = { selectedTableForDelete = null },
            onConfirm = {
                viewModel.deleteTable(table.id)
                selectedTableForDelete = null
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableCard(
    table: PokerTable,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    playerCount: Int
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "tableCardScale"
    )

    val borderColor = if (table.status == "ACTIVE") WinGreen.copy(alpha = 0.8f) else Gold.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (table.hasEntryFee) {
                        Surface(
                            color = Gold.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ENTRY FEE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    StatusBadge(status = table.status)
                }
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableBottomSheet(
    initialChipValue: Long?,
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?, Boolean, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember(initialChipValue) { 
        mutableStateOf(initialChipValue?.toString() ?: "") 
    }
    var hasEntryFee by remember { mutableStateOf(false) }
    var entryFeeAmount by remember { mutableStateOf("") }
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
            SectionHeader(title = "NEW POKER TABLE", suit = "♠")

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

            Spacer(modifier = Modifier.height(16.dp))

            // Entry Fee Toggle Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Entry Fee",
                                style = MaterialTheme.typography.titleMedium,
                                color = Cream,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Require entry fee for this game",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cream.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = hasEntryFee,
                            onCheckedChange = { hasEntryFee = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Gold,
                                checkedTrackColor = Gold.copy(alpha = 0.5f),
                                uncheckedThumbColor = Cream.copy(alpha = 0.5f),
                                uncheckedTrackColor = Gold.copy(alpha = 0.2f)
                            )
                        )
                    }

                    if (hasEntryFee) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = entryFeeAmount,
                            onValueChange = { entryFeeAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Entry Fee Amount", color = Cream.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = "CREATE TABLE",
                onClick = {
                    if (tableName.isBlank()) {
                        error = "Table name is required"
                        return@GoldGradientButton
                    }
                    val chipValueLong = chipValue.toLongOrNull()
                    val entryFeeLong = if (hasEntryFee) {
                        entryFeeAmount.toLongOrNull() ?: chipValueLong ?: 0L
                    } else null
                    onCreateTable(tableName.trim(), chipValueLong, hasEntryFee, entryFeeLong)
                }
            )
        }
    }
}
