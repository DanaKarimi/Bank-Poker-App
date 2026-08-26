package com.bankpoker.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.ui.theme.*

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    suit: String = "♠"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suit,
            color = Gold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Cream,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Gold.copy(alpha = 0.8f),
                            Gold.copy(alpha = 0.1f)
                        )
                    )
                )
        )
    }
}

@Composable
fun PokerChipButton(
    value: Long,
    label: String = if (value >= 1000) "${value / 1000}K" else "$value",
    chipColor: Color = when {
        value >= 5000 -> Color(0xFFE57373) // Red
        value >= 1000 -> Gold              // Gold
        value >= 500  -> Color(0xFF64B5F6) // Blue
        value >= 100  -> Color(0xFF81C784) // Green
        else          -> Cream
    },
    size: Dp = 56.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .drawBehind {
                val outerRadius = this.size.minDimension / 2f - 1.dp.toPx()
                // Outer gold/colored rim
                drawCircle(
                    color = chipColor,
                    radius = outerRadius,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Inner dashed casino chip ring
                drawCircle(
                    color = Cream.copy(alpha = 0.9f),
                    radius = outerRadius - 4.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                )
            }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        chipColor.copy(alpha = 0.35f),
                        FeltCard
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Cream,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (label.length > 3) 11.sp else 13.sp
        )
    }
}

@Composable
fun ChipSelector(
    onAddAmount: (Long) -> Unit,
    onRemoveLast: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val denominations = listOf(100L, 500L, 1000L, 5000L)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        denominations.forEach { amount ->
            PokerChipButton(
                value = amount,
                onClick = { onAddAmount(amount) }
            )
        }

        // Undo / Minus last chip button
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, LoseRed.copy(alpha = 0.6f), CircleShape)
                .background(FeltCard, CircleShape)
                .clickable(onClick = onRemoveLast),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                color = LoseRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Clear button
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape)
                .background(FeltCard, CircleShape)
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "C",
                color = Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color = Gold,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = Gold.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeltCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 16.sp, color = Gold)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Cream.copy(alpha = 0.65f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GoldGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Color.Black,
            disabledContainerColor = Gold.copy(alpha = 0.3f),
            disabledContentColor = Color.Black.copy(alpha = 0.4f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "♠ ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
