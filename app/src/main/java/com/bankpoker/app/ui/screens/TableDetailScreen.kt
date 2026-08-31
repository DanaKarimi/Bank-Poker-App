package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*


import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.bankpoker.app.ui.components.*
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.ui.theme.Amber80
import com.bankpoker.app.ui.theme.AvatarColors
import com.bankpoker.app.ui.theme.CasinoWatermarks
import com.bankpoker.app.ui.theme.Cream
import com.bankpoker.app.ui.theme.FeltBackground
import com.bankpoker.app.ui.theme.FeltCard
import com.bankpoker.app.ui.theme.Gold
import com.bankpoker.app.ui.theme.Green80
import com.bankpoker.app.ui.theme.LoseRed
import com.bankpoker.app.ui.theme.PokerChipAvatar
import com.bankpoker.app.ui.theme.Red80
import com.bankpoker.app.ui.theme.WinGreen
import com.bankpoker.app.ui.theme.Silver
import com.bankpoker.app.ui.theme.Bronze
import com.bankpoker.app.viewmodel.TableDetailUiState
import com.bankpoker.app.viewmodel.TableDetailViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TableDetailScreen(
    viewModel: TableDetailViewModel,
    onNavigateBack: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null,
    onNavigateToRequests: ((String, String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val players by viewModel.players.collectAsState(initial = emptyList())
    val buyIns by viewModel.buyIns.collectAsState(initial = emptyList())
    val exitRecords by viewModel.exitRecords.collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    val playerResults = remember(players, buyIns, exitRecords) {
        players.map { player ->
            val totalBuyIns = buyIns.filter { it.playerId == player.id }.sumOf { it.amount }
            val totalExits = exitRecords.filter { it.playerId == player.id }.sumOf { it.amount }
            val netResult = totalExits - totalBuyIns
            PlayerResult(
                player = player,
                totalBuyIns = totalBuyIns,
                totalExits = totalExits,
                netResult = netResult
            )
        }
    }
    val settlements = remember(playerResults) {
        calculateSettlement(playerResults)
    }

    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showBuyInDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCloseTableDialog by remember { mutableStateOf(false) }
    var selectedPlayerForBuyIn by remember { mutableStateOf<Player?>(null) }
    var selectedPlayerForExit by remember { mutableStateOf<Player?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.table?.name ?: "Table Detail", 
                            color = Cream, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Gold)
                    }
                },
                actions = {
                    val isRefreshing by viewModel.isRefreshing.collectAsState()

                    IconButton(
                        onClick = {
                            viewModel.refreshTableFromServer { success, errorMsg ->
                                if (success) {
                                    Toast.makeText(context, "Table synced from server", Toast.LENGTH_SHORT).show()
                                } else if (errorMsg != null) {
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh from Server",
                            tint = Gold
                        )
                    }

                    if (onNavigateToRequests != null && uiState.table?.groupId != null) {
                        IconButton(onClick = {
                            uiState.table?.let { t ->
                                onNavigateToRequests(t.groupId ?: "", t.name)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pending Requests",
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }

                    IconButton(onClick = {
                        val text = buildShareResultsText(
                            tableName = uiState.table?.name ?: "Table",
                            playerResults = playerResults,
                            settlements = settlements
                        )
                        shareText(context, text)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Results", tint = Gold)
                    }
                    if (uiState.table?.status == "ACTIVE") {
                        IconButton(onClick = { showCloseTableDialog = true }) {
                            Text(
                                text = "Close",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground,
                    titleContentColor = Gold,
                    navigationIconContentColor = Gold
                )
            )
        }
    )
 { paddingValues ->
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
                // Summary bar
                TableSummaryBar(
                    totalBuyIns = uiState.totalBuyIns,
                    totalExits = uiState.totalExits,
                    remainingBalance = uiState.remainingBalance,
                    chipValue = uiState.table?.chipValue
                )

                HorizontalPagerTabs(
                    onAddPlayer = { showAddPlayerDialog = true },
                    onBuyInClick = { player ->
                        selectedPlayerForBuyIn = player
                        showBuyInDialog = true
                    },
                    onExitClick = { player ->
                        selectedPlayerForExit = player
                        showExitDialog = true
                    },
                    players = players,
                    buyIns = buyIns,
                    exitRecords = exitRecords,
                    tableId = uiState.table?.id ?: "",
                    viewModel = viewModel,
                    isTableActive = uiState.table?.status == "ACTIVE",
                    tableHasEntryFee = uiState.table?.hasEntryFee == true,
                    onPlayerClick = onPlayerClick
                )
            }
        }
    }

    
    if (showAddPlayerDialog) {
        val savedNames by viewModel.savedPlayerNames.collectAsState(initial = emptyList())
        AddPlayerBottomSheet(
            onDismiss = { showAddPlayerDialog = false },
            onAddPlayer = { name ->
                viewModel.addPlayer(name)
                showAddPlayerDialog = false
            },
            savedNames = savedNames,
            existingNames = players.map { it.name }
        )
    }

    if (showBuyInDialog && selectedPlayerForBuyIn != null) {
        val currentPlayer = selectedPlayerForBuyIn!!
        BuyInBottomSheet(
            playerName = currentPlayer.name,
            onDismiss = {
                showBuyInDialog = false
                selectedPlayerForBuyIn = null
            },
            onConfirm = { amount, note ->
                val playerId = currentPlayer.id
                viewModel.addBuyIn(playerId, amount, note) { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Buy-in recorded successfully", Toast.LENGTH_SHORT).show()
                    } else if (errorMsg != null) {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
                showBuyInDialog = false
                selectedPlayerForBuyIn = null
            },
            viewModel = viewModel,
            playerId = currentPlayer.id
        )
    }

    if (showExitDialog && selectedPlayerForExit != null) {
        val currentPlayer = selectedPlayerForExit!!
        ExitBottomSheet(
            playerName = currentPlayer.name,
            onDismiss = {
                showExitDialog = false
                selectedPlayerForExit = null
            },
            onConfirm = { amount, note ->
                val playerId = currentPlayer.id
                viewModel.addExitRecord(playerId, amount, note) { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Exit recorded successfully", Toast.LENGTH_SHORT).show()
                    } else if (errorMsg != null) {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
                showExitDialog = false
                selectedPlayerForExit = null
            },
            viewModel = viewModel,
            playerId = currentPlayer.id
        )
    }
    
    if (showCloseTableDialog) {
        AlertDialog(
            onDismissRequest = { showCloseTableDialog = false },
            title = { Text("Close Table", color = Gold) },
            text = { Text("Are you sure you want to close this table? No new transactions will be allowed.", color = Cream) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.closeTable()
                        showCloseTableDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseTableDialog = false }) {
                    Text("Cancel", color = Gold)
                }
            }
        )
    }
}

@Composable
fun TableSummaryBar(
    totalBuyIns: Long,
    totalExits: Long,
    remainingBalance: Long,
    chipValue: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                    text = "TABLE SUMMARY",
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
                HeroStat(
                    label = "BUY-INS",
                    value = formatAmount(totalBuyIns, chipValue),
                    color = WinGreen
                )
                HeroStat(
                    label = "EXITS",
                    value = formatAmount(totalExits, chipValue),
                    color = Amber80
                )
                HeroStat(
                    label = "REMAINING",
                    value = formatAmount(remainingBalance, chipValue),
                    color = when {
                        remainingBalance < 0 -> LoseRed
                        remainingBalance == 0L -> Cream
                        else -> WinGreen
                    }
                )
            }
        }
    }
}

@Composable
fun HeroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Cream.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HorizontalPagerTabs(
    onAddPlayer: () -> Unit,
    onBuyInClick: (Player) -> Unit,
    onExitClick: (Player) -> Unit,
    players: List<Player>,
    buyIns: List<BuyIn>,
    exitRecords: List<ExitRecord>,
    tableId: String,
    viewModel: TableDetailViewModel,
    isTableActive: Boolean,
    tableHasEntryFee: Boolean = false,
    onPlayerClick: ((String) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                text = "PLAYERS",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "HISTORY",
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "RESULTS",
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
            label = "TableDetailTabAnimation"
        ) { tab ->
            when (tab) {
                0 -> PlayersTab(
                    players = players,
                    buyIns = buyIns,
                    exitRecords = exitRecords,
                    onAddPlayer = onAddPlayer,
                    onBuyInClick = onBuyInClick,
                    onExitClick = onExitClick,
                    isTableActive = isTableActive,
                    tableHasEntryFee = tableHasEntryFee,
                    viewModel = viewModel,
                    onPlayerClick = onPlayerClick
                )
                1 -> HistoryTab(
                    buyIns = buyIns,
                    exitRecords = exitRecords,
                    players = players,
                    viewModel = viewModel
                )
                2 -> ResultsTab(
                    players = players,
                    buyIns = buyIns,
                    exitRecords = exitRecords,
                    viewModel = viewModel,
                    tableHasEntryFee = tableHasEntryFee
                )
            }
        }
    }
}


