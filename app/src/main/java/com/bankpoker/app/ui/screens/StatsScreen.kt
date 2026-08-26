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
import com.bankpoker.app.data.local.entity.UnpaidVoroodiInfo
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voroodiDebtors by viewModel.unpaidVoroodiDebtors.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats & History", color = Gold, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                        color = WinGreen
                    )
                }
            }

            if (uiState.mostActive != null) {
                item {
                    HighlightCard(
                        title = "Most Active Player",
                        name = uiState.mostActive!!.name,
                        value = "${uiState.mostActive!!.gamesPlayed} games",
                        color = Gold
                    )
                }
            }

            item {
                Text(
                    text = "VOROODI DEBTORS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            if (voroodiDebtors.isEmpty()) {
                item {
                    Text(
                        text = "No unpaid voroodi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WinGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                items(voroodiDebtors) { debtor ->
                    VoroodiDebtorCard(
                        debtor = debtor,
                        onMarkPaid = { viewModel.markVoroodiPaid(debtor.playerId) }
                    )
                }
            }

            item {
                Text(
                    text = "Player History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.playerStats) { stat ->
                PlayerStatCard(stat = stat)
            }

            item {
                Text(
                    text = "Closed Tables",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.closedTableStats.isEmpty()) {
                item {
                    Text(
                        text = "No closed tables yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            items(uiState.closedTableStats) { stat ->
                ClosedTableCard(stat = stat)
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
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
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
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
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
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HighlightCard(
    title: String,
    name: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
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
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun PlayerStatCard(stat: PlayerStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarColor = AvatarColors[stat.name.hashCode().mod(AvatarColors.size).let { if (it < 0) it + AvatarColors.size else it }]
            Surface(
                color = avatarColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.5.dp, Gold, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stat.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stat.gamesPlayed} games",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "${if (stat.netResult > 0) "+" else ""}${stat.netResult}",
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    stat.netResult > 0 -> WinGreen
                    stat.netResult < 0 -> LoseRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stat.table.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatTimestamp(stat.table.closedAt ?: stat.table.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                    color = WinGreen
                )
            }
        }
    }
}

@Composable
fun VoroodiDebtorCard(
    debtor: UnpaidVoroodiInfo,
    onMarkPaid: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = LoseRed.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtor.playerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = if (!debtor.groupName.isNullOrBlank()) {
                    "${debtor.groupName} • ${debtor.tableName}"
                } else {
                    debtor.tableName
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
                    text = "${debtor.amount} chips",
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
                        text = "PAID ✓",
                        fontWeight = FontWeight.Bold,
                        color = WinGreen
                    )
                }
            }
        }
    }
}


