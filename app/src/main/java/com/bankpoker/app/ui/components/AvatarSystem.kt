package com.bankpoker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.ui.theme.Cream
import com.bankpoker.app.ui.theme.FeltCard
import com.bankpoker.app.ui.theme.Gold
import kotlin.math.abs

data class AvatarInfo(
    val id: String,
    val name: String,
    val symbol: String,
    val primaryColor: Color,
    val bgColor: Color
)

val BUNDLED_AVATARS: List<AvatarInfo> = listOf(
    AvatarInfo("avatar_1", "Ace of Spades", "♠", Color(0xFFE2E8F0), Color(0xFF0F172A)),
    AvatarInfo("avatar_2", "King of Hearts", "♥", Color(0xFFEF4444), Color(0xFF450A0A)),
    AvatarInfo("avatar_3", "Queen Diamonds", "♦", Color(0xFF38BDF8), Color(0xFF082F49)),
    AvatarInfo("avatar_4", "Jack of Clubs", "♣", Color(0xFF10B981), Color(0xFF064E3B)),
    AvatarInfo("avatar_5", "Wild Joker", "🃏", Color(0xFFA855F7), Color(0xFF3B0764)),
    AvatarInfo("avatar_6", "Card Shark", "🦈", Color(0xFF06B6D4), Color(0xFF164E63)),
    AvatarInfo("avatar_7", "High Roller", "🐂", Color(0xFFF97316), Color(0xFF7C2D12)),
    AvatarInfo("avatar_8", "Clever Fox", "🦊", Color(0xFFFB923C), Color(0xFF431407)),
    AvatarInfo("avatar_9", "Eagle Eye", "🦅", Color(0xFFFACC15), Color(0xFF422006)),
    AvatarInfo("avatar_10", "Royal Tiger", "🐯", Color(0xFFF59E0B), Color(0xFF451A03)),
    AvatarInfo("avatar_11", "Golden Lion", "🦁", Color(0xFFEAB308), Color(0xFF3B2D05)),
    AvatarInfo("avatar_12", "Mythic Dragon", "🐉", Color(0xFFEC4899), Color(0xFF500724)),
    AvatarInfo("avatar_13", "Grizzly Bear", "🐻", Color(0xFFD97706), Color(0xFF382006)),
    AvatarInfo("avatar_14", "Shadow Wolf", "🐺", Color(0xFF94A3B8), Color(0xFF1E293B)),
    AvatarInfo("avatar_15", "Night Hawk", "🦅", Color(0xFF818CF8), Color(0xFF1E1B4B)),
    AvatarInfo("avatar_16", "The Crown", "👑", Color(0xFFFDE047), Color(0xFF422006)),
    AvatarInfo("avatar_17", "Lucky Dice", "🎲", Color(0xFFF43F5E), Color(0xFF4C0519)),
    AvatarInfo("avatar_18", "All-In Star", "⭐", Color(0xFFFDE047), Color(0xFF451A03)),
    AvatarInfo("avatar_19", "Blazing Flame", "🔥", Color(0xFFFB7185), Color(0xFF7F1D1D)),
    AvatarInfo("avatar_20", "Iron Shield", "🛡️", Color(0xFF64748B), Color(0xFF0F172A)),
    AvatarInfo("avatar_21", "Mind Wizard", "🧙", Color(0xFFC084FC), Color(0xFF3B0764)),
    AvatarInfo("avatar_22", "Royal Knight", "⚔️", Color(0xFF60A5FA), Color(0xFF172554)),
    AvatarInfo("avatar_23", "Lucky Clover", "🍀", Color(0xFF4ADE80), Color(0xFF052E16)),
    AvatarInfo("avatar_24", "The Vault", "🏛️", Color(0xFF2DD4BF), Color(0xFF042F2E))
)

fun getAvatarInfo(avatarId: String?, fallbackName: String = ""): AvatarInfo {
    if (!avatarId.isNullOrBlank()) {
        val found = BUNDLED_AVATARS.find { it.id.equals(avatarId.trim(), ignoreCase = true) }
        if (found != null) return found
    }
    val index = if (fallbackName.isNotBlank()) {
        abs(fallbackName.hashCode()) % BUNDLED_AVATARS.size
    } else {
        0
    }
    return BUNDLED_AVATARS[index]
}

@Composable
fun PokerAvatar(
    avatarId: String?,
    name: String = "",
    size: Dp = 42.dp,
    borderWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier
) {
    val info = getAvatarInfo(avatarId, name)
    val fontSize = (size.value * 0.44f).sp

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val outerRadius = this.size.minDimension / 2f - borderWidth.toPx() / 2f
                // Outer gold rim
                drawCircle(
                    color = Gold.copy(alpha = 0.85f),
                    radius = outerRadius,
                    style = Stroke(width = borderWidth.toPx())
                )
                // Inner dashed ring
                if (size >= 32.dp) {
                    drawCircle(
                        color = Cream.copy(alpha = 0.65f),
                        radius = (outerRadius - 2.5.dp.toPx()).coerceAtLeast(0f),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                    )
                }
            }
            .padding(3.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(info.primaryColor.copy(alpha = 0.25f), info.bgColor)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = info.symbol,
            fontSize = fontSize,
            color = info.primaryColor,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Universal UserBadge complying with the display rule:
 * Avatar + Display Name + small @username below.
 */
@Composable
fun UserBadge(
    displayName: String,
    username: String? = null,
    avatarId: String? = null,
    role: String? = null,
    avatarSize: Dp = 42.dp,
    showUsername: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PokerAvatar(
            avatarId = avatarId,
            name = displayName,
            size = avatarSize
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayName.ifBlank { username ?: "Player" },
                    color = Cream,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (role == "SUPER_ADMIN" || role == "ADMIN") {
                    Surface(
                        color = Gold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold)
                    ) {
                        Text(
                            text = if (role == "SUPER_ADMIN") "SUPER" else "HOST",
                            color = Gold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            if (showUsername && !username.isNullOrBlank() && !username.startsWith("guest_")) {
                Text(
                    text = "@$username",
                    color = Cream.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (showUsername && (username?.startsWith("guest_") == true || role == "GUEST")) {
                Text(
                    text = "Guest",
                    color = Gold.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Avatar Picker Dialog allowing selecting 1 of 24 bundled avatars
 */
@Composable
fun AvatarPickerDialog(
    selectedAvatarId: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        title = {
            Text(
                text = "Choose Your Avatar",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(BUNDLED_AVATARS) { avatar ->
                        val isSelected = avatar.id.equals(selectedAvatarId, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    onAvatarSelected(avatar.id)
                                    onDismiss()
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Gold else Color.White.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                PokerAvatar(
                                    avatarId = avatar.id,
                                    size = 48.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = avatar.name,
                                color = if (isSelected) Gold else Cream.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Cream)
            }
        }
    )
}
