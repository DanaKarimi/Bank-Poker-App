package com.bankpoker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.ApiConfig
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.components.GoldGradientButton
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerTestScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var baseUrlInput by remember { mutableStateOf(ApiConfig.getEffectiveBaseUrl()) }
    var usernameInput by remember { mutableStateOf("admin") }
    var passwordInput by remember { mutableStateOf("password123") }
    var responseLog by remember { mutableStateOf("Ready to test server connection.") }
    var isLoading by remember { mutableStateOf(false) }
    var currentToken by remember { mutableStateOf(tokenManager.getToken()) }

    fun updateBaseUrl() {
        ApiConfig.customBaseUrl = baseUrlInput.trim()
        ApiClient.resetClient()
        val newService = ApiClient.getApiService(tokenManager, ApiConfig.getEffectiveBaseUrl())
        remoteRepository.updateApiService(newService)
        responseLog = "Base URL updated to: ${ApiConfig.getEffectiveBaseUrl()}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SERVER TEST",
                        style = MaterialTheme.typography.titleLarge,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Server URL Configuration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Gold.copy(alpha = 0.7f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FeltCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🌐 SERVER CONFIGURATION",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            label = { Text("Base URL", color = Cream.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { updateBaseUrl() },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Apply URL", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    baseUrlInput = ApiConfig.DEFAULT_BASE_URL
                                    updateBaseUrl()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset 10.0.2.2")
                            }
                        }
                    }
                }

                // Section 2: Health Check Test
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "❤️ HEALTH CHECK",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        GoldGradientButton(
                            text = if (isLoading) "TESTING..." else "TEST CONNECTION (/api/health)",
                            onClick = {
                                if (!isLoading) {
                                    isLoading = true
                                    responseLog = "Calling GET /api/health..."
                                    coroutineScope.launch {
                                        val result = remoteRepository.healthCheck()
                                        isLoading = false
                                        responseLog = if (result.isSuccess) {
                                            val data = result.getOrNull()
                                            "✅ HEALTH CHECK SUCCESSFUL!\nStatus: ${data?.status}\nTimestamp: ${data?.timestamp}"
                                        } else {
                                            "❌ ERROR: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Section 3: Auth Test (Register / Login)
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🔑 AUTHENTICATION TEST",
                            style = MaterialTheme.typography.labelLarge,
                            color = Gold,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username", color = Cream.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password", color = Cream.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Cream,
                                unfocusedTextColor = Cream,
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Gold.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isLoading) {
                                        isLoading = true
                                        responseLog = "Calling POST /api/auth/register..."
                                        coroutineScope.launch {
                                            val result = remoteRepository.register(usernameInput, passwordInput, "ADMIN")
                                            isLoading = false
                                            responseLog = if (result.isSuccess) {
                                                val data = result.getOrNull()
                                                "✅ REGISTRATION SUCCESS!\nUser ID: ${data?.user?.id}\nUsername: ${data?.user?.username}\nRole: ${data?.user?.role}"
                                            } else {
                                                "❌ REGISTRATION FAILED:\n${result.exceptionOrNull()?.message}"
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WinGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Register", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (!isLoading) {
                                        isLoading = true
                                        responseLog = "Calling POST /api/auth/login..."
                                        coroutineScope.launch {
                                            val result = remoteRepository.login(usernameInput, passwordInput)
                                            isLoading = false
                                            currentToken = tokenManager.getToken()
                                            responseLog = if (result.isSuccess) {
                                                val data = result.getOrNull()
                                                "✅ LOGIN SUCCESS!\nToken: ${data?.token?.take(20)}...\nUsername: ${data?.user?.username}\nRole: ${data?.user?.role}"
                                            } else {
                                                "❌ LOGIN FAILED:\n${result.exceptionOrNull()?.message}"
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Login", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!currentToken.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    tokenManager.clearAll()
                                    currentToken = null
                                    responseLog = "Token cleared."
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = LoseRed),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear Saved Token")
                            }
                        }
                    }
                }

                // Section 4: Live Response Console
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF072413))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📟 RESPONSE CONSOLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Gold,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Gold,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Text(
                            text = responseLog,
                            color = Cream,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
