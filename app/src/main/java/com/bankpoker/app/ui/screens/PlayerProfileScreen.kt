package com.bankpoker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.PlayerProfileViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    viewModel: PlayerProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val profileData by viewModel.profileData.collectAsState()
    val avatarColor = AvatarColors[abs(profileData.playerName.hashCode()) % AvatarColors.size]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Player Profile",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground
                )
            )
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Avatar + Player Name
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(88.dp)
                                .border(2.dp, Gold, CircleShape),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(avatarColor, avatarColor.copy(alpha = 0.6f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profileData.playerName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = profileData.playerName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Global Stats Card
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
                                    text = "CAREER STATS",
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
                                HeroStat(
                                    label = "TABLES",
                                    value = "${profileData.tablesPlayed}",
                                    color = Cream
                                )
                                HeroStat(
                                    label = "W / L / D",
                                    value = "${profileData.winsCount} / ${profileData.lossesCount} / ${profileData.breakEvenCount}",
                                    color = Amber80
                                )
                                HeroStat(
                                    label = "NET RESULT",
                                    value = "${if (profileData.netResult >= 0) "+" else ""}${profileData.netResult}",
                                    color = if (profileData.netResult >= 0) WinGreen else LoseRed
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HeroStat(
                                    label = "BIGGEST WIN",
                                    value = "+${profileData.biggestWin}",
                                    color = WinGreen
                                )
                                HeroStat(
                                    label = "BIGGEST LOSS",
                                    value = "${profileData.biggestLoss}",
                                    color = if (profileData.biggestLoss < 0) LoseRed else Cream
                                )
                                HeroStat(
                                    label = "TOTAL BUY-INS",
                                    value = "${profileData.totalBuyIns}",
                                    color = Cream
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                HeroStat(
                                    label = "ENTRY FEES PAID",
                                    value = "${profileData.entryFeesPaidCount}",
                                    color = WinGreen
                                )
                            }
                        }
                    }
                }

                // Recent Games Section Title
                item {
                    Text(
                        text = "RECENT GAMES",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Recent Games List
                if (profileData.games.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FeltCard)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No games recorded yet",
                                    color = Cream.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(profileData.games) { game ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Gold.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = FeltCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = game.tableName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Cream,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!game.groupName.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Gold.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = game.groupName,
                                                    modifier = Modifier.padding(
                                                        horizontal = 6.dp,
                                                        vertical = 2.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Gold,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTimestamp(game.date),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Cream.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Buy-in: ${game.totalBuyIn}  •  Exit: ${game.totalExit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Cream.copy(alpha = 0.6f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (game.netResult >= 0) "+" else ""}${game.netResult}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (game.netResult >= 0) WinGreen else LoseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
