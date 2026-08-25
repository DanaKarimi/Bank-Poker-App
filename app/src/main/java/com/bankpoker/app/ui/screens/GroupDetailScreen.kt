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
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.flow.collectLatest

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

    var showCreateTableDialog by remember { mutableStateOf(false) }
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
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton(
                        text = "TABLES",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    TabButton(
                        text = "BALANCES",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    TabButton(
                        text = "PAYMENTS",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> TablesTab(tables = tables, onTableClick = onTableClick)
                        1 -> BalancesTab(balances = balances)
                        2 -> PaymentsTab(payments = payments)
                    }
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
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Gold.copy(alpha = 0.3f) else Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Gold else Gold.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (selected) Gold else Cream.copy(alpha = 0.7f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp,
            style = MaterialTheme.typography.labelLarge
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
                            Icon(
                                Icons.Default.TableBar,
                                contentDescription = null,
                                tint = Gold,
                                modifier = Modifier.size(20.dp)
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
                StatusBadge(status = table.status)
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
    onCreateTable: (String, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Table", color = Gold, fontWeight = FontWeight.Bold) },
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