@Composable
fun PlayersTab(
    players: List<Player>,
    buyIns: List<BuyIn>,
    exitRecords: List<ExitRecord>,
    onAddPlayer: () -> Unit,
    onBuyInClick: (Player) -> Unit,
    onExitClick: (Player) -> Unit,
    isTableActive: Boolean,
    tableHasEntryFee: Boolean = false,
    viewModel: com.bankpoker.app.viewmodel.TableDetailViewModel? = null,
    onPlayerClick: ((String) -> Unit)? = null
) {
    fun balanceOf(playerId: String): Long {
        val buy = buyIns.filter { it.playerId == playerId }.sumOf { it.amount }
        val exit = exitRecords.filter { it.playerId == playerId }.sumOf { it.amount }
        return buy - exit
    }

    val sortedPlayersWithRank = remember(players, buyIns, exitRecords) {
        players.sortedByDescending { balanceOf(it.id) }.mapIndexed { index, player ->
            Pair(player, index + 1)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredPlayers = remember(sortedPlayersWithRank, searchQuery) {
        if (searchQuery.isBlank()) sortedPlayersWithRank
        else sortedPlayersWithRank.filter { (player, _) ->
            player.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (players.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search players...", color = Cream.copy(alpha = 0.5f)) },
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
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "♠",
                            fontSize = 64.sp,
                            color = Gold.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No players yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Cream.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to add players",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.4f)
                        )
                    }
                }
            } else if (filteredPlayers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No players found",
                        color = Cream.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPlayers) { (player, rank) ->
                        val balance = balanceOf(player.id)

                        PlayerCard(
                            player = player,
                            currentBalance = balance,
                            finalResult = -balance,
                            onBuyInClick = { onBuyInClick(player) },
                            onExitClick = { onExitClick(player) },
                            isTableActive = isTableActive,
                            rank = rank,
                            tableHasEntryFee = tableHasEntryFee,
                            onToggleEntryFee = if (tableHasEntryFee && player.status == "PLAYING") {
                                { viewModel?.toggleEntryFee(player.id, !player.entryFeePaid) }
                            } else null,
                            onPlayerClick = onPlayerClick
                        )
                    }
                }
            }
        }

        if (isTableActive) {
            FloatingActionButton(
                onClick = onAddPlayer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .shadow(8.dp, CircleShape),
                containerColor = Gold,
                contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Player",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}


@Composable
fun PlayerCard(
    player: Player,
    currentBalance: Long,
    finalResult: Long,
    onBuyInClick: () -> Unit,
    onExitClick: () -> Unit,
    isTableActive: Boolean,
    rank: Int = 0,
    tableHasEntryFee: Boolean = false,
    onToggleEntryFee: (() -> Unit)? = null,
    onPlayerClick: ((String) -> Unit)? = null
) {
    val rankBorderColor = when (rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> Gold.copy(alpha = 0.5f)
    }
    val rankElevation = when (rank) {
        1 -> 10.dp
        2 -> 8.dp
        3 -> 6.dp
        else -> 4.dp
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (rank in 1..3) 2.dp else 1.5.dp,
                color = rankBorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = rankElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = onPlayerClick != null) {
                            onPlayerClick?.invoke(player.name)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rank > 0) {
                        Surface(
                            color = when (rank) {
                                1 -> Gold
                                2 -> Silver
                                3 -> Bronze
                                else -> Gold.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = when (rank) {
                                    1 -> "🥇 #1"
                                    2 -> "🥈 #2"
                                    3 -> "🥉 #3"
                                    else -> "#$rank"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (rank in 1..3) Color.Black else Gold,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Entry fee status indicator
                    if (tableHasEntryFee && player.status == "PLAYING") {
                        IconButton(
                            onClick = { onToggleEntryFee?.invoke() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (player.entryFeePaid) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(WinGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Paid",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(2.dp, Cream.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Empty circle for unpaid
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        PokerChipAvatar(name = player.name, size = 52.dp)
                    }


                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Cream,
                            fontWeight = FontWeight.Bold
                        )
                        if (tableHasEntryFee && player.status == "PLAYING") {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Entry Fee: ${if (player.entryFeePaid) "Paid" else "Unpaid"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (player.entryFeePaid) WinGreen else LoseRed,
                                fontWeight = FontWeight.Medium
                            )
                        } else {

                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(status = player.status)
                        }
                    }
                }

                if (player.status == "PLAYING") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (currentBalance >= 0) "+" else ""}$currentBalance",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (currentBalance >= 0) WinGreen else LoseRed,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "chips",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cream.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (player.status == "EXITED") {
                Spacer(modifier = Modifier.height(12.dp))
                val resultText = when {
                    finalResult > 0 -> "Creditor: +$finalResult"
                    finalResult < 0 -> "Debtor: $finalResult"
                    else -> "Break-even"
                }
                val resultColor = when {
                    finalResult > 0 -> WinGreen
                    finalResult < 0 -> LoseRed
                    else -> Cream
                }
                Surface(
                    color = resultColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resultText,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = resultColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (player.status == "PLAYING" && isTableActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBuyInClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buy-in", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onExitClick,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Gold, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Gold
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActionsTab(
    players: List<Player>,
    onBuyInClick: (Player) -> Unit,
    onExitClick: (Player) -> Unit,
    isTableActive: Boolean
) {
    if (!isTableActive) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Table is closed. No actions allowed.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }
    
    if (players.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active players",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* Show player selection for buy-in */ },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select a player from the list below to perform actions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        players.forEach { player ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onBuyInClick(player) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Buy-in")
                        }
                        Button(
                            onClick = { onExitClick(player) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Gold
                            ),
                            modifier = Modifier.border(1.dp, Gold, RoundedCornerShape(8.dp)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Exit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    buyIns: List<BuyIn>,
    exitRecords: List<ExitRecord>,
    players: List<Player>,
    viewModel: TableDetailViewModel
) {
    var selectedTransactionForEdit by remember { mutableStateOf<TransactionItem?>(null) }
    var selectedTransactionForDelete by remember { mutableStateOf<TransactionItem?>(null) }
    
    val playerMap = players.associateBy { it.id }
    
    // Combine and sort all transactions
    val allTransactions = remember(buyIns, exitRecords) {
        val buyInTransactions = buyIns.map { b ->
            TransactionItem(
                id = b.id,
                tableId = b.tableId,
                type = "Buy-in",
                playerId = b.playerId,
                playerName = playerMap[b.playerId]?.name ?: "Unknown",
                amount = b.amount,
                note = b.note,
                timestamp = b.createdAt
            )
        }
        val exitTransactions = exitRecords.map { e ->
            TransactionItem(
                id = e.id,
                tableId = e.tableId,
                type = "Exit",
                playerId = e.playerId,
                playerName = playerMap[e.playerId]?.name ?: "Unknown",
                amount = e.amount,
                note = e.note,
                timestamp = e.createdAt
            )
        }
        (buyInTransactions + exitTransactions).sortedByDescending { it.timestamp }
    }
    
    if (allTransactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    } else {
        Text(
            text = "Long-press a transaction to edit or delete",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allTransactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onEdit = { selectedTransactionForEdit = transaction },
                    onDelete = { selectedTransactionForDelete = transaction }
                )
            }
        }
    }
    
    if (selectedTransactionForEdit != null) {
        EditTransactionDialog(
            transaction = selectedTransactionForEdit!!,
            viewModel = viewModel,
            onDismiss = { selectedTransactionForEdit = null }
        )
    }

    if (selectedTransactionForDelete != null) {
        DeleteTransactionDialog(
            transaction = selectedTransactionForDelete!!,
            viewModel = viewModel,
            onDismiss = { selectedTransactionForDelete = null }
        )
    }
}

data class TransactionItem(
    val id: String,
    val tableId: String,
    val type: String,
    val playerId: String,
    val playerName: String,
    val amount: Long,
    val note: String?,
    val timestamp: Long
)

@Composable
fun TransactionCard(
    transaction: TransactionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showActionDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = Gold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize()
            .combinedClickable(
                onClick = { },
                onLongClick = { showActionDialog = true }
            ),
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
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (transaction.type == "Buy-in") Green80 else Amber80,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = transaction.type,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = transaction.playerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.note != null) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = formatTimestamp(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                text = "+${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == "Buy-in") Green80 else Amber80
            )
        }
    }

    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("Transaction Actions", color = Gold) },
            text = { Text("What would you like to do?", color = Cream) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showActionDialog = false
                        onEdit()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Gold)
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { showActionDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Gold)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showActionDialog = false
                            onDelete()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        )
    }
}

