package com.bankpoker.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// =======================================================
// UNIFIED DESIGN SYSTEM TOKENS (Shared Parity with Web)
// =======================================================
object DesignTokens {
    // Brand & Felt Surface Palette
    val FeltGreen = Color(0xFF0B4625)
    val FeltDark = Color(0xFF062815)
    val FeltCard = Color(0xFF0F532D)
    val FeltCardDark = Color(0xFF0A3A20)
    
    // Gold Accent Palette
    val GoldAccent = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFF3E5AB)
    val GoldDark = Color(0xFFAA8C2C)
    
    // Text Palette
    val CreamText = Color(0xFFF5F5DC)
    val CreamMuted = Color(0x99F5F5DC)
    
    // Status & Feedback Palette
    val WinGreen = Color(0xFF4CAF50)
    val LoseRed = Color(0xFFF44336)
    val LiveRed = Color(0xFFE53935)
    
    // Ranks & Trophies
    val Silver = Color(0xFFC0C0C0)
    val Bronze = Color(0xFFCD7F32)

    // Spacing Scale
    val SpacingXS = 4.dp
    val SpacingSM = 8.dp
    val SpacingMD = 12.dp
    val SpacingLG = 16.dp
    val SpacingXL = 20.dp
    val SpacingXXL = 24.dp

    // Corner Radii Scale
    val RadiusSM = 8.dp
    val RadiusMD = 12.dp
    val RadiusLG = 16.dp
    val RadiusXL = 24.dp
    val RadiusFull = 999.dp
}

// Backward Compatibility Aliases for Existing Call Sites
val Green80 = Color(0xFF4CAF50)
val Green40 = Color(0xFF388E3C)
val GreenDark = Color(0xFF1B5E20)
val Red80 = Color(0xFFEF5350)
val Red40 = Color(0xFFD32F2F)
val Amber80 = Color(0xFFFFA726)
val Amber40 = Color(0xFFF57C00)
val PokerBackground = Color(0xFF121212)
val PokerSurface = Color(0xFF1E1E1E)
val PokerSurfaceVariant = Color(0xFF2D2D2D)

// Casino Classic Theme Colors
val FeltBackground = DesignTokens.FeltGreen
val FeltCard = DesignTokens.FeltCard
val FeltCardDark = DesignTokens.FeltCardDark
val FeltDark = DesignTokens.FeltDark
val Gold = DesignTokens.GoldAccent
val GoldLight = DesignTokens.GoldLight
val GoldDark = DesignTokens.GoldDark
val Cream = DesignTokens.CreamText
val CreamMuted = DesignTokens.CreamMuted
val WinGreen = DesignTokens.WinGreen
val LoseRed = DesignTokens.LoseRed
val LiveRed = DesignTokens.LiveRed
val Silver = DesignTokens.Silver
val Bronze = DesignTokens.Bronze

val AvatarColors = listOf(
    Color(0xFFE57373),  // Red
    Color(0xFF64B5F6),  // Blue
    Color(0xFF81C784),  // Green
    Color(0xFFFFD54F),  // Amber
    Color(0xFFBA68C8),  // Purple
    Color(0xFF4FC3F7),  // Light Blue
    Color(0xFFFF8A65),  // Deep Orange
    Color(0xFFAED581),  // Light Green
    Color(0xFFF06292),  // Pink
    Color(0xFF7986CB)   // Indigo
)
