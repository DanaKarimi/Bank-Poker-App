package com.bankpoker.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.EntryFeeRecord
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.ui.components.*
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupHistoryScreen(
    viewModel: GroupHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val group by viewModel.group.collectAsState(initial = null)
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val entryFeeRecords by viewModel.entryFeeRecords.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedPaymentForAction by remember { mutableStateOf<Payment?>(null) }
    var selectedPaymentForEdit by remember { mutableStateOf<Payment?>(null) }
    var selectedPaymentForDelete by remember { mutableStateOf<Payment?>(null) }

    var selectedEntryFeeForAction by remember { mutableStateOf<EntryFeeRecord?>(null) }
    var selectedEntryFeeForEdit by remember { mutableStateOf<EntryFeeRecord?>(null) }
    var selectedEntryFeeForDelete by remember { mutableStateOf<EntryFeeRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (group != null) "${group?.name} History" else "History",
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
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
            CasinoWatermarks()

            Column(modifier = Modifier.fillMaxSize()) {
                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = "PAYMENTS",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "ENTRY FEES",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hint
                Text(
                    text = "Hold a row to edit or delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "GroupHistoryTabAnimation",
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> PaymentsHistoryTab(
                            payments = payments,
                            onPaymentLongClick = { payment ->
                                selectedPaymentForAction = payment
                            }
                        )
                        1 -> EntryFeesHistoryTab(
                            entryFeeRecords = entryFeeRecords,
                            onEntryFeeLongClick = { record ->
                                selectedEntryFeeForAction = record
                            }
                        )
                    }
                }
            }
        }
    }

    // Payment Action Sheet
    if (selectedPaymentForAction != null) {
        val payment = selectedPaymentForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedPaymentForAction = null },
            containerColor = FeltCard,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.6f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${payment.fromPlayer} → ${payment.toPlayer}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cream
                    )
                    Text(
                        text = "$${payment.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionRow(
                    icon = Icons.Default.Edit,
                    label = "Edit Payment",
                    iconTint = Gold,
                    onClick = {
                        val p = selectedPaymentForAction
                        selectedPaymentForAction = null
                        selectedPaymentForEdit = p
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete Payment",
                    iconTint = LoseRed,
                    textColor = LoseRed,
                    onClick = {
                        val p = selectedPaymentForAction
                        selectedPaymentForAction = null
                        selectedPaymentForDelete = p
                    }
                )
            }
        }
    }

    // Edit Payment Sheet
    if (selectedPaymentForEdit != null) {
        val payment = selectedPaymentForEdit!!
        EditPaymentBottomSheet(
            payment = payment,
            onDismiss = { selectedPaymentForEdit = null },
            onSave = { newAmount ->
                viewModel.updatePayment(payment.id, newAmount)
                selectedPaymentForEdit = null
            }
        )
    }

    // Delete Payment Confirmation Dialog
    if (selectedPaymentForDelete != null) {
        val payment = selectedPaymentForDelete!!
        AlertDialog(
            onDismissRequest = { selectedPaymentForDelete = null },
            title = {
                Text(
                    text = "Delete Payment?",
                    color = LoseRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Deleting this payment will reverse the balance changes ($${payment.amount} between ${payment.fromPlayer} and ${payment.toPlayer}) and restore the remaining debt.",
                    color = Cream
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(payment.id)
                        selectedPaymentForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoseRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("REVERSE & DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedPaymentForDelete = null }
                ) {
                    Text("CANCEL", color = Cream)
                }
            },
            containerColor = FeltCard,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Entry Fee Action Sheet
    if (selectedEntryFeeForAction != null) {
        val record = selectedEntryFeeForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedEntryFeeForAction = null },
            containerColor = FeltCard,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.6f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = record.playerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Cream
                        )
                        Text(
                            text = "Table: ${record.tableName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "$${record.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionRow(
                    icon = Icons.Default.Edit,
                    label = "Edit Entry Fee",
                    iconTint = Gold,
                    onClick = {
                        val ef = selectedEntryFeeForAction
                        selectedEntryFeeForAction = null
                        selectedEntryFeeForEdit = ef
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete Entry Fee",
                    iconTint = LoseRed,
                    textColor = LoseRed,
                    onClick = {
                        val ef = selectedEntryFeeForAction
                        selectedEntryFeeForAction = null
                        selectedEntryFeeForDelete = ef
                    }
                )
            }
        }
    }

    // Edit Entry Fee Sheet
    if (selectedEntryFeeForEdit != null) {
        val record = selectedEntryFeeForEdit!!
        EditEntryFeeBottomSheet(
            record = record,
            onDismiss = { selectedEntryFeeForEdit = null },
            onSave = { newAmount, isPaid ->
                viewModel.updateEntryFeeRecord(record.id, newAmount, isPaid)
                selectedEntryFeeForEdit = null
            }
        )
    }

    // Delete Entry Fee Confirmation Dialog
    if (selectedEntryFeeForDelete != null) {
        val record = selectedEntryFeeForDelete!!
        AlertDialog(
            onDismissRequest = { selectedEntryFeeForDelete = null },
            title = {
                Text(
                    text = "Delete Entry Fee?",
                    color = LoseRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove the entry fee obligation of $${record.amount} for ${record.playerName} (${record.tableName})?",
                    color = Cream
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntryFeeRecord(record.id)
                        selectedEntryFeeForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoseRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedEntryFeeForDelete = null }
                ) {
                    Text("CANCEL", color = Cream)
                }
            },
            containerColor = FeltCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PaymentsHistoryTab(
    payments: List<Payment>,
    onPaymentLongClick: (Payment) -> Unit
) {
    if (payments.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "No payment history yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Cream.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(payments, key = { it.id }) { payment ->
                PaymentHistoryCard(
                    payment = payment,
                    onLongClick = { onPaymentLongClick(payment) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaymentHistoryCard(
    payment: Payment,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "PaymentCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
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
                        text = payment.fromPlayer,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = " → ",
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = payment.toPlayer,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(payment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.4f)
                )
            }
            Text(
                text = "$${payment.amount}",
                style = MaterialTheme.typography.titleMedium,
                color = Gold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EntryFeesHistoryTab(
    entryFeeRecords: List<EntryFeeRecord>,
    onEntryFeeLongClick: (EntryFeeRecord) -> Unit
) {
    if (entryFeeRecords.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "No entry fee history yet",
                style = MaterialTheme.typography.bodyLarge,
                color = Cream.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entryFeeRecords, key = { it.id }) { record ->
                EntryFeeHistoryCard(
                    record = record,
                    onLongClick = { onEntryFeeLongClick(record) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryFeeHistoryCard(
    record: EntryFeeRecord,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "EntryFeeCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                color = Gold.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = record.playerName,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Table: ${record.tableName}",
                    color = Cream.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatTimestamp(record.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.4f)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$${record.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (record.paid) {
                    Text(
                        text = "Paid ✓",
                        color = WinGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Unpaid ✗",
                        color = LoseRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPaymentBottomSheet(
    payment: Payment,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amountText by remember { mutableStateOf(payment.amount.toString()) }
    val amountLong = amountText.toLongOrNull() ?: 0L
    val isValid = amountLong > 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "EDIT PAYMENT", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            // Fixed Payer & Receiver
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Payer (From): ${payment.fromPlayer}",
                        color = Cream,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Receiver (To): ${payment.toPlayer}",
                        color = Cream,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("Amount", color = Gold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                    cursorColor = Gold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = "SAVE CHANGES",
                onClick = {
                    if (isValid) {
                        onSave(amountLong)
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryFeeBottomSheet(
    record: EntryFeeRecord,
    onDismiss: () -> Unit,
    onSave: (amount: Long, paid: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf(record.amount.toString()) }
    var isPaid by remember { mutableStateOf(record.paid) }
    val amountLong = amountText.toLongOrNull() ?: 0L
    val isValid = amountLong >= 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FeltCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "EDIT ENTRY FEE", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FeltBackground.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Player: ${record.playerName}",
                        color = Cream,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Table: ${record.tableName}",
                        color = Cream.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() }) {
                        amountText = input
                    }
                },
                label = { Text("Amount", color = Gold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                    cursorColor = Gold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Paid switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paid Status",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cream,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isPaid,
                    onCheckedChange = { isPaid = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold,
                        checkedTrackColor = WinGreen,
                        uncheckedThumbColor = Cream,
                        uncheckedTrackColor = FeltBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = "SAVE CHANGES",
                onClick = {
                    if (isValid) {
                        onSave(amountLong, isPaid)
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
