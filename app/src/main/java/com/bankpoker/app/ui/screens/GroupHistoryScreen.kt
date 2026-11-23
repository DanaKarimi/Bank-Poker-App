package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.local.entity.EntryFeeRecord
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.ui.components.*
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupHistoryViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupHistoryScreen(
    viewModel: GroupHistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val group by viewModel.group.collectAsState(initial = null)
    val payments by viewModel.payments.collectAsState(initial = emptyList())
    val entryFeeRecords by viewModel.entryFeeRecords.collectAsState(initial = emptyList())
    val canManageEntryFees by viewModel.canManageEntryFees.collectAsState()

    val socketManager = remember { com.bankpoker.app.data.remote.SocketManager.getInstance(context) }

    LaunchedEffect(socketManager, group?.id, group?.serverId) {
        val sId = group?.serverId ?: group?.id
        if (!sId.isNullOrBlank()) {
            socketManager.joinGroup(sId)
        }
        socketManager.events.collect { event ->
            when (event.event) {
                "entry_fee_updated" -> {
                    val eventGroupId = event.payload?.optString("groupId", "")
                    if (eventGroupId.isNullOrEmpty() || eventGroupId == sId || eventGroupId == group?.id) {
                        viewModel.fetchEntryFeesFromServer()
                    }
                }
                "payment_created", "payment_updated", "payment_deleted" -> {
                    val eventGroupId = event.payload?.optString("groupId", "")
                    if (eventGroupId.isNullOrEmpty() || eventGroupId == sId || eventGroupId == group?.id) {
                        viewModel.fetchPaymentsFromServer()
                    }
                }
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            viewModel.fetchPaymentsFromServer()
        } else if (selectedTab == 1) {
            viewModel.fetchEntryFeesFromServer()
        }
    }

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
                    text = when {
                        selectedTab == 1 && canManageEntryFees -> "Tap a player to expand • Tap badge to toggle • Hold row to edit"
                        selectedTab == 1 -> "Tap a player to view table entry fees"
                        else -> "Hold a row to edit or delete"
                    },
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
                            canManage = canManageEntryFees,
                            onEntryFeeLongClick = { record ->
                                if (canManageEntryFees) {
                                    selectedEntryFeeForAction = record
                                }
                            },
                            onTogglePaid = { record ->
                                viewModel.updateEntryFeeRecord(record.id, record.amount, !record.paid)
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

    // Delete Payment Confirmation Bottom Sheet
    if (selectedPaymentForDelete != null) {
        val payment = selectedPaymentForDelete!!
        DeletePaymentBottomSheet(
            payment = payment,
            onDismiss = { selectedPaymentForDelete = null },
            onConfirm = {
                viewModel.deletePayment(payment.id)
                selectedPaymentForDelete = null
            }
        )
    }

    // Entry Fee Action Sheet
    if (selectedEntryFeeForAction != null && canManageEntryFees) {
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

    // Delete Entry Fee Confirmation Bottom Sheet
    if (selectedEntryFeeForDelete != null) {
        val record = selectedEntryFeeForDelete!!
        DeleteEntryFeeBottomSheet(
            record = record,
            onDismiss = { selectedEntryFeeForDelete = null },
            onConfirm = {
                viewModel.deleteEntryFeeRecord(record.id)
                selectedEntryFeeForDelete = null
            }
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

private data class PlayerEntryFeeGroup(
    val key: String,
    val playerName: String,
    val records: List<EntryFeeRecord>,
    val paidCount: Int,
    val unpaidCount: Int,
    val paidAmount: Long,
    val unpaidAmount: Long
)

private fun formatCurrencyAmount(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@Composable
private fun EntryFeesHistoryTab(
    entryFeeRecords: List<EntryFeeRecord>,
    canManage: Boolean = true,
    onEntryFeeLongClick: (EntryFeeRecord) -> Unit,
    onTogglePaid: (EntryFeeRecord) -> Unit = {}
) {
    var expandedPlayerKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    val groupedPlayers = remember(entryFeeRecords) {
        entryFeeRecords
            .groupBy { it.playerName.trim().lowercase(Locale.US) }
            .map { (normKey, recs) ->
                val displayName = recs.firstOrNull { it.playerName.isNotBlank() }?.playerName?.trim()
                    ?: "Unknown Player"
                // Unpaid tables first, then paid tables; newest first within each
                val sortedRecords = recs.sortedWith(
                    compareBy<EntryFeeRecord> { it.paid }
                        .thenByDescending { it.timestamp }
                )
                val paidRecs = sortedRecords.filter { it.paid }
                val unpaidRecs = sortedRecords.filter { !it.paid }
                PlayerEntryFeeGroup(
                    key = normKey,
                    playerName = displayName,
                    records = sortedRecords,
                    paidCount = paidRecs.size,
                    unpaidCount = unpaidRecs.size,
                    paidAmount = paidRecs.sumOf { it.amount },
                    unpaidAmount = unpaidRecs.sumOf { it.amount }
                )
            }
            .sortedWith(
                compareByDescending<PlayerEntryFeeGroup> { it.unpaidCount > 0 }
                    .thenByDescending { it.unpaidCount }
                    .thenByDescending { it.unpaidAmount }
                    .thenBy { it.playerName.lowercase(Locale.US) }
            )
    }

    if (groupedPlayers.isEmpty()) {
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
            items(groupedPlayers, key = { it.key }) { playerGroup ->
                val isExpanded = expandedPlayerKeys.contains(playerGroup.key)
                PlayerEntryFeeAccordionCard(
                    group = playerGroup,
                    isExpanded = isExpanded,
                    canManage = canManage,
                    onToggleExpand = {
                        expandedPlayerKeys = if (isExpanded) {
                            expandedPlayerKeys - playerGroup.key
                        } else {
                            expandedPlayerKeys + playerGroup.key
                        }
                    },
                    onEntryFeeLongClick = onEntryFeeLongClick,
                    onTogglePaid = onTogglePaid
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerEntryFeeAccordionCard(
    group: PlayerEntryFeeGroup,
    isExpanded: Boolean,
    canManage: Boolean,
    onToggleExpand: () -> Unit,
    onEntryFeeLongClick: (EntryFeeRecord) -> Unit,
    onTogglePaid: (EntryFeeRecord) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val hasUnpaid = group.unpaidCount > 0
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "AccordionChevronRotation"
    )

    val borderColor = if (hasUnpaid) LoseRed.copy(alpha = 0.55f) else Gold.copy(alpha = 0.4f)
    val initial = group.playerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FeltCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed / Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Player Initial Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FeltBackground)
                        .border(1.5.dp, if (hasUnpaid) LoseRed.copy(alpha = 0.7f) else Gold.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Name + Summary Line
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.playerName,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val amountSummary = if (hasUnpaid) {
                        if (group.paidAmount > 0L) {
                            "$${formatCurrencyAmount(group.unpaidAmount)} unpaid ($${formatCurrencyAmount(group.paidAmount)} paid)"
                        } else {
                            "$${formatCurrencyAmount(group.unpaidAmount)} unpaid"
                        }
                    } else {
                        "$${formatCurrencyAmount(group.paidAmount)} paid"
                    }
                    Text(
                        text = "${group.paidCount} Paid • ${group.unpaidCount} Unpaid",
                        color = Cream.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = amountSummary,
                        color = if (hasUnpaid) Color(0xFFFFB74D) else WinGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Pill Badge + Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (hasUnpaid) LoseRed.copy(alpha = 0.2f) else WinGreen.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasUnpaid) LoseRed.copy(alpha = 0.6f) else WinGreen.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = if (hasUnpaid) "${group.unpaidCount} Unpaid" else "All Paid",
                            color = if (hasUnpaid) LoseRed else WinGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Gold,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation)
                    )
                }
            }

            // Expanded Table Records
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FeltBackground.copy(alpha = 0.45f))
                ) {
                    HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 1.dp)
                    group.records.forEachIndexed { index, record ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = Gold.copy(alpha = 0.12f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        if (canManage) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEntryFeeLongClick(record)
                                        }
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.tableName.ifBlank { "Table" },
                                    color = Cream,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatTimestamp(record.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cream.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "$${formatCurrencyAmount(record.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gold,
                                    fontWeight = FontWeight.Bold
                                )

                                if (canManage) {
                                    Surface(
                                        onClick = { onTogglePaid(record) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (record.paid) WinGreen.copy(alpha = 0.2f) else LoseRed.copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (record.paid) WinGreen.copy(alpha = 0.6f) else LoseRed.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text(
                                            text = if (record.paid) "Paid ✓" else "Unpaid ✗",
                                            color = if (record.paid) WinGreen else LoseRed,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (record.paid) WinGreen.copy(alpha = 0.16f) else LoseRed.copy(alpha = 0.16f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (record.paid) WinGreen.copy(alpha = 0.45f) else LoseRed.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Text(
                                            text = if (record.paid) "Paid ✓" else "Unpaid ✗",
                                            color = if (record.paid) WinGreen else LoseRed,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isValid) {
                            onSave(amountLong)
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletePaymentBottomSheet(
    payment: Payment,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(LoseRed.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, LoseRed.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LoseRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DELETE PAYMENT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LoseRed,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure you want to delete this payment of $${payment.amount} between ${payment.fromPlayer} and ${payment.toPlayer}?",
                style = MaterialTheme.typography.bodyMedium,
                color = Cream.copy(alpha = 0.85f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This will reverse balance changes and restore remaining debt.",
                style = MaterialTheme.typography.bodySmall,
                color = Cream.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoseRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (isValid) {
                            onSave(amountLong, isPaid)
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteEntryFeeBottomSheet(
    record: EntryFeeRecord,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(LoseRed.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, LoseRed.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LoseRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DELETE ENTRY FEE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LoseRed,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure you want to remove the entry fee obligation of $${record.amount} for ${record.playerName} (${record.tableName})?",
                style = MaterialTheme.typography.bodyMedium,
                color = Cream.copy(alpha = 0.85f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Cream
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoseRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
