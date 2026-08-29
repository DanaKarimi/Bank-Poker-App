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
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.bankpoker.app.data.local.entity.PokerTable
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

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        "ACTIVE" -> WinGreen.copy(alpha = 0.15f)
        "CLOSED" -> LoseRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    }
    val textColor = when (status) {
        "ACTIVE" -> WinGreen
        "CLOSED" -> LoseRed
        else -> MaterialTheme.colorScheme.onSecondary
    }
    val borderColor = when (status) {
        "ACTIVE" -> WinGreen
        "CLOSED" -> LoseRed
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier.border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(6.dp)
        ),
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableActionBottomSheet(
    table: PokerTable,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FeltCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Gold) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = table.status)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit Table Action
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismiss()
                        onEditClick()
                    }
                    .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Gold.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Table",
                        tint = Gold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Edit Table",
                        style = MaterialTheme.typography.titleMedium,
                        color = Cream,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete Table Action
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismiss()
                        onDeleteClick()
                    }
                    .border(1.dp, LoseRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = LoseRed.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Table",
                        tint = LoseRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete Table",
                        style = MaterialTheme.typography.titleMedium,
                        color = LoseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTableBottomSheet(
    table: PokerTable,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Boolean, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf(table.name) }
    var chipValue by remember { mutableStateOf(table.chipValue?.toString() ?: "") }
    var hasEntryFee by remember { mutableStateOf(table.hasEntryFee) }
    var entryFeeAmount by remember { mutableStateOf(table.entryFee?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val chipPresets = listOf(5L, 10L, 25L, 50L, 100L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FeltCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Gold) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "EDIT TABLE", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tableName,
                onValueChange = { 
                    tableName = it
                    if (error != null) error = null
                },
                label = { Text("Table Name", color = Cream.copy(alpha = 0.7f)) },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = Gold,
                    focusedContainerColor = FeltBackground,
                    unfocusedContainerColor = FeltBackground
                )
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error!!,
                    color = LoseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chip presets
            Text(
                text = "CHIP VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chipPresets.forEach { preset ->
                    val isSelected = chipValue == preset.toString()
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Gold else Gold.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                chipValue = if (isSelected) "" else preset.toString()
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Gold.copy(alpha = 0.25f) else FeltBackground
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$$preset",
                                color = if (isSelected) Gold else Cream,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entry Fee Toggle Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Entry Fee",
                                style = MaterialTheme.typography.titleMedium,
                                color = Cream,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Require entry fee for this game",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cream.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = hasEntryFee,
                            onCheckedChange = { hasEntryFee = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Gold,
                                checkedTrackColor = Gold.copy(alpha = 0.5f),
                                uncheckedThumbColor = Cream.copy(alpha = 0.5f),
                                uncheckedTrackColor = Gold.copy(alpha = 0.2f)
                            )
                        )
                    }

                    if (hasEntryFee) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = entryFeeAmount,
                            onValueChange = { entryFeeAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Entry Fee Amount", color = Cream.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream,
                                cursorColor = Gold,
                                focusedContainerColor = FeltCard,
                                unfocusedContainerColor = FeltCard
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = "SAVE CHANGES",
                onClick = {
                    if (tableName.isBlank()) {
                        error = "Table name is required"
                        return@GoldGradientButton
                    }
                    val chipValueLong = chipValue.toLongOrNull()
                    val entryFeeLong = if (hasEntryFee) {
                        entryFeeAmount.toLongOrNull() ?: chipValueLong ?: 0L
                    } else null
                    onConfirm(tableName.trim(), chipValueLong, hasEntryFee, entryFeeLong)
                }
            )
        }
    }
}

@Composable
fun DeleteTableConfirmDialog(
    tableName: String,
    playersCount: Int,
    recordsCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        title = {
            Text("Delete Table?", color = Gold, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "This permanently deletes \"$tableName\" with $playersCount players and $recordsCount records. Cannot be undone.",
                color = Cream,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = LoseRed)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Gold)
            }
        }
    )
}

@Composable
fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = Gold,
    textColor: Color = Cream,
    backgroundColor: Color = iconTint.copy(alpha = 0.12f),
    borderColor: Color = iconTint.copy(alpha = 0.5f),
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
