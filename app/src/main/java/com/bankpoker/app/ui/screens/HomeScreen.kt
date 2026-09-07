package com.bankpoker.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.TokenManager
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
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(context, tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var isConnected by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var isLookingUpCode by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var authStateVersion by remember { mutableIntStateOf(0) }
    var unreadNotifCount by remember { mutableIntStateOf(0) }

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

    val isLoggedIn = remember(authStateVersion, tokenManager.getToken()) {
        !tokenManager.getToken().isNullOrBlank()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            refreshConnection()
                            onServerTestClick()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                                .border(
                                    BorderStroke(1.dp, Color.White),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Server Connection Status",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PokerAvatar(
                                    avatarId = currentAvatarId,
                                    name = currentDisplayName,
                                    size = 28.dp
                                )
                                Text(
                                    text = currentDisplayName,
                                    color = Cream,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (isGuest) {
                                    Surface(
                                        color = Gold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "GUEST",
                                            color = Gold,
                                            fontSize = 9.sp,
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
                                        Badge(
                                            containerColor = Gold,
                                            contentColor = Color.Black
                                        ) {
                                            Text("$unreadNotifCount", fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hero Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Gold.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♠",
                        fontSize = 58.sp,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "BANK POKER",
                    style = MaterialTheme.typography.displaySmall,
                    color = Cream,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Card suits row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "♠", color = Gold, fontSize = 16.sp)
                    Text(text = "♥", color = LoseRed, fontSize = 16.sp)
                    Text(text = "♦", color = LoseRed, fontSize = 16.sp)
                    Text(text = "♣", color = Gold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.5.dp)
                        .background(Gold, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(28.dp))

                // SMART SINGLE CODE INPUT
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Gold.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "JOIN BY CODE",
                            color = Gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
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
                                        fontSize = 13.sp,
                                        color = Cream.copy(alpha = 0.4f),
                                        letterSpacing = 1.sp
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { handleLookupCode() }
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Cream,
                                    fontSize = 16.sp,
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                if (isLookingUpCode) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                                } else {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Lookup")
                                }
                            }
                        }

                        Text(
                            text = "Supports Group Invite Codes & Table Codes",
                            color = Cream.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Two Side-By-Side Mode Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeOptionCard(
                        title = "QUICK\nTABLE",
                        subtitle = "Instant game",
                        icon = "♠",
                        onClick = onQuickTableClick,
                        modifier = Modifier.weight(1f)
                    )

                    HomeOptionCard(
                        title = "POKER\nGROUPS",
                        subtitle = "Circle stats",
                        icon = "👥",
                        onClick = onGroupsClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            remoteRepository = remoteRepository,
            tokenManager = tokenManager,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = {
                showAuthDialog = false
                authStateVersion++
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
            }
        )
    }
}

@Composable
fun HomeOptionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(
                width = 1.5.dp,
                color = Gold.copy(alpha = 0.75f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Gold.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, Gold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 26.sp,
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(Gold.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Cream.copy(alpha = 0.7f)
            )
        }
    }
}
