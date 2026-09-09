package com.bankpoker.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.SocketManager
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.ActiveTableSummaryDto
import com.bankpoker.app.data.remote.dto.UserGroupSummaryDto
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.*
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: PokerRepository,
    onQuickTableClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onServerTestClick: () -> Unit = {},
    onCreateGroupClick: () -> Unit = {},
    onNavigateToTable: (String) -> Unit = {},
    onNavigateToGroup: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAdminManagementClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val tokenManager = remember { TokenManager.getInstance(context) }
    val socketManager = remember { SocketManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(context, tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var isConnected by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var isLookingUpCode by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var authStateVersion by remember { mutableIntStateOf(0) }
    var unreadNotifCount by remember { mutableIntStateOf(0) }

    // Live Feed & Groups state
    var activeTables by remember { mutableStateOf<List<ActiveTableSummaryDto>>(emptyList()) }
    var myGroups by remember { mutableStateOf<List<UserGroupSummaryDto>>(emptyList()) }

    // Local quick tables state
    val quickTables by repository.getQuickTables().collectAsState(initial = emptyList())

    // FAB Bottom Sheet state
    var showFabBottomSheet by remember { mutableStateOf(false) }
    var showQuickTableCreateDialog by remember { mutableStateOf(false) }
    var isCreatingQuickTable by remember { mutableStateOf(false) }

    fun shareCode(code: String, name: String, isGroup: Boolean) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                if (isGroup) "Join my poker group \"$name\" on BankPoker using code: $code"
                else "Join my poker table \"$name\" on BankPoker using code: $code"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Code")
        context.startActivity(shareIntent)
    }

    suspend fun loadFeedAndGroups() {
        if (tokenManager.isLoggedIn()) {
            withContext(Dispatchers.IO) {
                try {
                    val tablesRes = remoteRepository.getActiveTables()
                    val groupsRes = remoteRepository.getMyGroups()
                    withContext(Dispatchers.Main) {
                        if (tablesRes.isSuccess) {
                            activeTables = tablesRes.getOrNull() ?: emptyList()
                        }
                        if (groupsRes.isSuccess) {
                            val list = groupsRes.getOrNull() ?: emptyList()
                            myGroups = list
                            list.forEach { g ->
                                val existing = repository.getGroupById(g.id)
                                if (existing == null) {
                                    repository.createGroup(
                                        name = g.name,
                                        mode = "ONLINE",
                                        serverId = g.id,
                                        customId = g.id
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore background load failures
                }
            }
        }
    }

    suspend fun checkConnection() {
        withContext(Dispatchers.IO) {
            try {
                val service = ApiClient.getApiService(context, tokenManager)
                val response = service.healthCheck()
                val success = response.isSuccessful && response.body()?.status?.equals("ok", ignoreCase = true) == true
                withContext(Dispatchers.Main) {
                    isConnected = success
                }
                if (success && tokenManager.isLoggedIn()) {
                    val notifRes = remoteRepository.getNotifications()
                    if (notifRes.isSuccess) {
                        val count = notifRes.getOrNull()?.unreadCount ?: 0
                        withContext(Dispatchers.Main) {
                            unreadNotifCount = count
                        }
                    }
                    loadFeedAndGroups()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isConnected = false
                }
            }
        }
    }

    fun refreshConnection() {
        coroutineScope.launch {
            checkConnection()
        }
    }

    fun doRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                checkConnection()
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkConnection()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshConnection()
                authStateVersion++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isLoggedIn = remember(authStateVersion, tokenManager.getToken()) {
        !tokenManager.getToken().isNullOrBlank()
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            socketManager.connect()
        }
    }

    LaunchedEffect(myGroups) {
        myGroups.forEach { g ->
            if (g.id.isNotBlank()) {
                socketManager.joinGroup(g.id)
            }
        }
    }

    LaunchedEffect(Unit) {
        socketManager.events.collect { evt ->
            when (evt.event) {
                "buyin_recorded", "exit_recorded", "payment_created", "settlement_done", "group_updated", "table_created", "table_closed", "table_updated", "entry_fee_updated" -> {
                    loadFeedAndGroups()
                }
            }
        }
    }

    fun handleLookupCode() {
        val trimmed = codeInput.trim().uppercase()
        if (trimmed.length < 4) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Enter a valid 6-character code")
            }
            return
        }

        keyboardController?.hide()
        isLookingUpCode = true
        coroutineScope.launch {
            val result = remoteRepository.lookupCode(trimmed)
            isLookingUpCode = false
            if (result.isSuccess) {
                val lookup = result.getOrNull()!!
                if (lookup.type == "GROUP") {
                    onNavigateToGroup(lookup.id)
                } else if (lookup.type == "TABLE") {
                    onNavigateToTable(lookup.id)
                } else {
                    snackbarHostState.showSnackbar("Found ${lookup.name}")
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Code not found"
                snackbarHostState.showSnackbar("Error: $err")
            }
        }
    }

    val currentDisplayName = remember(authStateVersion, isLoggedIn) {
        tokenManager.getDisplayName() ?: tokenManager.getUsername() ?: "Player"
    }
    val currentAvatarId = remember(authStateVersion, isLoggedIn) {
        tokenManager.getAvatarId()
    }
    val isGuest = remember(authStateVersion, isLoggedIn) {
        tokenManager.isGuest()
    }
    val userRole = remember(authStateVersion, isLoggedIn) {
        tokenManager.getRole() ?: "USER"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "♠",
                            fontSize = 20.sp,
                            color = DesignTokens.GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BANK POKER",
                            color = DesignTokens.CreamText,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    // Super Admin Control Plane button (Parity with Web Header)
                    if (isLoggedIn && userRole == "SUPER_ADMIN") {
                        Surface(
                            modifier = Modifier
                                .clickable { onAdminManagementClick() }
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(DesignTokens.RadiusMD),
                            color = Color(0xFF451A03),
                            border = BorderStroke(1.dp, DesignTokens.GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Admin",
                                    tint = DesignTokens.GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    color = DesignTokens.GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (isLoggedIn) {
                        // User Avatar + Name Profile Chip
                        Surface(
                            modifier = Modifier
                                .clickable { showProfileDialog = true }
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(DesignTokens.RadiusFull),
                            color = DesignTokens.FeltCard,
                            border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PokerAvatar(
                                    avatarId = currentAvatarId,
                                    name = currentDisplayName,
                                    size = 26.dp
                                )
                                Text(
                                    text = currentDisplayName,
                                    color = DesignTokens.CreamText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 85.dp)
                                )
                                if (isGuest) {
                                    Surface(
                                        color = DesignTokens.GoldAccent.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "GUEST",
                                            color = DesignTokens.GoldAccent,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Notifications Icon with Unread Badge
                        IconButton(onClick = onNotificationsClick) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(containerColor = DesignTokens.GoldAccent, contentColor = Color.Black) {
                                            Text("$unreadNotifCount", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = DesignTokens.GoldAccent
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DesignTokens.GoldAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(DesignTokens.RadiusLG),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Settings Icon
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = DesignTokens.GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFabBottomSheet = true },
                containerColor = DesignTokens.GoldAccent,
                contentColor = Color.Black,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Game / Group", modifier = Modifier.size(28.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DesignTokens.FeltGreen
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF186349), DesignTokens.FeltGreen),
                        radius = 1500f
                    )
                )
                .padding(paddingValues)
        ) {
            CasinoWatermarks()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { doRefresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // =======================================================
                    // SMART CODE INPUT CARD (Paste-Aware Auto-Detect)
                    // =======================================================
                    UnifiedCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = DesignTokens.RadiusLG
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Pin,
                                        contentDescription = null,
                                        tint = DesignTokens.GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "JOIN BY CODE",
                                        color = DesignTokens.GoldAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }

                                val clipText = clipboardManager.getText()?.text?.trim()
                                if (codeInput.isBlank() && !clipText.isNullOrBlank() && clipText.length in 4..8) {
                                    Surface(
                                        modifier = Modifier.clickable {
                                            codeInput = clipText.uppercase()
                                        },
                                        color = DesignTokens.FeltDark,
                                        shape = RoundedCornerShape(DesignTokens.RadiusSM),
                                        border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = DesignTokens.GoldAccent,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "PASTE",
                                                color = DesignTokens.GoldAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { if (it.length <= 8) codeInput = it.uppercase() },
                                    placeholder = {
                                        Text(
                                            "ENTER 6-CHAR CODE",
                                            fontSize = 12.sp,
                                            color = DesignTokens.CreamText.copy(alpha = 0.4f),
                                            letterSpacing = 1.sp
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { handleLookupCode() }),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = DesignTokens.CreamText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DesignTokens.GoldAccent,
                                        unfocusedBorderColor = DesignTokens.GoldAccent.copy(alpha = 0.4f),
                                        cursorColor = DesignTokens.GoldAccent
                                    ),
                                    shape = RoundedCornerShape(DesignTokens.RadiusMD),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = { handleLookupCode() },
                                    enabled = !isLookingUpCode && codeInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.GoldAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(DesignTokens.RadiusMD),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    if (isLookingUpCode) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Lookup")
                                    }
                                }
                            }

                            Text(
                                text = "Enter 6-char group invite or table code to jump straight in",
                                color = DesignTokens.CreamMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // =======================================================
                    // 1. LIVE NOW: ACTIVE TABLES HORIZONTAL SECTION
                    // =======================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UnifiedLiveBadge()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIVE NOW",
                                    color = DesignTokens.GoldAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = DesignTokens.GoldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(DesignTokens.RadiusSM)
                                ) {
                                    Text(
                                        text = "${activeTables.size}",
                                        color = DesignTokens.GoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            TextButton(onClick = onQuickTableClick) {
                                Text("All Tables", color = DesignTokens.GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeTables.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = DesignTokens.FeltCard.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(DesignTokens.RadiusLG),
                                border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No tables active right now.", color = DesignTokens.CreamMuted, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = { showQuickTableCreateDialog = true }) {
                                        Text("+ Start a Quick Table", color = DesignTokens.GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(activeTables) { table ->
                                    LiveTableCard(
                                        table = table,
                                        onJoinClick = { onNavigateToTable(table.id) }
                                    )
                                }
                            }
                        }
                    }

                    // =======================================================
                    // 2. POKER GROUPS SECTION (Group Cards with NO balance)
                    // =======================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = DesignTokens.GoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "POKER GROUPS",
                                    color = DesignTokens.GoldAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = DesignTokens.GoldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(DesignTokens.RadiusSM)
                                ) {
                                    Text(
                                        text = "${myGroups.size}",
                                        color = DesignTokens.GoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            TextButton(onClick = onGroupsClick) {
                                Text("Manage Groups", color = DesignTokens.GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (myGroups.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = DesignTokens.FeltCard.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(DesignTokens.RadiusLG),
                                border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("You haven't joined any poker groups yet.", color = DesignTokens.CreamMuted, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(onClick = onCreateGroupClick) {
                                        Text("+ Create New Group", color = DesignTokens.GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                myGroups.forEach { group ->
                                    HomeGroupCard(
                                        group = group,
                                        onGroupClick = { onNavigateToGroup(group.id) },
                                        onCopyCode = {
                                            if (!group.inviteCode.isNullOrBlank()) {
                                                clipboardManager.setText(AnnotatedString(group.inviteCode))
                                                Toast.makeText(context, "Invite code ${group.inviteCode} copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onShareCode = {
                                            if (!group.inviteCode.isNullOrBlank()) {
                                                shareCode(group.inviteCode, group.name, isGroup = true)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // =======================================================
                    // 3. QUICK TABLES SECTION (Cards with code copy/share)
                    // =======================================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = DesignTokens.GoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QUICK TABLES",
                                    color = DesignTokens.GoldAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                                if (quickTables.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DesignTokens.GoldAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(DesignTokens.RadiusSM)
                                    ) {
                                        Text(
                                            text = "${quickTables.size}",
                                            color = DesignTokens.GoldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = onQuickTableClick) {
                                Text("History", color = DesignTokens.GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Create Instant Table Banner
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showQuickTableCreateDialog = true },
                            color = DesignTokens.FeltCard,
                            shape = RoundedCornerShape(DesignTokens.RadiusLG),
                            border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(DesignTokens.GoldAccent.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = DesignTokens.GoldAccent)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Instant Poker Table", color = DesignTokens.CreamText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("No group needed. Play immediately.", color = DesignTokens.CreamMuted, fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = { showQuickTableCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.GoldAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(DesignTokens.RadiusSM),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Create", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        // Recent Quick Tables List (if available)
                        if (quickTables.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                quickTables.take(5).forEach { qTable ->
                                    HomeQuickTableCard(
                                        table = qTable,
                                        onTableClick = { onNavigateToTable(qTable.id) },
                                        onCopyCode = {
                                            if (!qTable.code.isNullOrBlank()) {
                                                clipboardManager.setText(AnnotatedString(qTable.code!!))
                                                Toast.makeText(context, "Table code ${qTable.code} copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onShareCode = {
                                            if (!qTable.code.isNullOrBlank()) {
                                                shareCode(qTable.code!!, qTable.name, isGroup = false)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // =======================================================
    // FAB MODAL BOTTOM SHEET (New Group / New Quick Table)
    // =======================================================
    if (showFabBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabBottomSheet = false },
            containerColor = DesignTokens.FeltCard,
            contentColor = DesignTokens.CreamText,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DesignTokens.GoldAccent) },
            shape = RoundedCornerShape(topStart = DesignTokens.RadiusXL, topEnd = DesignTokens.RadiusXL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE NEW",
                    color = DesignTokens.GoldAccent,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )

                // Option 1: New Poker Group
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFabBottomSheet = false
                            onCreateGroupClick()
                        },
                    color = DesignTokens.FeltDark,
                    shape = RoundedCornerShape(DesignTokens.RadiusMD),
                    border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(DesignTokens.GoldAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = DesignTokens.GoldAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("New Poker Group", color = DesignTokens.CreamText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Dedicated circle with member ledger and invite code", color = DesignTokens.CreamMuted, fontSize = 11.sp)
                        }
                    }
                }

                // Option 2: New Quick Table
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFabBottomSheet = false
                            showQuickTableCreateDialog = true
                        },
                    color = DesignTokens.FeltDark,
                    shape = RoundedCornerShape(DesignTokens.RadiusMD),
                    border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(DesignTokens.GoldAccent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = DesignTokens.GoldAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("New Quick Table", color = DesignTokens.CreamText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Fast casual table without requiring a group", color = DesignTokens.CreamMuted, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // =======================================================
    // CREATE QUICK TABLE BOTTOM SHEET
    // =======================================================
    if (showQuickTableCreateDialog) {
        CreateQuickTableBottomSheet(
            onDismiss = { showQuickTableCreateDialog = false },
            isCreating = isCreatingQuickTable,
            onCreateTable = { name, chip, hasEntryFee, entryFeeLong ->
                isCreatingQuickTable = true
                coroutineScope.launch {
                    val res = remoteRepository.createQuickTable(name, chip, entryFeeLong)
                    isCreatingQuickTable = false
                    if (res.isSuccess) {
                        showQuickTableCreateDialog = false
                        val obj = res.getOrNull()
                        val tableId = obj?.getAsJsonObject("table")?.get("id")?.asString
                            ?: obj?.get("tableId")?.asString
                        if (tableId != null) {
                            repository.createTable(
                                name = name,
                                chipValue = chip,
                                groupId = null,
                                hasEntryFee = hasEntryFee,
                                entryFee = entryFeeLong,
                                customId = tableId
                            )
                            onNavigateToTable(tableId)
                        } else {
                            refreshConnection()
                        }
                    } else {
                        // Fallback for offline usage: create local quick table in Room
                        val localTable = repository.createTable(
                            name = name,
                            chipValue = chip,
                            groupId = null,
                            hasEntryFee = hasEntryFee,
                            entryFee = entryFeeLong
                        )
                        showQuickTableCreateDialog = false
                        onNavigateToTable(localTable.id)
                    }
                }
            }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            remoteRepository = remoteRepository,
            tokenManager = tokenManager,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = {
                showAuthDialog = false
                authStateVersion++
                refreshConnection()
            }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            remoteRepository = remoteRepository,
            tokenManager = tokenManager,
            onDismiss = { showProfileDialog = false },
            onProfileUpdated = {
                authStateVersion++
            },
            onLogout = {
                showProfileDialog = false
                authStateVersion++
                refreshConnection()
            },
            onAdminManagementClick = {
                showProfileDialog = false
                onAdminManagementClick()
            }
        )
    }
}

// ---------------- LIVE TABLE CARD COMPONENT ----------------
@Composable
fun LiveTableCard(
    table: ActiveTableSummaryDto,
    onJoinClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable { onJoinClick() },
        color = DesignTokens.FeltCard,
        shape = RoundedCornerShape(DesignTokens.RadiusLG),
        border = BorderStroke(1.2.dp, DesignTokens.GoldAccent.copy(alpha = 0.6f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = table.name,
                    color = DesignTokens.CreamText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                UnifiedLiveBadge()
            }

            Text(
                text = "${table.gameType ?: "NL Hold'em"} • ${table.groupName ?: "Quick Table"}",
                color = DesignTokens.CreamMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${table.playerCount} players",
                    color = DesignTokens.GoldAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.GoldAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(DesignTokens.RadiusSM),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Join", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ---------------- HOME GROUP CARD COMPONENT ----------------
@Composable
fun HomeGroupCard(
    group: UserGroupSummaryDto,
    onGroupClick: () -> Unit,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGroupClick() },
        color = DesignTokens.FeltCard,
        shape = RoundedCornerShape(DesignTokens.RadiusLG),
        border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.4f)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("♣", color = DesignTokens.GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.name,
                        color = DesignTokens.CreamText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (group.isCreator) {
                        UnifiedAdminBadge()
                    }
                    Surface(
                        color = DesignTokens.FeltDark,
                        shape = RoundedCornerShape(DesignTokens.RadiusFull),
                        border = BorderStroke(0.8.dp, DesignTokens.GoldAccent.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${group.memberCount} members",
                            color = DesignTokens.CreamText.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Invite code row with copy & share actions (NO balance per hard rule)
            if (!group.inviteCode.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.clickable { onCopyCode() },
                            color = DesignTokens.FeltDark,
                            shape = RoundedCornerShape(DesignTokens.RadiusSM),
                            border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.inviteCode,
                                    color = DesignTokens.GoldAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = DesignTokens.GoldAccent.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onShareCode,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = DesignTokens.GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- HOME QUICK TABLE CARD COMPONENT ----------------
@Composable
fun HomeQuickTableCard(
    table: PokerTable,
    onTableClick: () -> Unit,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTableClick() },
        color = DesignTokens.FeltCard,
        shape = RoundedCornerShape(DesignTokens.RadiusLG),
        border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.35f)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = DesignTokens.GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = table.name,
                        color = DesignTokens.CreamText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = if (table.status == "ACTIVE") DesignTokens.WinGreen.copy(alpha = 0.15f) else Color.DarkGray,
                    shape = RoundedCornerShape(DesignTokens.RadiusFull),
                    border = BorderStroke(0.8.dp, if (table.status == "ACTIVE") DesignTokens.WinGreen else Color.Gray)
                ) {
                    Text(
                        text = table.status,
                        color = if (table.status == "ACTIVE") DesignTokens.WinGreen else Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!table.code.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.clickable { onCopyCode() },
                            color = DesignTokens.FeltDark,
                            shape = RoundedCornerShape(DesignTokens.RadiusSM),
                            border = BorderStroke(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = table.code!!,
                                    color = DesignTokens.GoldAccent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = DesignTokens.GoldAccent.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onShareCode,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = DesignTokens.GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Unpublished • Local Table",
                        color = DesignTokens.CreamMuted,
                        fontSize = 11.sp
                    )
                }

                if (table.chipValue != null) {
                    Text(
                        text = "Chip: $${table.chipValue}",
                        color = DesignTokens.GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ---------------- CREATE QUICK TABLE BOTTOM SHEET ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuickTableBottomSheet(
    onDismiss: () -> Unit,
    isCreating: Boolean = false,
    onCreateTable: (String, Long?, Boolean, Long?) -> Unit
) {
    var tableName by remember { mutableStateOf("") }
    var chipValue by remember { mutableStateOf("") }
    var hasEntryFee by remember { mutableStateOf(false) }
    var entryFeeAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val chipPresets = listOf(5L, 10L, 25L, 50L, 100L)

    ModalBottomSheet(
        onDismissRequest = { if (!isCreating) onDismiss() },
        sheetState = sheetState,
        containerColor = DesignTokens.FeltCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DesignTokens.GoldAccent) },
        shape = RoundedCornerShape(topStart = DesignTokens.RadiusXL, topEnd = DesignTokens.RadiusXL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "NEW QUICK TABLE", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tableName,
                onValueChange = { 
                    tableName = it
                    if (error != null) error = null
                },
                label = { Text("Table Name", color = DesignTokens.CreamText.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. Quick Game #1", color = DesignTokens.CreamText.copy(alpha = 0.4f)) },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignTokens.GoldAccent,
                    unfocusedBorderColor = DesignTokens.GoldAccent.copy(alpha = 0.4f),
                    focusedTextColor = DesignTokens.CreamText,
                    unfocusedTextColor = DesignTokens.CreamText,
                    cursorColor = DesignTokens.GoldAccent,
                    focusedContainerColor = DesignTokens.FeltGreen,
                    unfocusedContainerColor = DesignTokens.FeltGreen
                )
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error!!,
                    color = DesignTokens.LoseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chip Value Selector
            Text(
                text = "DEFAULT CHIP VALUE",
                style = MaterialTheme.typography.labelSmall,
                color = DesignTokens.GoldAccent.copy(alpha = 0.75f),
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
                                color = if (isSelected) DesignTokens.GoldAccent else DesignTokens.GoldAccent.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(DesignTokens.RadiusSM)
                            )
                            .clickable {
                                chipValue = if (isSelected) "" else preset.toString()
                            },
                        shape = RoundedCornerShape(DesignTokens.RadiusSM),
                        color = if (isSelected) DesignTokens.GoldAccent.copy(alpha = 0.25f) else DesignTokens.FeltGreen
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$$preset",
                                color = if (isSelected) DesignTokens.GoldAccent else DesignTokens.CreamText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = chipValue,
                onValueChange = { chipValue = it.filter { c -> c.isDigit() } },
                label = { Text("Custom Chip Value (optional)", color = DesignTokens.CreamText.copy(alpha = 0.7f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMD),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignTokens.GoldAccent,
                    unfocusedBorderColor = DesignTokens.GoldAccent.copy(alpha = 0.4f),
                    focusedTextColor = DesignTokens.CreamText,
                    unfocusedTextColor = DesignTokens.CreamText,
                    cursorColor = DesignTokens.GoldAccent,
                    focusedContainerColor = DesignTokens.FeltGreen,
                    unfocusedContainerColor = DesignTokens.FeltGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Entry Fee Toggle Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DesignTokens.GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(DesignTokens.RadiusMD)),
                shape = RoundedCornerShape(DesignTokens.RadiusMD),
                colors = CardDefaults.cardColors(containerColor = DesignTokens.FeltGreen)
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
                                color = DesignTokens.CreamText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Require entry fee for this game",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignTokens.CreamMuted
                            )
                        }
                        Switch(
                            checked = hasEntryFee,
                            onCheckedChange = { hasEntryFee = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DesignTokens.GoldAccent,
                                checkedTrackColor = DesignTokens.GoldAccent.copy(alpha = 0.5f),
                                uncheckedThumbColor = DesignTokens.CreamText.copy(alpha = 0.5f),
                                uncheckedTrackColor = DesignTokens.GoldAccent.copy(alpha = 0.2f)
                            )
                        )
                    }

                    if (hasEntryFee) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = entryFeeAmount,
                            onValueChange = { entryFeeAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("Entry Fee Amount", color = DesignTokens.CreamText.copy(alpha = 0.7f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DesignTokens.RadiusMD),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.GoldAccent,
                                unfocusedBorderColor = DesignTokens.GoldAccent.copy(alpha = 0.4f),
                                focusedTextColor = DesignTokens.CreamText,
                                unfocusedTextColor = DesignTokens.CreamText,
                                cursorColor = DesignTokens.GoldAccent,
                                focusedContainerColor = DesignTokens.FeltCard,
                                unfocusedContainerColor = DesignTokens.FeltCard
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = if (isCreating) "CREATING..." else "CREATE & ENTER",
                enabled = !isCreating,
                onClick = {
                    val name = tableName.trim().ifBlank { "Quick Table" }
                    val chipValueLong = chipValue.toLongOrNull()
                    val entryFeeLong = if (hasEntryFee) {
                        entryFeeAmount.toLongOrNull() ?: chipValueLong ?: 0L
                    } else null
                    onCreateTable(name, chipValueLong, hasEntryFee, entryFeeLong)
                }
            )
        }
    }
}
