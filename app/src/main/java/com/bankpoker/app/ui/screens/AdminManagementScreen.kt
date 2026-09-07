package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.*
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.PokerAvatar
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(context, tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Overview", "Users", "Groups", "Tables")

    // State
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var overviewStats by remember { mutableStateOf<AdminStatsDto?>(null) }
    var usersList by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var groupsList by remember { mutableStateOf<List<AdminGroupDto>>(emptyList()) }
    var tablesList by remember { mutableStateOf<List<AdminTableDto>>(emptyList()) }

    // Dialog States
    var userToDelete by remember { mutableStateOf<AdminUserDto?>(null) }
    var userToChangeRole by remember { mutableStateOf<AdminUserDto?>(null) }
    var groupToDelete by remember { mutableStateOf<AdminGroupDto?>(null) }
    var tableToDelete by remember { mutableStateOf<AdminTableDto?>(null) }

    // Drill into table state
    var inspectingTable by remember { mutableStateOf<AdminTableDto?>(null) }
    var tablePlayers by remember { mutableStateOf<List<AdminTablePlayerDto>>(emptyList()) }
    var isLoadingPlayers by remember { mutableStateOf(false) }

    // Player action states inside drill-down
    var playerToRename by remember { mutableStateOf<AdminTablePlayerDto?>(null) }
    var renameSeatInput by remember { mutableStateOf("") }
    var playerToAdjustBalance by remember { mutableStateOf<AdminTablePlayerDto?>(null) }
    var balanceAdjustmentInput by remember { mutableStateOf("") }

    fun loadOverview() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = remoteRepository.getAdminOverview()
            isLoading = false
            if (res.isSuccess) {
                overviewStats = res.getOrNull()?.stats
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Failed to load overview"
            }
        }
    }

    fun loadUsers() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = remoteRepository.getAdminUsers()
            isLoading = false
            if (res.isSuccess) {
                usersList = res.getOrNull() ?: emptyList()
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Failed to load users"
            }
        }
    }

    fun loadGroups() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = remoteRepository.getAdminGroups()
            isLoading = false
            if (res.isSuccess) {
                groupsList = res.getOrNull() ?: emptyList()
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Failed to load groups"
            }
        }
    }

    fun loadTables() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = remoteRepository.getAdminTables()
            isLoading = false
            if (res.isSuccess) {
                tablesList = res.getOrNull() ?: emptyList()
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "Failed to load tables"
            }
        }
    }

    fun refreshCurrentTab() {
        when (selectedTab) {
            0 -> loadOverview()
            1 -> loadUsers()
            2 -> loadGroups()
            3 -> loadTables()
        }
    }

    LaunchedEffect(selectedTab) {
        refreshCurrentTab()
    }

    fun drillIntoTable(table: AdminTableDto) {
        inspectingTable = table
        isLoadingPlayers = true
        tablePlayers = emptyList()
        coroutineScope.launch {
            val res = remoteRepository.getAdminTablePlayers(table.id)
            isLoadingPlayers = false
            if (res.isSuccess) {
                tablePlayers = res.getOrNull() ?: emptyList()
            } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADMIN MANAGEMENT",
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Gold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshCurrentTab() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FeltBackground)
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = FeltDark,
                    contentColor = Gold
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            selectedContentColor = Gold,
                            unselectedContentColor = Cream.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Gold,
                        trackColor = FeltDark
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        color = LoseRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LoseRed)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = LoseRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = errorMessage!!, color = LoseRed, fontSize = 12.sp)
                        }
                    }
                }

                // Content by Tab
                when (selectedTab) {
                    0 -> OverviewTabContent(overviewStats)
                    1 -> UsersTabContent(
                        users = usersList,
                        onDeleteUser = { userToDelete = it },
                        onChangeRole = { userToChangeRole = it }
                    )
                    2 -> GroupsTabContent(
                        groups = groupsList,
                        onDeleteGroup = { groupToDelete = it }
                    )
                    3 -> TablesTabContent(
                        tables = tablesList,
                        onInspectTable = { drillIntoTable(it) },
                        onDeleteTable = { tableToDelete = it }
                    )
                }
            }
        }
    }

    // --- Delete User Confirmation Dialog ---
    if (userToDelete != null) {
        val user = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = FeltCard,
            title = { Text("Delete User?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete user \"${user.username}\"? All associated sessions and profile records will be permanently removed.",
                    color = Cream,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = user.id
                        userToDelete = null
                        coroutineScope.launch {
                            val res = remoteRepository.deleteAdminUser(uid)
                            if (res.isSuccess) {
                                Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show()
                                loadUsers()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoseRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }

    // --- Change User Role Dialog ---
    if (userToChangeRole != null) {
        val user = userToChangeRole!!
        var selectedRole by remember { mutableStateOf(user.role) }
        val roles = listOf("USER", "ADMIN", "SUPER_ADMIN")

        AlertDialog(
            onDismissRequest = { userToChangeRole = null },
            containerColor = FeltCard,
            title = { Text("Change Role: ${user.username}", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new permissions level:", color = Cream, fontSize = 13.sp)
                    roles.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                colors = RadioButtonDefaults.colors(selectedColor = Gold, unselectedColor = Cream.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = role,
                                color = if (selectedRole == role) Gold else Cream,
                                fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid = user.id
                        val newRole = selectedRole
                        userToChangeRole = null
                        coroutineScope.launch {
                            val res = remoteRepository.updateAdminUserRole(uid, newRole)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Role updated to $newRole", Toast.LENGTH_SHORT).show()
                                loadUsers()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text("Save Role", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToChangeRole = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }

    // --- Delete Group Confirmation Dialog ---
    if (groupToDelete != null) {
        val grp = groupToDelete!!
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor = FeltCard,
            title = { Text("Delete Group?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete group \"${grp.name}\"? All associated tables, balances, and player rosters will be permanently wiped.",
                    color = Cream,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gid = grp.id
                        groupToDelete = null
                        coroutineScope.launch {
                            val res = remoteRepository.deleteAdminGroup(gid)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                                loadGroups()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoseRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }

    // --- Delete Table Confirmation Dialog ---
    if (tableToDelete != null) {
        val tbl = tableToDelete!!
        AlertDialog(
            onDismissRequest = { tableToDelete = null },
            containerColor = FeltCard,
            title = { Text("Delete Table?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete table \"${tbl.name}\"? All buy-ins, exits, and activity history for this table will be deleted.",
                    color = Cream,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tid = tbl.id
                        tableToDelete = null
                        coroutineScope.launch {
                            val res = remoteRepository.deleteAdminTable(tid)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Table deleted successfully", Toast.LENGTH_SHORT).show()
                                loadTables()
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoseRed, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tableToDelete = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }

    // --- Drill-down: Table Players Inspector Modal ---
    if (inspectingTable != null) {
        val table = inspectingTable!!
        AlertDialog(
            onDismissRequest = { inspectingTable = null },
            containerColor = FeltCard,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = table.name,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ledger Inspector & Balance Adjustment",
                            color = Cream.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { inspectingTable = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Cream)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLoadingPlayers) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold)
                        }
                    } else if (tablePlayers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No players seated at this table.", color = Cream.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tablePlayers) { player ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = FeltDark,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                PokerAvatar(
                                                    avatarId = player.linkedUser?.avatarId ?: "avatar_1",
                                                    name = player.name,
                                                    size = 32.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = player.name,
                                                        color = Cream,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    if (player.linkedUser != null) {
                                                        Text(
                                                            text = "@${player.linkedUser.username}",
                                                            color = Gold,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                            }

                                            // Balance chip
                                            val balance = player.balance
                                            Surface(
                                                color = if (balance >= 0) WinGreen.copy(alpha = 0.2f) else LoseRed.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (balance >= 0) WinGreen else LoseRed)
                                            ) {
                                                Text(
                                                    text = if (balance >= 0) "+$balance" else "$balance",
                                                    color = if (balance >= 0) WinGreen else LoseRed,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Buy-ins: ${player.totalBuyIns} | Exits: ${player.totalExits}",
                                                color = Cream.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    playerToRename = player
                                                    renameSeatInput = player.name
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = Gold)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Rename Seat", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    playerToAdjustBalance = player
                                                    balanceAdjustmentInput = ""
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Adjust Balance", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // --- Rename Seat Dialog ---
    if (playerToRename != null) {
        val player = playerToRename!!
        val table = inspectingTable!!
        AlertDialog(
            onDismissRequest = { playerToRename = null },
            containerColor = FeltCard,
            title = { Text("Rename Player Seat", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new name for seat currently labeled \"${player.name}\":", color = Cream, fontSize = 12.sp)
                    OutlinedTextField(
                        value = renameSeatInput,
                        onValueChange = { renameSeatInput = it },
                        label = { Text("Player Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameSeatInput.trim()
                        if (newName.isBlank()) return@Button
                        playerToRename = null
                        coroutineScope.launch {
                            val res = remoteRepository.updateAdminTablePlayer(table.id, player.id, name = newName)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Seat renamed to $newName", Toast.LENGTH_SHORT).show()
                                drillIntoTable(table)
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToRename = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }

    // --- Adjust Balance Dialog ---
    if (playerToAdjustBalance != null) {
        val player = playerToAdjustBalance!!
        val table = inspectingTable!!
        AlertDialog(
            onDismissRequest = { playerToAdjustBalance = null },
            containerColor = FeltCard,
            title = { Text("Adjust Balance: ${player.name}", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current Balance: ${player.balance}", color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Enter balance delta (+ to increase chips via exit, - to decrease chips via buy-in):",
                        color = Cream,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = balanceAdjustmentInput,
                        onValueChange = { balanceAdjustmentInput = it },
                        placeholder = { Text("e.g. +50000 or -25000", color = Cream.copy(alpha = 0.4f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val adj = balanceAdjustmentInput.trim().toLongOrNull()
                        if (adj == null || adj == 0L) {
                            Toast.makeText(context, "Please enter a valid non-zero adjustment", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        playerToAdjustBalance = null
                        coroutineScope.launch {
                            val res = remoteRepository.updateAdminTablePlayer(table.id, player.id, balanceAdjustment = adj)
                            if (res.isSuccess) {
                                Toast.makeText(context, "Balance adjusted by $adj", Toast.LENGTH_SHORT).show()
                                drillIntoTable(table)
                            } else {
                                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text("Apply Adjustment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToAdjustBalance = null }) {
                    Text("Cancel", color = Cream)
                }
            }
        )
    }
}

// ---------------------- Sub-components for Tabs ----------------------

@Composable
fun OverviewTabContent(stats: AdminStatsDto?) {
    if (stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading metrics...", color = Gold, fontSize = 13.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PLATFORM OVERVIEW",
            color = Gold,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard("Total Users", "${stats.totalUsers}", Icons.Default.People, modifier = Modifier.weight(1f))
            AdminMetricCard("Active Players", "${stats.activePlayers}", Icons.Default.Person, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard("Registered", "${stats.fullUsers}", Icons.Default.VerifiedUser, modifier = Modifier.weight(1f))
            AdminMetricCard("Guests", "${stats.guestUsers}", Icons.Default.AccountCircle, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard("Total Groups", "${stats.totalGroups}", Icons.Default.Group, modifier = Modifier.weight(1f))
            AdminMetricCard("Total Tables", "${stats.totalTables}", Icons.Default.TableBar, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminMetricCard("Live Tables", "${stats.activeTables}", Icons.Default.PlayCircle, modifier = Modifier.weight(1f), isPositive = true)
            AdminMetricCard("Quick Tables", "${stats.quickTables}", Icons.Default.Bolt, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isPositive: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = FeltCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Cream.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(imageVector = icon, contentDescription = null, tint = if (isPositive) WinGreen else Gold, modifier = Modifier.size(16.dp))
            }
            Text(
                text = value,
                color = if (isPositive) WinGreen else Cream,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun UsersTabContent(
    users: List<AdminUserDto>,
    onDeleteUser: (AdminUserDto) -> Unit,
    onChangeRole: (AdminUserDto) -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found.", color = Cream.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "USER ACCOUNTS (${users.size})",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
        items(users) { user ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FeltCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PokerAvatar(
                                avatarId = user.avatarId ?: "avatar_1",
                                name = user.displayName ?: user.username,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = user.displayName ?: user.username,
                                    color = Cream,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "@${user.username} • ${user.groupCount} groups",
                                    color = Cream.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Role badge
                        Surface(
                            color = when (user.role) {
                                "SUPER_ADMIN" -> Gold.copy(alpha = 0.2f)
                                "ADMIN" -> WinGreen.copy(alpha = 0.2f)
                                else -> FeltDark
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                when (user.role) {
                                    "SUPER_ADMIN" -> Gold
                                    "ADMIN" -> WinGreen
                                    else -> Cream.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Text(
                                text = user.role,
                                color = when (user.role) {
                                    "SUPER_ADMIN" -> Gold
                                    "ADMIN" -> WinGreen
                                    else -> Cream.copy(alpha = 0.7f)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onChangeRole(user) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(14.dp), tint = Gold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change Role", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onDeleteUser(user) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LoseRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupsTabContent(
    groups: List<AdminGroupDto>,
    onDeleteGroup: (AdminGroupDto) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No groups found.", color = Cream.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ALL POKER GROUPS (${groups.size})",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
        items(groups) { group ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FeltCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = group.name,
                                color = Cream,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Owner: ${group.owner?.displayName ?: group.owner?.username ?: "Unknown"} • ${group.memberCount} members • ${group.tableCount} tables",
                                color = Cream.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { onDeleteGroup(group) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LoseRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (!group.inviteCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = FeltDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "CODE: ${group.inviteCode}",
                                color = Gold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TablesTabContent(
    tables: List<AdminTableDto>,
    onInspectTable: (AdminTableDto) -> Unit,
    onDeleteTable: (AdminTableDto) -> Unit
) {
    if (tables.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tables found.", color = Cream.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ALL ACTIVE & ARCHIVED TABLES (${tables.size})",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        }
        items(tables) { table ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FeltCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = table.name,
                                    color = Cream,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (table.isQuickTable) {
                                    Surface(
                                        color = Gold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "QUICK",
                                            color = Gold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Group: ${table.groupName ?: "None (Quick Table)"} • ${table.playerCount} players • Status: ${table.status}",
                                color = Cream.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { onDeleteTable(table) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LoseRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!table.code.isNullOrBlank()) {
                            Surface(
                                color = FeltDark,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "CODE: ${table.code}",
                                    color = Gold,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = { onInspectTable(table) },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inspect Players", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
