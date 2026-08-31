package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.GoldGradientButton
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var groupName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createdInviteCode by remember { mutableStateOf<String?>(null) }
    var createdGroupName by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    val hasToken = !tokenManager.getToken().isNullOrBlank()

    fun handleCreateGroup() {
        if (groupName.isBlank()) {
            errorMessage = "Please enter a valid group name."
            return
        }

        if (!hasToken) {
            errorMessage = "Authentication required. Please log in as an Admin via Server Test first."
            return
        }

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            val result = remoteRepository.createGroup(groupName.trim())
            isLoading = false

            if (result.isSuccess) {
                val data = result.getOrNull()
                createdInviteCode = data?.inviteCode
                createdGroupName = groupName.trim()
                showSuccessDialog = true
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to create group."
            }
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
                            text = "CREATE SERVER GROUP",
                            style = MaterialTheme.typography.titleMedium,
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
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
                    containerColor = Color.Transparent
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF062319), RoundedCornerShape(18.dp))
                                .border(1.5.dp, Gold, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♣",
                                color = Gold,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Host a New Poker Group",
                            style = MaterialTheme.typography.titleLarge,
                            color = Gold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Create a cloud group on the BankPoker server. Players will be able to join using the 6-character invite code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cream.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Input Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "GROUP DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = groupName,
                            onValueChange = {
                                groupName = it
                                errorMessage = null
                            },
                            label = { Text("Group Name", color = Cream.copy(alpha = 0.7f)) },
                            placeholder = { Text("e.g. Friday Night High Stakes", color = Cream.copy(alpha = 0.35f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                                cursorColor = Gold
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = LoseRed.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFFF8A80),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        GoldGradientButton(
                            text = if (isLoading) "CREATING GROUP..." else "CREATE GROUP",
                            onClick = { handleCreateGroup() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Success Dialog with Large Invite Code
    if (showSuccessDialog && createdInviteCode != null) {
        Dialog(onDismissRequest = {
            showSuccessDialog = false
            onNavigateBack()
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Gold, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "♠ GROUP CREATED ♠",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = createdGroupName ?: "Your Group",
                        style = MaterialTheme.typography.titleLarge,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Share this invite code with players so they can join on their app or web dashboard:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    // Invite Code Display Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF041C0E), RoundedCornerShape(16.dp))
                            .border(1.5.dp, Gold, RoundedCornerShape(16.dp))
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = createdInviteCode!!,
                            color = Gold,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(createdInviteCode!!))
                                isCopied = true
                                Toast.makeText(context, "Invite code copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Gold,
                                containerColor = FeltCard
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(Gold, Gold.copy(alpha = 0.6f)))
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Gold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCopied) "Copied" else "Copy Code", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showSuccessDialog = false
                                onNavigateBack()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
