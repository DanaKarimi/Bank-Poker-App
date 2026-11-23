package com.bankpoker.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.dto.LeaderboardPlayerDto
import com.bankpoker.app.data.remote.dto.LeaderboardResponse
import com.bankpoker.app.ui.components.PokerAvatar
import com.bankpoker.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val BumpChartPalette = listOf(
    Color(0xFFF5C542), // Gold
    Color(0xFF34D399), // Emerald
    Color(0xFF60A5FA), // Sky Blue
    Color(0xFFF472B6), // Rose Pink
    Color(0xFFFB923C), // Tangerine
    Color(0xFFA78BFA), // Violet
    Color(0xFF2DD4BF), // Teal
    Color(0xFFF87171), // Coral Red
    Color(0xFFA3E635), // Lime
    Color(0xFF38BDF8), // Cyan
    Color(0xFFE879F9), // Fuchsia
    Color(0xFFFBBF24)  // Amber
)

private fun formatSignedDollars(amount: Long): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(abs(amount))
    return when {
        amount > 0L -> "+$$formatted"
        amount < 0L -> "-$$formatted"
        else -> "$0"
    }
}

@Composable
fun LeaderboardSection(
    data: LeaderboardResponse?,
    isLoading: Boolean
) {
    val snapshots = data?.snapshots ?: emptyList()
    val fullLeaderboard = data?.leaderboard ?: emptyList()

    var filterMode by remember { mutableStateOf("top10") } // "top5" | "top10" | "all"
    var selectedPlayerKey by remember { mutableStateOf<String?>(null) }

    val visiblePlayers = remember(fullLeaderboard, filterMode) {
        when (filterMode) {
            "top5" -> fullLeaderboard.take(5)
            "top10" -> fullLeaderboard.take(10)
            else -> fullLeaderboard
        }
    }

    val colorMap = remember(fullLeaderboard) {
        fullLeaderboard.mapIndexed { idx, p ->
            (p.userId ?: p.playerId.ifBlank { p.name }) to BumpChartPalette[idx % BumpChartPalette.size]
        }.toMap()
    }

    if (isLoading && fullLeaderboard.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
        return
    }

    if (snapshots.isEmpty() || fullLeaderboard.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No Closed Tables Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Close at least one table in this group to track all-time standings and rank progression.",
                style = MaterialTheme.typography.bodySmall,
                color = Cream.copy(alpha = 0.65f)
            )
        }
        return
    }

    val maxRankInChart = remember(visiblePlayers) {
        var m = max(visiblePlayers.size, 1)
        visiblePlayers.forEach { p ->
            p.history.forEach { h -> if (h.rank > m) m = h.rank }
        }
        m
    }

    val selectedPlayer = remember(fullLeaderboard, selectedPlayerKey) {
        if (selectedPlayerKey == null) null
        else fullLeaderboard.find { (it.userId ?: it.playerId.ifBlank { it.name }) == selectedPlayerKey }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SECTION 1: RANK PROGRESSION BUMP CHART CARD
        item(key = "bump_chart_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RANK PROGRESSION",
                                color = Gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Across ${snapshots.size} closed ${if (snapshots.size == 1) "table" else "tables"}",
                                color = Cream.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("top5" to "Top 5", "top10" to "Top 10", "all" to "All").forEach { (id, label) ->
                                val active = filterMode == id
                                Surface(
                                    onClick = { filterMode = id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) Gold else FeltBackground,
                                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) Color.Black else Cream.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tappable Player Chips (Horizontal Scroll)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visiblePlayers.forEach { p ->
                            val key = p.userId ?: p.playerId.ifBlank { p.name }
                            val color = colorMap[key] ?: Gold
                            val isSelected = selectedPlayerKey == key
                            Surface(
                                onClick = {
                                    selectedPlayerKey = if (isSelected) null else key
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) FeltBackground else FeltBackground.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) color else Gold.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = "#${p.rank} ${p.name}",
                                        color = Cream,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Bump Chart Canvas inside Horizontally Scrollable Box
                    val colWidthDp = 68.dp
                    val rowHeightDp = if (maxRankInChart <= 6) 34.dp else 26.dp
                    val chartWidthDp = maxOf(300, snapshots.size * 68 + 64).dp
                    val chartHeightDp = maxOf(160, (maxRankInChart + 1) * (if (maxRankInChart <= 6) 34 else 26) + 24).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FeltBackground.copy(alpha = 0.7f))
                            .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Y-axis rank labels
                                Column(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(chartHeightDp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (r in 1..maxRankInChart) {
                                        Text(
                                            text = "#$r",
                                            color = when (r) {
                                                1 -> Gold
                                                2 -> Color(0xFFC0C0C0)
                                                3 -> Color(0xFFCD7F32)
                                                else -> Cream.copy(alpha = 0.55f)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Canvas(
                                    modifier = Modifier
                                        .width(chartWidthDp)
                                        .height(chartHeightDp)
                                ) {
                                    val padX = 24f
                                    val padY = 18f
                                    val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                                    val usableH = (size.height - padY * 2).coerceAtLeast(1f)
                                    val xStep = if (snapshots.size <= 1) 0f else usableW / (snapshots.size - 1)
                                    val yStep = if (maxRankInChart <= 1) 0f else usableH / (maxRankInChart - 1)

                                    fun xFor(idx: Int): Float =
                                        if (snapshots.size <= 1) size.width / 2f else padX + idx * xStep

                                    fun yFor(rank: Int): Float =
                                        if (maxRankInChart <= 1) size.height / 2f else padY + (rank - 1) * yStep

                                    // Horizontal rank gridlines
                                    for (r in 1..maxRankInChart) {
                                        val y = yFor(r)
                                        drawLine(
                                            color = Gold.copy(alpha = 0.14f),
                                            start = Offset(padX, y),
                                            end = Offset(size.width - padX, y),
                                            strokeWidth = 1.5f
                                        )
                                    }

                                    // Draw player trajectory curves
                                    visiblePlayers.forEach { p ->
                                        val key = p.userId ?: p.playerId.ifBlank { p.name }
                                        val baseColor = colorMap[key] ?: Gold
                                        val isSelected = selectedPlayerKey == key
                                        val alpha = if (selectedPlayerKey == null || isSelected) 1f else 0.18f
                                        val strokeW = if (isSelected) 7f else 4.5f
                                        val color = baseColor.copy(alpha = alpha)

                                        val pts = p.history.map { Offset(xFor(it.snapshotIndex), yFor(it.rank)) }
                                        if (pts.size >= 2) {
                                            val path = Path().apply {
                                                moveTo(pts[0].x, pts[0].y)
                                                for (i in 1 until pts.size) {
                                                    val prev = pts[i - 1]
                                                    val curr = pts[i]
                                                    val midX = (prev.x + curr.x) / 2f
                                                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                                }
                                            }
                                            drawPath(
                                                path = path,
                                                color = color,
                                                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                            )
                                        }

                                        pts.forEachIndexed { idx, pt ->
                                            val played = p.history[idx].sessionNet != null
                                            drawCircle(
                                                color = if (played) color else FeltBackground,
                                                radius = if (isSelected) 9f else 6.5f,
                                                center = pt
                                            )
                                            if (!played) {
                                                drawCircle(
                                                    color = color,
                                                    radius = if (isSelected) 9f else 6.5f,
                                                    center = pt,
                                                    style = Stroke(width = 2.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // X-axis date labels
                            Row(
                                modifier = Modifier
                                    .padding(start = 28.dp, top = 4.dp)
                                    .width(chartWidthDp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                snapshots.forEach { snap ->
                                    Text(
                                        text = snap.dateLabel.ifBlank { "#${snap.index + 1}" },
                                        color = Cream.copy(alpha = 0.65f),
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Selected Player Detail Card
                    if (selectedPlayer != null) {
                        val lastPt = selectedPlayer.history.lastOrNull()
                        Surface(
                            color = FeltBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedPlayer.name} • Rank #${selectedPlayer.rank}",
                                        color = Gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (lastPt != null) {
                                        Text(
                                            text = "Latest: ${lastPt.dateLabel} (${lastPt.tableName})",
                                            color = Cream.copy(alpha = 0.65f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatSignedDollars(selectedPlayer.cumulativeNet),
                                        color = if (selectedPlayer.cumulativeNet >= 0) WinGreen else LoseRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Best: ${formatSignedDollars(selectedPlayer.bestSessionNet)}",
                                        color = Cream.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: ALL-TIME STANDINGS HEADER
        item(key = "standings_header") {
            Text(
                text = "ALL-TIME STANDINGS (${fullLeaderboard.size})",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // STANDINGS CARDS
        items(fullLeaderboard, key = { it.userId ?: it.playerId.ifBlank { it.name } }) { player ->
            val key = player.userId ?: player.playerId.ifBlank { player.name }
            val curveColor = colorMap[key] ?: Gold
            val isSelected = selectedPlayerKey == key

            LeaderboardStandingCard(
                player = player,
                curveColor = curveColor,
                isSelected = isSelected,
                onClick = { selectedPlayerKey = if (isSelected) null else key }
            )
        }
    }
}

@Composable
private fun LeaderboardStandingCard(
    player: LeaderboardPlayerDto,
    curveColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rankBg = when (player.rank) {
        1 -> Gold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> FeltBackground
    }
    val rankFg = if (player.rank <= 3) Color.Black else Gold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Gold else Gold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeltCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rank Pill + Delta
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rankBg
                ) {
                    Text(
                        text = "#${player.rank}",
                        color = rankFg,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val deltaText = when {
                    player.rankDelta > 0 -> "▲${player.rankDelta}"
                    player.rankDelta < 0 -> "▼${abs(player.rankDelta)}"
                    else -> "—"
                }
                val deltaColor = when {
                    player.rankDelta > 0 -> WinGreen
                    player.rankDelta < 0 -> LoseRed
                    else -> Cream.copy(alpha = 0.45f)
                }
                Text(
                    text = deltaText,
                    color = deltaColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // Avatar + Curve Color Dot
            Box {
                PokerAvatar(
                    avatarId = player.avatarId ?: "avatar_1",
                    name = player.name,
                    size = 38.dp
                )
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(curveColor)
                        .border(1.5.dp, FeltBackground, CircleShape)
                )
            }

            // Name & Stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${player.tablesPlayed} tables • ${player.winRate}% win • Best ${formatSignedDollars(player.bestSessionNet)}",
                    color = Cream.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Cumulative Net
            Text(
                text = formatSignedDollars(player.cumulativeNet),
                color = when {
                    player.cumulativeNet > 0L -> WinGreen
                    player.cumulativeNet < 0L -> LoseRed
                    else -> Cream
                },
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}
