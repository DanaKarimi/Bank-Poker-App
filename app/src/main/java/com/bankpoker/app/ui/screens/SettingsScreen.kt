package com.bankpoker.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.ServerConfigManager
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.AvatarPickerDialog
import com.bankpoker.app.ui.components.GoldGradientButton
import com.bankpoker.app.ui.components.PokerAvatar
import com.bankpoker.app.ui.components.SectionHeader
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit = {},
    onAdminManagementClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val serverConfigManager = remember { ServerConfigManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(context, tokenManager)
        RemoteRepository(service, tokenManager)
    }

    // Profile State
    var displayNameInput by remember { mutableStateOf(tokenManager.getDisplayName() ?: tokenManager.getUsername() ?: "") }
    var selectedAvatarId by remember { mutableStateOf(tokenManager.getAvatarId() ?: "avatar_1") }
    var isSavingProfile by remember { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf("") }
    var profileError by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Server State
    var serverUrlInput by remember { mutableStateOf(serverConfigManager.getBaseUrl()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<Boolean?>(null) }
    var serverMessage by remember { mutableStateOf("") }

    // Notification Prefs State
    var notifPrefs by remember {
        mutableStateOf(
            mapOf(
                "table_created" to true,
                "member_joined" to true,
                "request_to_me" to true,
                "settlement" to true
            )
        )
    }
    var isLoadingPrefs by remember { mutableStateOf(false) }
    var isSavingPrefs by remember { mutableStateOf(false) }

    val isLoggedIn = remember { tokenManager.isLoggedIn() }
    val username = remember { tokenManager.getUsername() ?: "Guest" }
    val isGuest = remember { tokenManager.isGuest() }

    // Load initial notification preferences
    LaunchedEffect(Unit) {
        if (isLoggedIn && !isGuest) {
            isLoadingPrefs = true
            val result = remoteRepository.getNotificationSettings()
            isLoadingPrefs = false
            if (result.isSuccess) {
                val s = result.getOrNull()
                if (!s.isNullOrEmpty()) {
                    notifPrefs = notifPrefs.mapValues { entry -> s[entry.key] ?: entry.value }
                }
            }
        }
    }

    fun handleSaveProfile() {
        isSavingProfile = true
        profileMessage = ""
        profileError = ""
        coroutineScope.launch {
            val result = remoteRepository.updateProfile(displayNameInput.trim(), selectedAvatarId)
            isSavingProfile = false
            if (result.isSuccess) {
                profileMessage = "Profile updated successfully!"
            } else {
                profileError = result.exceptionOrNull()?.message ?: "Failed to update profile."
            }
        }
    }

    fun handleTestConnection() {
        isTestingConnection = true
        serverMessage = ""
        coroutineScope.launch {
            val savedUrl = serverConfigManager.saveBaseUrl(serverUrlInput.trim())
            serverUrlInput = savedUrl
            val newService = ApiClient.rebuild(context, savedUrl)
            remoteRepository.updateApiService(newService)
            val result = remoteRepository.healthCheck()
            isTestingConnection = false
            if (result.isSuccess) {
                connectionStatus = true
                serverMessage = "Connected successfully to $savedUrl"
            } else {
                connectionStatus = false
                serverMessage = "Failed to connect: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun handleSaveServerUrl() {
        val saved = serverConfigManager.saveBaseUrl(serverUrlInput.trim())
        serverUrlInput = saved
        ApiClient.rebuild(context, saved)
        serverMessage = "Server URL saved: $saved"
    }

    fun handleTogglePref(key: String, value: Boolean) {
        val updated = notifPrefs.toMutableMap()
        updated[key] = value
        notifPrefs = updated
        isSavingPrefs = true
        coroutineScope.launch {
            remoteRepository.updateNotificationSettings(updated)
            isSavingPrefs = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SETTINGS & PROFILE",
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 18.sp
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Super Admin Management Entry (Visible ONLY when role == SUPER_ADMIN)
                if (tokenManager.getRole() == "SUPER_ADMIN") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1705)),
                        border = BorderStroke(1.5.dp, Gold)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "SUPER ADMIN MANAGEMENT",
                                    color = Gold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Browse users, groups, and live tables. Rename seats, adjust player balances, and manage platform permissions.",
                                color = Cream.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = onAdminManagementClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Management Panel", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 1: Identity & Profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PROFILE & AVATAR",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )

                        // Avatar Picker Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { showAvatarPicker = true }
                                    .border(BorderStroke(2.dp, Gold), CircleShape)
                            ) {
                                PokerAvatar(
                                    avatarId = selectedAvatarId,
                                    name = displayNameInput.ifBlank { username },
                                    size = 72.dp
                                )
                            }
                            Column {
                                TextButton(
                                    onClick = { showAvatarPicker = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Gold)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Avatar (24 Choices)")
                                }
                                Text(
                                    text = "@$username",
                                    color = Cream.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }

                        // Display Name Input
                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            label = { Text("Display Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                focusedLabelColor = Gold,
                                unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (profileMessage.isNotBlank()) {
                            Text(
                                text = profileMessage,
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (profileError.isNotBlank()) {
                            Text(
                                text = profileError,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { handleSaveProfile() },
                            enabled = !isSavingProfile && displayNameInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSavingProfile) "Saving..." else "SAVE PROFILE",
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Section 2: Notification Preferences
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "NOTIFICATION PREFERENCES",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )

                        PreferenceToggle(
                            title = "New Table Created",
                            description = "Notify when a new table is opened in your groups",
                            checked = notifPrefs["table_created"] ?: true,
                            onCheckedChange = { handleTogglePref("table_created", it) }
                        )

                        HorizontalDivider(color = Gold.copy(alpha = 0.15f))

                        PreferenceToggle(
                            title = "New Member Joined",
                            description = "Notify when someone joins your groups",
                            checked = notifPrefs["member_joined"] ?: true,
                            onCheckedChange = { handleTogglePref("member_joined", it) }
                        )

                        HorizontalDivider(color = Gold.copy(alpha = 0.15f))

                        PreferenceToggle(
                            title = "Join & Chip Requests",
                            description = "Notify when player requests buy-ins or cashouts",
                            checked = notifPrefs["request_to_me"] ?: true,
                            onCheckedChange = { handleTogglePref("request_to_me", it) }
                        )

                        HorizontalDivider(color = Gold.copy(alpha = 0.15f))

                        PreferenceToggle(
                            title = "Settlement & Balances",
                            description = "Notify when group balances are settled",
                            checked = notifPrefs["settlement"] ?: true,
                            onCheckedChange = { handleTogglePref("settlement", it) }
                        )
                    }
                }

                // Section 3: Server Connection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SERVER CONNECTION",
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = serverUrlInput,
                            onValueChange = {
                                serverUrlInput = it
                                connectionStatus = null
                            },
                            label = { Text("Server Base URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                focusedLabelColor = Gold,
                                unfocusedLabelColor = Cream.copy(alpha = 0.7f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    serverUrlInput = "https://bankjoker.ir"
                                    connectionStatus = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Production", color = Gold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    serverUrlInput = "http://10.0.2.2:3000"
                                    connectionStatus = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Emulator", color = Gold, fontSize = 11.sp)
                            }
                        }

                        if (serverMessage.isNotBlank()) {
                            Text(
                                text = serverMessage,
                                color = if (connectionStatus == true) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { handleTestConnection() },
                                enabled = !isTestingConnection,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Gold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isTestingConnection) "Testing..." else "Test Connection",
                                    color = Gold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { handleSaveServerUrl() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save URL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 4: Account Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                tokenManager.clearToken()
                                onLoggedOut()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7F1D1D),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOGOUT / SWITCH ACCOUNT", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "BankPoker v1.0.0 • Hybrid Online/Offline Architecture",
                            color = Cream.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = {
                selectedAvatarId = it
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
        )
    }
}

@Composable
fun PreferenceToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Cream,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = description,
                color = Cream.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Gold,
                uncheckedThumbColor = Cream.copy(alpha = 0.7f),
                uncheckedTrackColor = FeltDark
            )
        )
    }
}
