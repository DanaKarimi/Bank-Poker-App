package com.bankpoker.app.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthDialog(
    remoteRepository: RemoteRepository,
    tokenManager: TokenManager,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Guest, 1: Login, 2: Register
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("avatar_1") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = FeltCard,
        title = {
            Text(
                text = "Player Identity",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; errorMessage = null },
                        label = { Text("Guest", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; errorMessage = null },
                        label = { Text("Sign In", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; errorMessage = null },
                        label = { Text("Register", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = LoseRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Quick Guest Join
                        Text(
                            text = "Instant 1-tap join without a password. You can convert to a full account anytime.",
                            color = Cream.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        // Avatar Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { showAvatarPicker = true }
                                    .border(2.dp, Gold, CircleShape)
                            ) {
                                PokerAvatar(
                                    avatarId = selectedAvatarId,
                                    name = displayName.ifBlank { "Guest" },
                                    size = 56.dp
                                )
                            }
                            TextButton(onClick = { showAvatarPicker = true }) {
                                Text("Change Avatar", color = Gold)
                            }
                        }

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name", color = Cream) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (displayName.isBlank()) {
                                    errorMessage = "Please enter your display name"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = remoteRepository.guestJoin(displayName, selectedAvatarId)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Guest join failed"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                            } else {
                                Text("Join as Guest", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    1 -> {
                        // Sign In
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username", color = Cream) },
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
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Cream) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Username and password required"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val result = remoteRepository.login(username, password)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                            } else {
                                Text("Sign In", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        // Register
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { showAvatarPicker = true }
                                    .border(2.dp, Gold, CircleShape)
                            ) {
                                PokerAvatar(
                                    avatarId = selectedAvatarId,
                                    name = displayName.ifBlank { username },
                                    size = 56.dp
                                )
                            }
                            TextButton(onClick = { showAvatarPicker = true }) {
                                Text("Choose Avatar", color = Gold)
                            }
                        }

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name", color = Cream) },
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
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Unique Username", color = Cream) },
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
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Cream) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Username and password required"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val regResult = remoteRepository.register(
                                        username = username,
                                        password = password,
                                        displayName = displayName.ifBlank { username },
                                        avatarId = selectedAvatarId
                                    )
                                    if (regResult.isSuccess) {
                                        val loginResult = remoteRepository.login(username, password)
                                        isLoading = false
                                        if (loginResult.isSuccess) {
                                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        } else {
                                            errorMessage = "Registered! Please sign in."
                                            selectedTab = 1
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = regResult.exceptionOrNull()?.message ?: "Registration failed"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                            } else {
                                Text("Create Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = Cream)
            }
        }
    )

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedAvatarId = selectedAvatarId,
            onAvatarSelected = { selectedAvatarId = it },
            onDismiss = { showAvatarPicker = false }
        )
    }
}

@Composable
fun ProfileDialog(
    remoteRepository: RemoteRepository,
    tokenManager: TokenManager,
    onDismiss: () -> Unit,
    onProfileUpdated: () -> Unit,
    onLogout: () -> Unit,
    onAdminManagementClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser = tokenManager.getUser()
    val isGuest = tokenManager.isGuest()

    var editDisplayName by remember { mutableStateOf(currentUser?.displayName ?: tokenManager.getDisplayName() ?: "") }
    var editAvatarId by remember { mutableStateOf(currentUser?.avatarId ?: tokenManager.getAvatarId()) }
    var activateUsername by remember { mutableStateOf("") }
    var activatePassword by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showActivateSection by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = FeltCard,
        title = {
            Text(
                text = "Player Profile",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User Badge Preview
                UserBadge(
                    displayName = editDisplayName.ifBlank { currentUser?.username ?: "Player" },
                    username = currentUser?.username,
                    avatarId = editAvatarId,
                    role = currentUser?.role,
                    avatarSize = 56.dp
                )

                TextButton(onClick = { showAvatarPicker = true }) {
                    Text("Change Avatar", color = Gold, fontSize = 13.sp)
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = LoseRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = { Text("Display Name", color = Cream) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (editDisplayName.isBlank()) {
                            errorMessage = "Display name cannot be empty"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = remoteRepository.updateProfile(editDisplayName, editAvatarId)
                            isLoading = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                onProfileUpdated()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Update failed"
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    } else {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }

                // If Guest, show Activate Account option
                if (isGuest) {
                    HorizontalDivider(color = Gold.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Guest Account",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Convert your guest account to a permanent account with a username & password to never lose your games.",
                                color = Cream.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )

                            if (!showActivateSection) {
                                OutlinedButton(
                                    onClick = { showActivateSection = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Gold)
                                ) {
                                    Text("Activate Account", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedTextField(
                                    value = activateUsername,
                                    onValueChange = { activateUsername = it },
                                    label = { Text("Choose Username", color = Cream) },
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
                                    value = activatePassword,
                                    onValueChange = { activatePassword = it },
                                    label = { Text("Choose Password", color = Cream) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Gold,
                                        unfocusedBorderColor = Gold.copy(alpha = 0.5f),
                                        focusedTextColor = Cream,
                                        unfocusedTextColor = Cream
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (activateUsername.isBlank() || activatePassword.isBlank()) {
                                            errorMessage = "Username and password required to activate"
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val result = remoteRepository.activateAccount(activateUsername, activatePassword)
                                            isLoading = false
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Account permanently activated!", Toast.LENGTH_SHORT).show()
                                                showActivateSection = false
                                                onProfileUpdated()
                                            } else {
                                                errorMessage = result.exceptionOrNull()?.message ?: "Activation failed"
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Confirm Activation", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Super Admin Management Entry (Visible ONLY when role == SUPER_ADMIN)
                if (currentUser?.role == "SUPER_ADMIN" || tokenManager.getRole() == "SUPER_ADMIN") {
                    HorizontalDivider(color = Gold.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onAdminManagementClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF422006),
                            contentColor = Gold
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp), tint = Gold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Management", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Gold.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                // Sign out button
                TextButton(
                    onClick = {
                        tokenManager.clearAll()
                        Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LoseRed)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Close", color = Cream)
            }
        }
    )

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedAvatarId = editAvatarId,
            onAvatarSelected = { editAvatarId = it },
            onDismiss = { showAvatarPicker = false }
        )
    }
}
