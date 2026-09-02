package com.bankpoker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color.Black,
    secondary = Gold,
    tertiary = Gold,
    background = FeltBackground,
    onBackground = Cream,
    surface = FeltBackground,
    onSurface = Cream,
    surfaceVariant = FeltCard,
    onSurfaceVariant = Cream,
    error = Red80
)

@Composable
fun BankPokerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun CasinoWatermarks() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "♠",
            fontSize = 130.sp,
            color = Color.Black.copy(alpha = 0.04f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-20).dp, y = 40.dp)
                .rotate(-15f)
        )
        Text(
            text = "♥",
            fontSize = 120.sp,
            color = LoseRed.copy(alpha = 0.04f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = 140.dp)
                .rotate(12f)
        )
        Text(
            text = "♣",
            fontSize = 140.sp,
            color = Color.Black.copy(alpha = 0.04f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-15).dp, y = (-80).dp)
                .rotate(20f)
        )
        Text(
            text = "♦",
            fontSize = 125.sp,
            color = LoseRed.copy(alpha = 0.04f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 15.dp, y = (-30).dp)
                .rotate(-10f)
        )
    }
}

@Composable
fun PokerChipAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    avatarColor: Color = AvatarColors[abs(name.hashCode()) % AvatarColors.size]
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "?" }
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val outerRadius = this.size.minDimension / 2f - 1.dp.toPx()
                // Outer gold rim
                drawCircle(
                    color = Gold.copy(alpha = 0.85f),
                    radius = outerRadius,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Inner dashed cream casino chip ring
                drawCircle(
                    color = Cream.copy(alpha = 0.85f),
                    radius = outerRadius - 3.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )
            }
            .padding(4.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(avatarColor, avatarColor.copy(alpha = 0.6f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = if (size >= 70.dp) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

