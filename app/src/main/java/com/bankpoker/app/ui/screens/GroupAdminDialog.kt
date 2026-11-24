package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bankpoker.app.data.remote.dto.GroupAdminOverviewResponse
import com.bankpoker.app.data.remote.dto.GroupAdminTableDetailResponse
import com.bankpoker.app.data.remote.dto.GroupAdminTableTxDto
import com.bankpoker.app.data.remote.dto.GroupMemberDto
import com.bankpoker.app.ui.components.PokerAvatar
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupDetailViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private fun formatAdminMoney(amount: Long): String {
    val f = NumberFormat.getNumberInstance(Locale.US).format(abs(amount))
    return when {
        amount > 0L -> "+$$f"
        amount < 0L -> "-$$f"
        else -> "$0"
    }
}

@Composable
fun GroupAdminDialog(
    viewModel: GroupDetailViewModel,
    groupName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val overview by viewModel.adminOverview.collectAsState()
    val isLoading by viewModel.isLoadingAdmin.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Members, 1: Tables, 2: Payments & Resets
    var statusBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) } // isError to msg

    // Member actions
    var memberToRemove by remember { mutableStateOf<GroupMemberDto?>(null) }
    var memberToReset by remember { mutableStateOf<GroupMemberDto?>(null) }
    var tempPassword by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }

    // Table correction sub-view
    var editingTableId by remember { mutableStateOf<String?>(null) }
    var tableDetail by remember { mutableStateOf<GroupAdminTableDetailResponse?>(null) }
    var editingTx by remember { mutableStateOf<Pair<Boolean, GroupAdminTableTxDto>?>(null) } // isBuyIn to tx
    var editTxAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchAdminOverview()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = FeltCard,
            border = BorderStroke(2.dp, Gold)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FeltBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GROUP ADMIN",
                            color = Gold,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = groupName,
                            color = Cream.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Cream)
                    }
                }

                // 2 Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Members", "Tables").forEachIndexed { idx, title ->
                        val active = selectedTab == idx
                        Surface(
                            onClick = {
                                selectedTab = idx
                                editingTableId = null
                                tableDetail = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) Gold else FeltBackground,
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (active) Color.Black else Cream,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Status Banner
                statusBanner?.let { (isErr, msg) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isErr) LoseRed.copy(alpha = 0.2f) else WinGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (isErr) LoseRed else WinGreen)
                    ) {
                        Text(
                            text = msg,
                            color = if (isErr) LoseRed else WinGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isLoading && overview == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            val members = overview?.members ?: emptyList()
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(members, key = { it.userId }) { m ->
                                    val unsettled = abs(m.balance) > 0L
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = FeltBackground.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                PokerAvatar(avatarId = m.avatarId ?: "avatar_1", name = m.displayName, size = 38.dp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(m.displayName, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("@${m.username} • ${m.role}", color = Cream.copy(alpha = 0.6f), fontSize = 11.sp)
                                                }
                                                Text(
                                                    text = formatAdminMoney(m.balance),
                                                    color = when {
                                                        m.balance > 0L -> WinGreen
                                                        m.balance < 0L -> LoseRed
                                                        else -> Cream
                                                    },
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (!m.isOwner) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val nextRole = if (m.role == "ADMIN") "MEMBER" else "ADMIN"
                                                            viewModel.updateGroupMemberRole(m.userId, nextRole) { res ->
                                                                statusBanner = if (res.isSuccess) false to "${m.displayName} is now $nextRole"
                                                                else true to (res.exceptionOrNull()?.message ?: "Role update failed")
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text(if (m.role == "ADMIN") "Make Member" else "Make Admin", color = Gold, fontSize = 11.sp)
                                                    }
                                                }
                                                if (!m.isOwner) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (unsettled) {
                                                                statusBanner = true to "Cannot remove ${m.displayName}: balance is ${formatAdminMoney(m.balance)}. Settle to $0 first."
                                                            } else {
                                                                memberToRemove = m
                                                            }
                                                        },
                                                        border = BorderStroke(1.dp, if (unsettled) Color(0xFFFFB74D) else LoseRed),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("Remove", color = if (unsettled) Color(0xFFFFB74D) else LoseRed, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            if (editingTableId != null && tableDetail != null) {
                                val detail = tableDetail!!
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { editingTableId = null; tableDetail = null }) {
                                                Text("← Back to Tables", color = Gold, fontWeight = FontWeight.Bold)
                                            }
                                            val tbl = detail.table
                                            if (tbl != null) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val nextSt = if (tbl.status == "CLOSED") "ACTIVE" else "CLOSED"
                                                        viewModel.updateAdminTableStatus(tbl.id, status = nextSt) { res ->
                                                            if (res.isSuccess) {
                                                                statusBanner = false to "Table marked $nextSt"
                                                                viewModel.loadAdminTableDetail(tbl.id) { td -> tableDetail = td }
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text(if (tbl.status == "CLOSED") "Reopen" else "Close", color = Gold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                    item { Text("BUY-INS (${detail.buyIns.size})", color = Gold, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                                    items(detail.buyIns, key = { "bi_${it.id}" }) { tx ->
                                        AdminTxRow(
                                            tx = tx,
                                            amountColor = WinGreen,
                                            onEdit = { editingTx = true to tx; editTxAmount = tx.amount.toString() },
                                            onDelete = {
                                                viewModel.deleteAdminTableTx(editingTableId!!, true, tx.id) { res ->
                                                    if (res.isSuccess) viewModel.loadAdminTableDetail(editingTableId!!) { td -> tableDetail = td }
                                                }
                                            }
                                        )
                                    }
                                    item { Text("EXITS (${detail.exits.size})", color = Gold, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                                    items(detail.exits, key = { "ex_${it.id}" }) { tx ->
                                        AdminTxRow(
                                            tx = tx,
                                            amountColor = Color(0xFFFFB74D),
                                            onEdit = { editingTx = false to tx; editTxAmount = tx.amount.toString() },
                                            onDelete = {
                                                viewModel.deleteAdminTableTx(editingTableId!!, false, tx.id) { res ->
                                                    if (res.isSuccess) viewModel.loadAdminTableDetail(editingTableId!!) { td -> tableDetail = td }
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                val tables = overview?.tables ?: emptyList()
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(tables, key = { it.id }) { t ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = FeltBackground.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f)),
                                            modifier = Modifier.clickable {
                                                editingTableId = t.id
                                                viewModel.loadAdminTableDetail(t.id) { td -> tableDetail = td }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("${t.name} (${t.status})", color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(
                                                        text = "${t.playerCount} players • BI $${t.totalBuyIns} • EX $${t.totalExits}",
                                                        color = Cream.copy(alpha = 0.6f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text("Edit →", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
    }

    // Confirm Remove Member Dialog
    memberToRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            containerColor = FeltCard,
            title = { Text("Remove ${m.displayName}?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Remove ${m.displayName} (@${m.username}) from this group? Their balance is $0 and all historical table records will remain intact.",
                    color = Cream
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = m
                        memberToRemove = null
                        viewModel.removeGroupMember(target.userId) { res ->
                            statusBanner = if (res.isSuccess) false to "${target.displayName} removed from group"
                            else true to (res.exceptionOrNull()?.message ?: "Failed to remove member")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoseRed, contentColor = Color.White)
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) { Text("Cancel", color = Cream) }
            }
        )
    }

    // Reset Password Dialog
    memberToReset?.let { m ->
        AlertDialog(
            onDismissRequest = { memberToReset = null },
            containerColor = FeltCard,
            title = { Text("Reset Password: ${m.username}", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!generatedCode.isNullOrBlank()) {
                        Text(
                            text = "Code (24h): $generatedCode",
                            color = Gold,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.resetMemberPassword(m.userId, generateCode = true) { res ->
                                if (res.isSuccess) {
                                    generatedCode = res.getOrNull()?.resetCode
                                    viewModel.fetchAdminOverview()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Generate 6-Char Reset Code", fontWeight = FontWeight.Bold) }

                    OutlinedTextField(
                        value = tempPassword,
                        onValueChange = { tempPassword = it },
                        label = { Text("Or set temp password (6+ chars)", color = Cream) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.resetMemberPassword(m.userId, newPassword = tempPassword, generateCode = false) { res ->
                                if (res.isSuccess) {
                                    statusBanner = false to "Password updated for ${m.username}"
                                    memberToReset = null
                                }
                            }
                        },
                        enabled = tempPassword.length >= 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Set Temporary Password", color = Gold) }
                }
            },
            confirmButton = {
                TextButton(onClick = { memberToReset = null }) { Text("Close", color = Cream) }
            }
        )
    }

    // Edit Closed-Table Transaction Dialog
    editingTx?.let { (isBuyIn, tx) ->
        AlertDialog(
            onDismissRequest = { editingTx = null },
            containerColor = FeltCard,
            title = { Text("Edit ${if (isBuyIn) "Buy-In" else "Exit"} (${tx.playerName})", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editTxAmount,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) editTxAmount = it },
                    label = { Text("Amount", color = Cream) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editTxAmount.toLongOrNull() ?: return@Button
                        val tId = editingTableId ?: return@Button
                        editingTx = null
                        viewModel.updateAdminTableTx(tId, isBuyIn, tx.id, amt, tx.note) { res ->
                            if (res.isSuccess) {
                                statusBanner = false to "Transaction updated"
                                viewModel.loadAdminTableDetail(tId) { td -> tableDetail = td }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editingTx = null }) { Text("Cancel", color = Cream) }
            }
        )
    }
}

@Composable
private fun AdminTxRow(
    tx: GroupAdminTableTxDto,
    amountColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = FeltBackground.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.playerName, color = Cream, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (!tx.note.isNullOrBlank()) {
                    Text(tx.note, color = Cream.copy(alpha = 0.55f), fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$${tx.amount}", color = amountColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Gold, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LoseRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