@Composable
fun ResultsTab(
    players: List<Player>,
    buyIns: List<BuyIn>,
    exitRecords: List<ExitRecord>,
    viewModel: TableDetailViewModel,
    tableHasEntryFee: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Calculate results for each player
    val playerResults = remember(players, buyIns, exitRecords) {
        players.map { player ->
            val totalBuyIns = buyIns.filter { it.playerId == player.id }.sumOf { it.amount }
            val totalExits = exitRecords.filter { it.playerId == player.id }.sumOf { it.amount }
            val netResult = totalExits - totalBuyIns
            PlayerResult(
                player = player,
                totalBuyIns = totalBuyIns,
                totalExits = totalExits,
                netResult = netResult
            )
        }
    }
    
    // Check if all players are exited
    val allExited = players.all { it.status == "EXITED" } && players.isNotEmpty()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Share button
        item {
            val uiState by viewModel.uiState.collectAsState()
            
            Button(
                onClick = {
                    val shareText = buildShareText(
                        tableName = uiState.table?.name ?: "Unknown Table",
                        playerResults = playerResults,
                        settlements = calculateSettlement(playerResults),
                        chipValue = uiState.table?.chipValue,
                        totalBuyIns = uiState.totalBuyIns,
                        totalExits = uiState.totalExits,
                        remainingBalance = uiState.remainingBalance
                    )
                    shareText(context, shareText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Color.Black
                )
            ) {
                Text("Share Results")
            }
        }

        // Settlement status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (allExited) Green80.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settlement Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusBadge(
                        status = if (allExited) "Ready" else "Waiting for exits",
                        modifier = Modifier
                    )
                }
            }
        }
        
        // Player results
        items(playerResults) { result ->
            PlayerResultCard(result = result, tableHasEntryFee = tableHasEntryFee)
        }
        
        // Smart settlement plan
        item {
            val uiState by viewModel.uiState.collectAsState()
            
            val settlements = calculateSettlement(playerResults)
            
            SettlementCard(
                settlements = settlements,
                chipValue = uiState.table?.chipValue,
                allExited = allExited
            )
        }
    }
}

