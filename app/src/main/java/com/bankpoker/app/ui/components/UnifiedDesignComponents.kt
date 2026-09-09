package com.bankpoker.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.ui.theme.*

/**
 * Unified Card matching Web's .bg-felt-card .border-gold-accent
 */
@Composable
fun UnifiedCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DesignTokens.FeltCard,
    borderColor: Color = DesignTokens.GoldAccent.copy(alpha = 0.35f),
    cornerRadius: Dp = DesignTokens.RadiusLG,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.SpacingLG),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Card(
        modifier = modifier
            .shadow(6.dp, shape)
            .clip(shape)
            .then(clickModifier)
            .border(1.dp, borderColor, shape),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Unified Primary Button with Gold Gradient
 */
@Composable
fun UnifiedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val shape = RoundedCornerShape(DesignTokens.RadiusMD)
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) Brush.horizontalGradient(
                        colors = listOf(DesignTokens.GoldAccent, Color(0xFFE5C158), DesignTokens.GoldDark)
                    ) else Brush.horizontalGradient(
                        colors = listOf(Color.DarkGray, Color.Gray)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Unified Secondary / Outline Button
 */
@Composable
fun UnifiedSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(DesignTokens.RadiusMD)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = DesignTokens.FeltDark.copy(alpha = 0.8f),
            contentColor = DesignTokens.CreamText
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DesignTokens.GoldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = DesignTokens.CreamText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Unified Live Badge with pulsing dot
 */
@Composable
fun UnifiedLiveBadge(
    modifier: Modifier = Modifier,
    text: String = "LIVE"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )

    Row(
        modifier = modifier
            .background(Color(0xFF3F0B0B), RoundedCornerShape(DesignTokens.RadiusFull))
            .border(1.dp, DesignTokens.LiveRed.copy(alpha = 0.7f), RoundedCornerShape(DesignTokens.RadiusFull))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(DesignTokens.LiveRed.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = Color(0xFFFF8A80),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Unified Admin Badge
 */
@Composable
fun UnifiedAdminBadge(
    modifier: Modifier = Modifier,
    text: String = "ADMIN"
) {
    Box(
        modifier = modifier
            .background(DesignTokens.GoldAccent.copy(alpha = 0.15f), RoundedCornerShape(DesignTokens.RadiusFull))
            .border(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(DesignTokens.RadiusFull))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = DesignTokens.GoldAccent,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Unified Rank Badge (1st Gold, 2nd Silver, 3rd Bronze)
 */
@Composable
fun UnifiedRankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (rank) {
        1 -> Triple(DesignTokens.GoldAccent, Color.Black, "1st")
        2 -> Triple(DesignTokens.Silver, Color.Black, "2nd")
        3 -> Triple(DesignTokens.Bronze, Color.White, "3rd")
        else -> Triple(Color(0xFF2E4C3B), DesignTokens.CreamText, "#$rank")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(DesignTokens.RadiusFull))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Unified Modal / Sheet Header
 */
@Composable
fun UnifiedModalHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(DesignTokens.FeltDark, RoundedCornerShape(DesignTokens.RadiusMD))
                        .border(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(DesignTokens.RadiusMD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = DesignTokens.GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    color = DesignTokens.GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = DesignTokens.CreamMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = DesignTokens.CreamMuted
            )
        }
    }
}
