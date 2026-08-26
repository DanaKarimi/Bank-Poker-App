package com.bankpoker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.UnpaidVoroodiInfo
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupDetailViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onTableClick: (String) -> Unit
) {
    val group by viewModel.group.collectAsState(initial = null)
    val tables by viewModel.tables.collectAsState(initial = emptyList())
    val balances by viewModel.balances.collectAsState(initial = emptyList())
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val voroodiDebtors by viewModel.voroodiDebtors.collectAsState(initial = emptyList())


    var showCreateTableDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 3 })

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = group?.name ?: "Group", 
                        color = Gold, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Back",
                            tint = Gold,
                            modifier = Modifier.rotate(45f)
                        )
                    }
                },
                actions = {
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

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> TablesTab(tables = tables, onTableClick = onTableClick)
                        1 -> BalancesTab(balances = balances)
                        2 -> GroupStatsTab(
                            tables = tables,
                            balances = balances,
                            payments = payments,
                            voroodiDebtors = voroodiDebtors,
                            onMarkPaid = { from, to, amount ->
                                viewModel.recordPayment(from, to, amount)
                            },
                            onMarkVoroodiPaid = { playerId ->
                                viewModel.markVoroodiPaid(playerId)
                            }
                        )

                    }
                }
            }
        }
    }

    if (showCreateTableDialog) {
        CreateTableDialog(
            onDismiss = { showCreateTableDialog = false },
            onCreateTable = { name, chipValue, hasEntryFee, entryFee ->
                viewModel.createTable(name, chipValue, hasEntryFee, entryFee)
                showCreateTableDialog = false
            }
        )
    }

    if (showEditDialog) {
        EditGroupNameDialog(
            currentName = group?.name ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                viewModel.updateGroupName(newName)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        DeleteGroupDialog(
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
}




@Composable
fun TablesTab(
    tables: List<PokerTable>,
    onTableClick: (String) -> Unit
) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tables) { table ->
                TableCardSimple(
                    table = table,
                    onClick = { onTableClick(table.id) }
                )
            }
        }
    }
}

@Composable
fun TableCardSimple(
    table: PokerTable,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                                text = "VOROODI",
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
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
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
    payments: List<Payment>,
    voroodiDebtors: List<UnpaidVoroodiInfo> = emptyList(),
    onMarkPaid: (String, String, Long) -> Unit,
    onMarkVoroodiPaid: (String) -> Unit = {}
) {
    val closedCount = tables.count { it.status == "CLOSED" }
    val biggestWinner = balances.maxByOrNull { it.balance }
    val biggestDebtor = balances.minByOrNull { it.balance }
    val totalPaid = payments.sumOf { it.amount }
    val settlements = calculateGroupSettlement(balances)

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
                    .border(1.5.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard)
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
                            color = Gold,
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Total settled so far: $totalPaid chips",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cream.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Voroodi Debtors
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "VOROODI DEBTORS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (voroodiDebtors.isEmpty()) {
                        Text(
                            text = "All voroodi collected!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WinGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        voroodiDebtors.forEach { debtor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = debtor.playerName,
                                        color = Cream,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Table: ${debtor.tableName}",
                                        color = Cream.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Voroodi: ${debtor.amount}",
                                        color = LoseRed,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { onMarkVoroodiPaid(debtor.playerId) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = WinGreen)
                                    ) {
                                        Text(
                                            text = "MARK PAID",
                                            fontWeight = FontWeight.Bold,
                                            color = WinGreen
                                        )
                                    }
                                }
                            }
                        }
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
                        .border(1.dp, WinGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard)
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
                        .border(1.dp, LoseRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard)
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
                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "SETTLEMENT PLAN",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
fun CreateTableDialog(
    onDismiss: () -> Unit,
    onCreateTable: (String, Long?, Boolean, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember { mutableStateOf("") }
    var hasEntryFee by remember { mutableStateOf(false) }
    var entryFeeAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Table", color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Voroodi toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voroodi (Entry Fee)?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Cream
                    )
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
                Spacer(modifier = Modifier.height(8.dp))
                
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                        focusedLabelColor = Gold,
                        cursorColor = Gold
                    )
                )
                
                if (hasEntryFee) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = entryFeeAmount,
                        onValueChange = { entryFeeAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Voroodi amount") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                            focusedLabelColor = Gold,
                            cursorColor = Gold
                        )
                    )
                }
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
                    val entryFeeLong = if (hasEntryFee) {
                        entryFeeAmount.toLongOrNull() ?: chipValueLong ?: 0L
                    } else null
                    onCreateTable(tableName.trim(), chipValueLong, hasEntryFee, entryFeeLong)
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

@Composable
fun EditGroupNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        title = { Text("Edit Group Name", color = Gold, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (error != null) error = null
                    },
                    label = { Text("Group Name") },
                    singleLine = true,
                    isError = error != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                        focusedLabelColor = Gold,
                        cursorColor = Gold,
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream
                    )
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        error = "Group name cannot be empty"
                        return@TextButton
                    }
                    onConfirm(name.trim())
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Gold)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Cream.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun DeleteGroupDialog(
    tablesCount: Int,
    playersCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        title = { Text("Delete Group?", color = LoseRed, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "This permanently deletes the group with its $tablesCount tables and $playersCount players. Cannot be undone.",
                color = Cream,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = LoseRed)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = LoseRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Cream.copy(alpha = 0.7f))
            }
        }
    )
}

