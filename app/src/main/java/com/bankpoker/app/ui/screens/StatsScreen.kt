package com.bankpoker.app.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.ui.theme.Amber80
import com.bankpoker.app.ui.theme.AvatarColors
import com.bankpoker.app.ui.theme.Cream
import com.bankpoker.app.ui.theme.FeltBackground
import com.bankpoker.app.ui.theme.FeltCard
import com.bankpoker.app.ui.theme.Gold
import com.bankpoker.app.ui.theme.Green80
import com.bankpoker.app.ui.theme.Red80
import com.bankpoker.app.ui.theme.WinGreen
import com.bankpoker.app.ui.theme.LoseRed
import com.bankpoker.app.viewmodel.PlayerStats
import com.bankpoker.app.viewmodel.StatsUiState
import com.bankpoker.app.viewmodel.StatsViewModel
import com.bankpoker.app.viewmodel.TableStats

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.bankpoker.app.ui.theme.CasinoWatermarks
import com.bankpoker.app.ui.theme.PokerChipAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit,
    onPlayerClick: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val entryFeeDebtors by viewModel.unpaidEntryFeeDebtors.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stats & History", 
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground,
                    titleContentColor = Gold,
                    navigationIconContentColor = Gold
                )
            )
        }
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { OverviewCard(uiState) }

                if (uiState.biggestWinner != null) {
                    item {
                        HighlightCard(
                            title = "Biggest Winner",
                            name = uiState.biggestWinner!!.name,
                            value = "+${uiState.biggestWinner!!.netResult}",
                            color = WinGreen,
                            onClick = if (onPlayerClick != null) { { onPlayerClick(uiState.biggestWinner!!.name) } } else null
                        )
                    }
                }

                if (uiState.mostActive != null) {
                    item {
                        HighlightCard(
                            title = "Most Active Player",
                            name = uiState.mostActive!!.name,
                            value = "${uiState.mostActive!!.gamesPlayed} games",
                            color = Gold,
                            onClick = if (onPlayerClick != null) { { onPlayerClick(uiState.mostActive!!.name) } } else null
                        )
                    }
                }

                item {
                    Text(
                        text = "ENTRY FEE DEBTORS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                if (entryFeeDebtors.isEmpty()) {
                    item {
                        Text(
                            text = "All entry fees collected!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WinGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    items(entryFeeDebtors) { debtor ->
                        EntryFeeDebtorCard(
                            debtor = debtor,
                            onMarkPaid = { viewModel.markEntryFeePaid(debtor.playerId) }
                        )
                    }
                }

                item {
                    Text(
                        text = "PLAYER HISTORY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                items(uiState.playerStats) { stat ->
                    PlayerStatCard(
                        stat = stat,
                        onClick = if (onPlayerClick != null) { { onPlayerClick(stat.name) } } else null
                    )
                }

                item {
                    Text(
                        text = "CLOSED TABLES",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                if (uiState.closedTableStats.isEmpty()) {
                    item {
                        Text(
                            text = "No closed tables yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cream.copy(alpha = 0.6f)
                        )
                    }
                }

                items(uiState.closedTableStats) { stat ->
                    ClosedTableCard(stat = stat)
                }
            }
        }
    }
}

@Composable
fun OverviewCard(uiState: StatsUiState) {
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
                    text = "OVERVIEW",
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
                StatColumn("Tables", uiState.totalTables.toString())
                StatColumn("Closed", uiState.closedTables.toString())
                StatColumn("Players", uiState.distinctPlayers.toString())
                StatColumn("Trans.", uiState.totalTransactions.toString())
            }
        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
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
            style = MaterialTheme.typography.titleMedium,
            color = Gold,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HighlightCard(
    title: String,
    name: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = color.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerStatCard(
    stat: PlayerStats,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = Gold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokerChipAvatar(name = stat.name, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stat.gamesPlayed} games",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "${if (stat.netResult > 0) "+" else ""}${stat.netResult}",
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    stat.netResult > 0 -> WinGreen
                    stat.netResult < 0 -> LoseRed
                    else -> Cream
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun ClosedTableCard(stat: TableStats) {
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
                Text(
                    text = stat.table.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(stat.table.closedAt ?: stat.table.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stat.playerCount} players",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.7f)
                )
                Text(
                    text = "Buy-ins: ${stat.totalBuyIns}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WinGreen
                )
            }
            if (stat.topWinnerName != null && stat.topWinnerNet > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Winner: ${stat.topWinnerName} (+${stat.topWinnerNet})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WinGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EntryFeeDebtorCard(
    debtor: UnpaidEntryFeeInfo,
    onMarkPaid: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = LoseRed.copy(alpha = 0.7f),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (!debtor.groupName.isNullOrBlank()) {
                    "${debtor.groupName} • Table: ${debtor.tableName}"
                } else {
                    "Table: ${debtor.tableName}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimestamp(debtor.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.4f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Entry Fee: ${debtor.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = LoseRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onMarkPaid,
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