data class PlayerResult(
    val player: Player,
    val totalBuyIns: Long,
    val totalExits: Long,
    val netResult: Long
)

@Composable
fun PlayerResultCard(
    result: PlayerResult,
    tableHasEntryFee: Boolean = false
) {
    val resultLabel = when {
        result.netResult > 0 -> "Creditor"
        result.netResult < 0 -> "Debtor"
        else -> "Break-even"
    }
    
    val resultColor = when {
        result.netResult > 0 -> WinGreen
        result.netResult < 0 -> LoseRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
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
                Column {
                    Text(
                        text = result.player.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (tableHasEntryFee) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (result.player.entryFeePaid) "Entry Fee: Paid" else "Entry Fee: Unpaid",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.player.entryFeePaid) WinGreen else LoseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                }
                Surface(
                    color = resultColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = resultLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (result.netResult == 0L) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Buy-ins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${result.totalBuyIns}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green80
                    )
                }
                Column {
                    Text(
                        text = "Exits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${result.totalExits}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Amber80
                    )
                }
                Column {
                    Text(
                        text = "Net Result",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${if (result.netResult > 0) "+" else ""}${result.netResult}",
                        style = MaterialTheme.typography.titleMedium,
                        color = resultColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayerBottomSheet(
    onDismiss: () -> Unit,
    onAddPlayer: (String) -> Unit,
    savedNames: List<String> = emptyList(),
    existingNames: List<String> = emptyList()
) {
    var playerName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val suggestions = savedNames
        .filter { it.contains(playerName, ignoreCase = true) && it.trim() != playerName.trim() }
        .take(8)

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
            SectionHeader(title = "ADD PLAYER", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            // Live Chip-Style Avatar Preview
            Text(
                text = "AVATAR PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            PokerChipAvatar(
                name = playerName.ifBlank { "?" },
                size = 76.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playerName,
                onValueChange = { 
                    playerName = it
                    if (error != null) error = null
                },
                label = { Text("Player Name", color = Cream.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. DANA", color = Cream.copy(alpha = 0.4f)) },
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

            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Previous players:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { name ->
                        SuggestionChip(
                            onClick = { playerName = name },
                            label = { Text(name, color = Cream) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = FeltBackground
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Gold.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = "ADD PLAYER",
                onClick = {
                    val trimmed = playerName.trim()
                    if (trimmed.isBlank()) {
                        error = "Player name is required"
                        return@GoldGradientButton
                    }
                    if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
                        error = "This player is already in the table"
                        return@GoldGradientButton
                    }
                    onAddPlayer(trimmed.uppercase())
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyInBottomSheet(
    playerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit,
    viewModel: TableDetailViewModel,
    playerId: String
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var playerBuyIns by remember { mutableStateOf(0L) }
    var playerExits by remember { mutableStateOf(0L) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(playerId) {
        playerBuyIns = viewModel.getPlayerTotalBuyIns(playerId)
        playerExits = viewModel.getPlayerTotalExits(playerId)
    }

    val currentBal = playerBuyIns - playerExits

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
            SectionHeader(title = "ADD BUY-IN: ${playerName.uppercase()}", suit = "♠")

            Spacer(modifier = Modifier.height(12.dp))

            // Player balance card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PLAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cream.copy(alpha = 0.6f)
                        )
                        Text(
                            text = playerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Cream,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CURRENT BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cream.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$currentBal chips",
                            style = MaterialTheme.typography.titleMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Total Display
            Text(
                text = "TOTAL BUY-IN",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (amount.isNotBlank()) "$amount CHIPS" else "0 CHIPS",
                style = MaterialTheme.typography.headlineMedium,
                color = Gold,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Poker Chip Selector
            Text(
                text = "TAP CHIPS TO ADD",
                style = MaterialTheme.typography.labelSmall,
                color = Cream.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            ChipSelector(
                onAddAmount = { add ->
                    val current = amount.toLongOrNull() ?: 0L
                    amount = (current + add).toString()
                    if (error != null) error = null
                },
                onRemoveLast = {
                    val current = amount.toLongOrNull() ?: 0L
                    val updated = maxOf(0L, current - 100L)
                    amount = if (updated == 0L) "" else updated.toString()
                },
                onClear = {
                    amount = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    amount = it.filter { c -> c.isDigit() }
                    if (error != null) error = null
                },
                label = { Text("Custom Amount", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
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

            GoldGradientButton(
                text = "CONFIRM BUY-IN",
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (amountLong == null || amountLong <= 0) {
                        error = "Amount must be greater than zero"
                        return@GoldGradientButton
                    }
                    onConfirm(amountLong, note.ifBlank { null })
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitBottomSheet(
    playerName: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit,
    viewModel: TableDetailViewModel,
    playerId: String
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var playerBuyIns by remember { mutableStateOf(0L) }
    var playerExits by remember { mutableStateOf(0L) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    LaunchedEffect(playerId) {
        playerBuyIns = viewModel.getPlayerTotalBuyIns(playerId)
        playerExits = viewModel.getPlayerTotalExits(playerId)
    }
    
    val currentBal = playerBuyIns - playerExits
    val exitAmountNum = amount.toLongOrNull() ?: 0L
    val simulatedNet = (playerExits + exitAmountNum) - playerBuyIns

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
            SectionHeader(title = "RECORD EXIT: ${playerName.uppercase()}", suit = "♠")

            Spacer(modifier = Modifier.height(12.dp))

            // Player career & exit summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Buy-ins: $playerBuyIns",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Previous Exits: $playerExits",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Chips: $currentBal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Net: ${if (simulatedNet >= 0) "+" else ""}$simulatedNet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (simulatedNet >= 0) WinGreen else LoseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Exit Display
            Text(
                text = "EXIT AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (amount.isNotBlank()) "$amount CHIPS" else "0 CHIPS",
                style = MaterialTheme.typography.headlineMedium,
                color = if (simulatedNet >= 0) WinGreen else LoseRed,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chip Selector for exit
            ChipSelector(
                onAddAmount = { add ->
                    val current = amount.toLongOrNull() ?: 0L
                    amount = (current + add).toString()
                    if (error != null) error = null
                },
                onRemoveLast = {
                    val current = amount.toLongOrNull() ?: 0L
                    val updated = maxOf(0L, current - 100L)
                    amount = if (updated == 0L) "" else updated.toString()
                },
                onClear = {
                    amount = ""
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    amount = it.filter { c -> c.isDigit() }
                    if (error != null) error = null
                },
                label = { Text("Exit Chip Amount", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
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

            GoldGradientButton(
                text = "SAVE EXIT",
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (amountLong == null || amountLong < 0) {
                        error = "Amount must be zero or positive"
                        return@GoldGradientButton
                    }
                    onConfirm(amountLong, note.ifBlank { null })
                }
            )
        }
    }
}

fun formatAmount(chips: Long, chipValue: Long?): String {
    return if (chipValue != null) {
        "$chips ($${chips * chipValue})"
    } else {
        "$chips"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

data class Settlement(
    val fromPlayer: String,
    val toPlayer: String,
    val amount: Long
)

@Composable
fun SettlementCard(
    settlements: List<Settlement>,
    chipValue: Long?,
    allExited: Boolean
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Settlement Plan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!allExited) {
                Text(
                    text = "Preview - becomes final when all players exit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber80
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (settlements.isEmpty()) {
                Text(
                    text = "No debts. Everyone is settled!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Green80
                )
            } else {
                settlements.forEach { settlement ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = settlement.fromPlayer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Red80,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " pays ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = settlement.toPlayer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Green80,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = if (chipValue != null) {
                                "${settlement.amount} chips ($${settlement.amount * chipValue})"
                            } else {
                                "${settlement.amount} chips"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun calculateSettlement(playerResults: List<PlayerResult>): List<Settlement> {
    val debtors = mutableListOf<Pair<String, Long>>()
    val creditors = mutableListOf<Pair<String, Long>>()
    
    playerResults.forEach { result ->
        when {
            result.netResult < 0 -> debtors.add(Pair(result.player.name, -result.netResult))
            result.netResult > 0 -> creditors.add(Pair(result.player.name, result.netResult))
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

fun buildShareResultsText(
    tableName: String,
    playerResults: List<PlayerResult>,
    settlements: List<Settlement>
): String {
    val sb = StringBuilder()
    sb.appendLine("🃏 $tableName")
    sb.appendLine("Players:")
    val sorted = playerResults.sortedByDescending { it.netResult }
    if (sorted.isEmpty()) {
        sb.appendLine("No players yet")
    } else {
        sorted.forEachIndexed { index, r ->
            val sign = if (r.netResult > 0) "+" else ""
            sb.appendLine("${index + 1}. ${r.player.name}: $sign${r.netResult}")
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

fun buildShareText(

    tableName: String,
    playerResults: List<PlayerResult>,
    settlements: List<Settlement>,
    chipValue: Long?,
    totalBuyIns: Long,
    totalExits: Long,
    remainingBalance: Long
): String {
    val sb = StringBuilder()
    
    // Header
    sb.appendLine("🃏 $tableName - Results")
    sb.appendLine(java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
    sb.appendLine("─────────────")
    
    // Player results
    playerResults.forEach { result ->
        val netText = when {
            result.netResult > 0 -> "+${result.netResult}"
            result.netResult < 0 -> "${result.netResult}"
            else -> "0"
        }
        val status = when {
            result.netResult > 0 -> "Creditor"
            result.netResult < 0 -> "Debtor"
            else -> "Break-even"
        }
        sb.appendLine("${result.player.name}: $netText ($status)")
    }
    
    sb.appendLine("─────────────")
    
    // Settlement plan
    if (settlements.isNotEmpty()) {
        sb.appendLine("Settlement Plan:")
        settlements.forEach { s ->
            val amountText = if (chipValue != null) {
                "${s.amount} chips ($${s.amount * chipValue})"
            } else {
                "${s.amount} chips"
            }
            sb.appendLine("${s.fromPlayer} pays ${s.toPlayer}: $amountText")
        }
        sb.appendLine("─────────────")
    }
    
    // Totals
    sb.appendLine("Total Buy-ins: $totalBuyIns")
    sb.appendLine("Total Exits: $totalExits")
    sb.appendLine("Remaining: $remainingBalance")
    
    return sb.toString()
}

fun shareText(context: android.content.Context, text: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share results")
    context.startActivity(shareIntent)
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionItem,
    viewModel: TableDetailViewModel,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${transaction.type}") },
        text = {
            Column {
                Text(
                    text = "Player: ${transaction.playerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Chip Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20L, 50L, 100L, 200L).forEach { value ->
                        SuggestionChip(
                            onClick = { amount = value.toString() },
                            label = { Text("+$value") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (amountLong == null || amountLong <= 0) {
                        error = "Amount must be greater than zero"
                        return@TextButton
                    }
                    
                    if (transaction.type == "Buy-in") {
                        viewModel.updateBuyIn(
                            BuyIn(
                                id = transaction.id,
                                tableId = transaction.tableId,
                                playerId = transaction.playerId,
                                amount = amountLong,
                                note = note.ifBlank { null },
                                createdAt = transaction.timestamp
                            )
                        )
                    } else {
                        viewModel.updateExitRecord(
                            ExitRecord(
                                id = transaction.id,
                                tableId = transaction.tableId,
                                playerId = transaction.playerId,
                                amount = amountLong,
                                note = note.ifBlank { null },
                                createdAt = transaction.timestamp
                            )
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Gold
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gold)
            }
        }
    )
}

@Composable
fun DeleteTransactionDialog(
    transaction: TransactionItem,
    viewModel: TableDetailViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${transaction.type}?") },
        text = { 
            Text("Are you sure you want to delete this ${transaction.type.lowercase()} for ${transaction.playerName}?")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (transaction.type == "Buy-in") {
                        viewModel.deleteBuyIn(
                            BuyIn(
                                id = transaction.id,
                                tableId = transaction.tableId,
                                playerId = transaction.playerId,
                                amount = transaction.amount,
                                note = transaction.note,
                                createdAt = transaction.timestamp
                            )
                        )
                    } else {
                        viewModel.deleteExitRecord(
                            ExitRecord(
                                id = transaction.id,
                                tableId = transaction.tableId,
                                playerId = transaction.playerId,
                                amount = transaction.amount,
                                note = transaction.note,
                                createdAt = transaction.timestamp
                            ),
                            transaction.playerId
                        )
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (selected) Gold else Gold.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (selected) Gold.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Gold else Cream.copy(alpha = 0.7f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
