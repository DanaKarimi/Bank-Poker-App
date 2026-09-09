package com.bankpoker.app.ui.screens

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
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.SocketManager
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.ActiveTableSummaryDto
import com.bankpoker.app.data.remote.dto.UserGroupSummaryDto
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.AuthDialog
import com.bankpoker.app.ui.components.PokerAvatar
import com.bankpoker.app.ui.components.ProfileDialog
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
    var isLoadingFeed by remember { mutableStateOf(false) }

    // FAB Bottom Sheet state
    var showFabBottomSheet by remember { mutableStateOf(false) }
    var showQuickTableCreateDialog by remember { mutableStateOf(false) }
    var quickTableNameInput by remember { mutableStateOf("") }
    var quickTableChipInput by remember { mutableStateOf("100000") }
    var isCreatingQuickTable by remember { mutableStateOf(false) }

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
                            myGroups = groupsRes.getOrNull() ?: emptyList()
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
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BANK POKER",
                            color = Cream,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            refreshConnection()
                            onServerTestClick()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                                .border(BorderStroke(1.dp, Color.White), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Server Connection Status",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Super Admin Control Plane button (Parity with Web Header)
                    if (isLoggedIn && userRole == "SUPER_ADMIN") {
                        Surface(
                            modifier = Modifier
                                .clickable { onAdminManagementClick() }
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF451A03),
                            border = BorderStroke(1.dp, Gold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Admin",
                                    tint = Gold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    color = Gold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (isLoggedIn) {
                        Surface(
                            modifier = Modifier
                                .clickable { showProfileDialog = true }
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = FeltCard,
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
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
                                    color = Cream,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 85.dp)
                                )
                                if (isGuest) {
                                    Surface(
                                        color = Gold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "GUEST",
                                            color = Gold,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = onNotificationsClick) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifCount > 0) {
                                        Badge(containerColor = Gold, contentColor = Color.Black) {
                                            Text("$unreadNotifCount", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Gold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFabBottomSheet = true },
                containerColor = Gold,
                contentColor = Color.Black,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Game / Group", modifier = Modifier.size(28.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                // Quick Join By Code Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "JOIN BY CODE",
                            color = Gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

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
                                        color = Cream.copy(alpha = 0.4f),
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
                                    color = Cream,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold,
                                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                                    cursorColor = Gold
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { handleLookupCode() },
                                enabled = !isLookingUpCode && codeInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                if (isLookingUpCode) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Lookup")
                                }
                            }
                        }
                    }
                }

                // ---------------- 1. LIVE NOW: ACTIVE TABLES ----------------
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE NOW",
                                color = Gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Gold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${activeTables.size}",
                                    color = Gold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        TextButton(onClick = onQuickTableClick) {
                            Text("All Tables", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (activeTables.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = FeltCard.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No tables active right now.", color = Cream.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { showQuickTableCreateDialog = true }) {
                                    Text("+ Start a Quick Table", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

                // ---------------- 2. POKER GROUPS SECTION ----------------
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "POKER GROUPS",
                                color = Gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Gold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${myGroups.size}",
                                    color = Gold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        TextButton(onClick = onGroupsClick) {
                            Text("Manage Groups", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (myGroups.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = FeltCard.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Gold.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("You haven't joined any poker groups yet.", color = Cream.copy(alpha = 0.6f), fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = onCreateGroupClick) {
                                    Text("+ Create New Group", color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    }
                                )
                            }
                        }
                    }
                }

                // ---------------- 3. QUICK TABLES SECTION ----------------
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QUICK TABLES",
                                color = Gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }

                        TextButton(onClick = onQuickTableClick) {
                            Text("History", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuickTableCreateDialog = true },
                        color = FeltCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Gold.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Gold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Instant Poker Table", color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("No group needed. Play immediately.", color = Cream.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = { showQuickTableCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Create", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    }

    // --- FAB MODAL BOTTOM SHEET ---
    if (showFabBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFabBottomSheet = false },
            containerColor = FeltCard,
            contentColor = Cream
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CREATE NEW",
                    color = Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
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
                    color = FeltDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Gold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("New Poker Group", color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Dedicated circle with member ledger and invite code", color = Cream.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }

                // Option 2: New Quick Table
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFabBottomSheet = false
                            quickTableNameInput = ""
                            quickTableChipInput = "100000"
                            showQuickTableCreateDialog = true
                        },
                    color = FeltDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Gold.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("New Quick Table", color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Fast casual table without requiring a group", color = Cream.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- CREATE QUICK TABLE DIALOG ---
    if (showQuickTableCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCreatingQuickTable) showQuickTableCreateDialog = false },
            containerColor = FeltCard,
            title = { Text("New Quick Table", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quickTableNameInput,
                        onValueChange = { quickTableNameInput = it },
                        label = { Text("Table Name") },
                        placeholder = { Text("e.g. Friday Night Game") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = quickTableChipInput,
                        onValueChange = { quickTableChipInput = it },
                        label = { Text("Default Buy-In / Chips") },
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
                        val name = quickTableNameInput.trim().ifBlank { "Quick Table" }
                        val chip = quickTableChipInput.trim().toLongOrNull() ?: 100000L
                        isCreatingQuickTable = true
                        coroutineScope.launch {
                            val res = remoteRepository.createQuickTable(name, chip, null)
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
                                    groupId = null
                                )
                                showQuickTableCreateDialog = false
                                onNavigateToTable(localTable.id)
                            }
                        }
                    },
                    enabled = !isCreatingQuickTable,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    if (isCreatingQuickTable) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("Create & Enter", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuickTableCreateDialog = false },
                    enabled = !isCreatingQuickTable
                ) {
                    Text("Cancel", color = Cream)
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
        color = FeltCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, Gold.copy(alpha = 0.6f)),
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
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = WinGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = WinGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Text(
                text = "${table.gameType ?: "NL Hold'em"} • ${table.groupName ?: "Quick Table"}",
                color = Cream.copy(alpha = 0.6f),
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
                    color = Gold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
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
    onCopyCode: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGroupClick() },
        color = FeltCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("♣", color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = group.name,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (group.isCreator) {
                        Surface(
                            color = Gold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.8.dp, Gold)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = Gold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Surface(
                        color = FeltDark,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${group.memberCount} members",
                            color = Cream.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invite code chip (tap to copy)
                if (!group.inviteCode.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.clickable { onCopyCode() },
                        color = FeltDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.inviteCode,
                                color = Gold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = Gold.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
