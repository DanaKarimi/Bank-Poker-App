package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.ui.components.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onTableClick: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onNavigateToRequests: ((String, String) -> Unit)? = null
) {

    val context = LocalContext.current
    val group by viewModel.group.collectAsState(initial = null)
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val balances by viewModel.balances.collectAsState(initial = emptyList())

    var showCreateTableSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedTableForAction by remember { mutableStateOf<PokerTable?>(null) }
    var selectedTableForEdit by remember { mutableStateOf<PokerTable?>(null) }
    var selectedTableForDelete by remember { mutableStateOf<PokerTable?>(null) }
    var tableDeleteDetails by remember { mutableStateOf(Pair(0, 0)) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = group?.name ?: "Group", 
                            color = Cream, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Gold
                        )
                    }
                },
                actions = {
                    if (onNavigateToRequests != null && group?.mode == "ONLINE") {
                        IconButton(onClick = {
                            group?.let { g ->
                                onNavigateToRequests(g.id, g.name)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pending Requests",
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "History",
                            tint = Gold
                        )
                    }

                    IconButton(onClick = {
                        val shareSettlements = calculateGroupSettlement(balances)
                        val text = buildGroupShareResultsText(
                            groupName = group?.name ?: "Group",
                            balances = balances,
                            settlements = shareSettlements
                        )
                        shareText(context, text)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Results",
                            tint = Gold
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Gold
                        )
                    }


                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(FeltCard)
                    ) {
                        if (onNavigateToRequests != null && group?.mode == "ONLINE") {
                            DropdownMenuItem(
                                text = { Text("Pending Requests", color = Cream) },
                                onClick = {
                                    showMenu = false
                                    group?.let { g -> onNavigateToRequests(g.id, g.name) }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300)
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit Name", color = Cream) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Gold
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group", color = LoseRed) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = LoseRed
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
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

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = "TABLES",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "BALANCES",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "STATS",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "GroupDetailTabAnimation",
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> TablesTab(
                            tables = tables,
                            onTableClick = onTableClick,
                            onTableLongClick = { table -> selectedTableForAction = table }
                        )
                        1 -> BalancesTab(balances = balances)
                        2 -> GroupStatsTab(
                            tables = tables,
                            balances = balances,
                            onRecordManualPayment = { payer, receiver, amount ->
                                viewModel.recordManualPayment(payer, receiver, amount)
                            },
                            onMarkPaid = { from, to, amount ->
                                viewModel.recordPayment(from, to, amount)
                            },
                            onNavigateToHistory = onNavigateToHistory,
                            onPlayerClick = onPlayerClick
                        )
                    }
                }
            }
        }
    }

    if (showCreateTableSheet) {
        CreateTableBottomSheet(
            onDismiss = { showCreateTableSheet = false },
            onCreateTable = { name, chipValue, hasEntryFee, entryFee ->
                viewModel.createTable(
                    name = name,
                    chipValue = chipValue,
                    hasEntryFee = hasEntryFee,
                    entryFee = entryFee,
                    onSuccess = {
                        Toast.makeText(context, "Table created successfully!", Toast.LENGTH_SHORT).show()
                        showCreateTableSheet = false
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    if (showEditDialog) {
        EditGroupNameBottomSheet(
            currentName = group?.name ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                viewModel.updateGroupName(newName)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteGroupBottomSheet(
            groupName = group?.name ?: "Group",
            tablesCount = tables.size,
            playersCount = balances.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteGroup()
                onNavigateBack()
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
}




@Composable
fun TablesTab(
    tables: List<PokerTable>,
    onTableClick: (String) -> Unit,
    onTableLongClick: (PokerTable) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTables = remember(tables, searchQuery) {
        if (searchQuery.isBlank()) tables
        else tables.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (tables.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "♠",
                    fontSize = 64.sp,
                    color = Gold.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO TABLES YET",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to create a table",
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTables) { table ->
                    TableCardSimple(
                        table = table,
                        onClick = { onTableClick(table.id) },
                        onLongClick = { onTableLongClick(table) }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableCardSimple(
    table: PokerTable,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "tableCardScale"
    )

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
                color = if (table.status == "ACTIVE") WinGreen.copy(alpha = 0.6f) else Gold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "♠",
                                color = Gold,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.titleMedium,
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
            
            if (table.chipValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Chip Value: $${table.chipValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold
                )
            }
        }
    }
}

@Composable
fun BalancesTab(
    balances: List<GroupBalance>
) {
    if (balances.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "No balances yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Cream.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(balances) { balance ->
                BalanceCard(balance = balance)
            }
        }
    }
}

@Composable
fun BalanceCard(
    balance: GroupBalance
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = Gold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = balance.playerName,
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$${balance.balance}",
                style = MaterialTheme.typography.titleMedium,
                color = if (balance.balance >= 0) WinGreen else LoseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GroupStatsTab(
    tables: List<PokerTable>,
    balances: List<GroupBalance>,
    onRecordManualPayment: (String, String, Long) -> Unit = { _, _, _ -> },
    onMarkPaid: (String, String, Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null
) {
    val closedCount = tables.count { it.status == "CLOSED" }
    val biggestWinner = balances.maxByOrNull { it.balance }
    val biggestDebtor = balances.minByOrNull { it.balance }
    val settlements = calculateGroupSettlement(balances)

    var showManualPaymentSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "♠", color = Gold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GROUP STATS",
                            style = MaterialTheme.typography.labelLarge,
                            color = Cream,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroStat(label = "TABLES", value = "${tables.size}", color = Cream)
                        HeroStat(label = "CLOSED", value = "$closedCount", color = Amber80)
                        HeroStat(label = "PLAYERS", value = "${balances.size}", color = WinGreen)
                    }
                }
            }
        }

        // Biggest winner
        if (biggestWinner != null && biggestWinner.balance > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, WinGreen.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .clickable(enabled = onPlayerClick != null) {
                            onPlayerClick?.invoke(biggestWinner.playerName)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆 BIGGEST WINNER",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${biggestWinner.playerName}  +${biggestWinner.balance}",
                            style = MaterialTheme.typography.titleMedium,
                            color = WinGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Biggest debtor
        if (biggestDebtor != null && biggestDebtor.balance < 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, LoseRed.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .clickable(enabled = onPlayerClick != null) {
                            onPlayerClick?.invoke(biggestDebtor.playerName)
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💸 BIGGEST DEBTOR",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${biggestDebtor.playerName}  ${biggestDebtor.balance}",
                            style = MaterialTheme.typography.titleMedium,
                            color = LoseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Settlement plan
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                            text = "SETTLEMENT PLAN",
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Button(
                            onClick = { showManualPaymentSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Manual Payment",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (balances.isEmpty()) {
                        Text(
                            text = "No data yet. Close a table in this group first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.6f)
                        )
                    } else if (settlements.isEmpty()) {
                        Text(
                            text = "All settled! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WinGreen
                        )
                    } else {
                        settlements.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = s.fromPlayer,
                                        color = LoseRed,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = " pays ",
                                        color = Cream.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = s.toPlayer,
                                        color = WinGreen,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${s.amount}",
                                        color = Gold,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { onMarkPaid(s.fromPlayer, s.toPlayer, s.amount) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = WinGreen)
                                    ) {
                                        Text("PAID ✓")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full History Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            GoldGradientButton(
                text = "VIEW FULL HISTORY",
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showManualPaymentSheet) {
        ManualPaymentBottomSheet(
            balances = balances,
            onDismiss = { showManualPaymentSheet = false },
            onConfirm = { payer, receiver, amount ->
                onRecordManualPayment(payer, receiver, amount)
                showManualPaymentSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPaymentBottomSheet(
    balances: List<GroupBalance>,
    onDismiss: () -> Unit,
    onConfirm: (payer: String, receiver: String, amount: Long) -> Unit
) {
    val payerCandidates = remember(balances) {
        val debtors = balances.filter { it.balance < 0 }.sortedBy { it.balance }.map { it.playerName }
        val others = balances.filter { it.balance >= 0 }.map { it.playerName }
        (debtors + others).distinct()
    }

    var selectedPayer by remember(payerCandidates) { mutableStateOf(payerCandidates.firstOrNull() ?: "") }
    var payerExpanded by remember { mutableStateOf(false) }

    val receiverCandidates = remember(balances, selectedPayer) {
        val creditors = balances.filter { it.balance > 0 && it.playerName != selectedPayer }.sortedByDescending { it.balance }.map { it.playerName }
        val others = balances.filter { it.balance <= 0 && it.playerName != selectedPayer }.map { it.playerName }
        (creditors + others).distinct()
    }

    var selectedReceiver by remember(receiverCandidates) {
        mutableStateOf(receiverCandidates.firstOrNull() ?: "")
    }
    var receiverExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    val amountLong = amountText.toLongOrNull() ?: 0L

    val isValid = selectedPayer.isNotBlank() &&
            selectedReceiver.isNotBlank() &&
            selectedPayer != selectedReceiver &&
            amountLong > 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "MANUAL PAYMENT", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            // Payer selector
            ExposedDropdownMenuBox(
                expanded = payerExpanded,
                onExpandedChange = { payerExpanded = !payerExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPayer,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payer (From)", color = Gold) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payerExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                        focusedLabelColor = Gold,
                        unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                        focusedTrailingIconColor = Gold,
                        unfocusedTrailingIconColor = Cream.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = payerExpanded,
                    onDismissRequest = { payerExpanded = false },
                    modifier = Modifier.background(FeltCard)
                ) {
                    payerCandidates.forEach { name ->
                        val balance = balances.find { it.playerName == name }?.balance
                        val balanceLabel = if (balance != null) {
                            if (balance < 0) " (Owes ${-balance})" else if (balance > 0) " (+${balance})" else " (0)"
                        } else ""
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$name$balanceLabel",
                                    color = if (balance != null && balance < 0) LoseRed else Cream
                                )
                            },
                            onClick = {
                                selectedPayer = name
                                payerExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Receiver selector
            ExposedDropdownMenuBox(
                expanded = receiverExpanded,
                onExpandedChange = { receiverExpanded = !receiverExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedReceiver,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Receiver (To)", color = Gold) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                        focusedLabelColor = Gold,
                        unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                        focusedTrailingIconColor = Gold,
                        unfocusedTrailingIconColor = Cream.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = receiverExpanded,
                    onDismissRequest = { receiverExpanded = false },
                    modifier = Modifier.background(FeltCard)
                ) {
                    receiverCandidates.forEach { name ->
                        val balance = balances.find { it.playerName == name }?.balance
                        val balanceLabel = if (balance != null) {
                            if (balance > 0) " (Owed +${balance})" else if (balance < 0) " (${balance})" else " (0)"
                        } else ""
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$name$balanceLabel",
                                    color = if (balance != null && balance > 0) WinGreen else Cream
                                )
                            },
                            onClick = {
                                selectedReceiver = name
                                receiverExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("Amount", color = Gold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                    cursorColor = Gold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Gold Gradient Confirm Button
            GoldGradientButton(
                text = "RECORD PAYMENT",
                onClick = {
                    if (isValid) {
                        onConfirm(selectedPayer, selectedReceiver, amountLong)
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun calculateGroupSettlement(balances: List<GroupBalance>): List<Settlement> {

    val debtors = mutableListOf<Pair<String, Long>>()
    val creditors = mutableListOf<Pair<String, Long>>()
    
    balances.forEach { balance ->
        when {
            balance.balance < 0 -> debtors.add(Pair(balance.playerName, -balance.balance))
            balance.balance > 0 -> creditors.add(Pair(balance.playerName, balance.balance))
        }
    }
    
    val settlements = mutableListOf<Settlement>()
    
    var i = 0
    var j = 0
    
    while (i < debtors.size && j < creditors.size) {
        val debtor = debtors[i]
        val creditor = creditors[j]
        
        val amount = minOf(debtor.second, creditor.second)
        
        settlements.add(
            Settlement(
                fromPlayer = debtor.first,
                toPlayer = creditor.first,
                amount = amount
            )
        )
        
        debtors[i] = Pair(debtor.first, debtor.second - amount)
        creditors[j] = Pair(creditor.first, creditor.second - amount)
        
        if (debtors[i].second == 0L) i++
        if (creditors[j].second == 0L) j++
    }
    
    return settlements
}



@Composable
fun PaymentsTab(
    payments: List<Payment>
) {
    if (payments.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "No payments yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Cream.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(payments) { payment ->
                PaymentCard(payment = payment)
            }
        }
    }
}

@Composable
fun PaymentCard(
    payment: Payment
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${payment.fromPlayer} → ${payment.toPlayer}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${payment.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableBottomSheet(
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?, Boolean, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember { mutableStateOf("") }
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
            SectionHeader(title = "NEW GROUP TABLE", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tableName,
                onValueChange = { 
                    tableName = it
                    if (error != null) error = null
                },
                label = { Text("Table Name", color = Cream.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. Friday Game #1", color = Cream.copy(alpha = 0.4f)) },
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

            // Chip presets
            Text(
                text = "CHIP VALUE",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupNameBottomSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "EDIT GROUP NAME", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (error != null) error = null
                },
                label = { Text("Group Name", color = Gold) },
                singleLine = true,
                isError = error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                    focusedLabelColor = Gold,
                    cursorColor = Gold,
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "Group name cannot be empty"
                            return@Button
                        }
                        onConfirm(name.trim())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteGroupBottomSheet(
    groupName: String,
    tablesCount: Int,
    playersCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(LoseRed.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, LoseRed.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LoseRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DELETE GROUP?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LoseRed,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure you want to permanently delete \"$groupName\" with its $tablesCount tables and $playersCount players?",
                style = MaterialTheme.typography.bodyMedium,
                color = Cream.copy(alpha = 0.85f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This action cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = Cream.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoseRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun buildGroupShareResultsText(
    groupName: String,
    balances: List<GroupBalance>,
    settlements: List<Settlement>
): String {
    val sb = StringBuilder()
    sb.appendLine("🃏 $groupName")
    sb.appendLine("Players:")
    val sorted = balances.sortedByDescending { it.balance }
    if (sorted.isEmpty()) {
        sb.appendLine("No player balances yet")
    } else {
        sorted.forEachIndexed { index, b ->
            val sign = if (b.balance > 0) "+" else ""
            sb.appendLine("${index + 1}. ${b.playerName}: $sign${b.balance}")
        }
    }
    sb.appendLine("Settlement:")
    if (settlements.isEmpty()) {
        sb.appendLine("All settled!")
    } else {
        settlements.forEach { s ->
            sb.appendLine("${s.fromPlayer} -> ${s.toPlayer}: ${s.amount}")
        }
    }
    return sb.toString().trimEnd()
}



