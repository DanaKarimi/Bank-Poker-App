package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.RequestDto
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(
    groupId: String,
    groupName: String = "Online Group",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Join, 1: Buy-In, 2: Exit
    val tabs = listOf("Join", "Buy-In", "Exit")

    var joinRequests by remember { mutableStateOf<List<RequestDto>>(emptyList()) }
    var buyInRequests by remember { mutableStateOf<List<RequestDto>>(emptyList()) }
    var exitRequests by remember { mutableStateOf<List<RequestDto>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionInProgressId by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    // Load pending requests from server
    suspend fun loadPendingRequests(showLoader: Boolean = false) {
        if (showLoader) isLoading = true
        else isRefreshing = true

        val result = remoteRepository.getPendingRequests(groupId)

        isLoading = false
        isRefreshing = false

        if (result.isSuccess) {
            val response = result.getOrNull()
            joinRequests = response?.joinRequests ?: emptyList()
            buyInRequests = response?.buyInRequests ?: emptyList()
            exitRequests = response?.exitRequests ?: emptyList()
            errorMessage = null
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to load requests."
        }
    }

    // Polling every 10 seconds while active
    LaunchedEffect(groupId) {
        loadPendingRequests(showLoader = true)
        while (isActive) {
            delay(10000)
            loadPendingRequests(showLoader = false)
        }
    }

    // Action Handlers
    fun handleApprove(request: RequestDto, type: String) {
        actionInProgressId = request.id
        coroutineScope.launch {
            val result = when (type) {
                "join" -> remoteRepository.approveJoinRequest(request.id)
                "buy-in" -> remoteRepository.approveBuyInRequest(request.id)
                "exit" -> remoteRepository.approveExitRequest(request.id)
                else -> Result.failure(Exception("Unknown type"))
            }
            actionInProgressId = null

            if (result.isSuccess) {
                Toast.makeText(context, "Request approved!", Toast.LENGTH_SHORT).show()
                loadPendingRequests(showLoader = false)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Approval failed."
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun handleReject(request: RequestDto, type: String) {
        actionInProgressId = request.id
        coroutineScope.launch {
            val result = when (type) {
                "join" -> remoteRepository.rejectJoinRequest(request.id)
                "buy-in" -> remoteRepository.rejectBuyInRequest(request.id)
                "exit" -> remoteRepository.rejectExitRequest(request.id)
                else -> Result.failure(Exception("Unknown type"))
            }
            actionInProgressId = null

            if (result.isSuccess) {
                Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show()
                loadPendingRequests(showLoader = false)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Rejection failed."
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♠", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PENDING REQUESTS",
                                style = MaterialTheme.typography.titleMedium,
                                color = Cream,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gold.copy(alpha = 0.8f)
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
                    IconButton(
                        onClick = {
                            coroutineScope.launch { loadPendingRequests(showLoader = false) }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) WinGreen else Gold
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
                // Custom Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(FeltCard, RoundedCornerShape(16.dp))
                        .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val count = when (index) {
                            0 -> joinRequests.size
                            1 -> buyInRequests.size
                            2 -> exitRequests.size
                            else -> 0
                        }
                        val isSelected = selectedTabIndex == index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF041C0E) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Gold else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) Gold else Cream.copy(alpha = 0.7f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )

                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSelected) Gold else Gold.copy(alpha = 0.6f), CircleShape)
                                            .size(18.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Notification
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { err ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = LoseRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Request List Content
                if (isLoading && joinRequests.isEmpty() && buyInRequests.isEmpty() && exitRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold)
                    }
                } else {
                    val currentList = when (selectedTabIndex) {
                        0 -> joinRequests
                        1 -> buyInRequests
                        2 -> exitRequests
                        else -> emptyList()
                    }
                    val currentType = when (selectedTabIndex) {
                        0 -> "join"
                        1 -> "buy-in"
                        2 -> "exit"
                        else -> ""
                    }

                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "♠",
                                    fontSize = 40.sp,
                                    color = Gold.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No pending ${tabs[selectedTabIndex].lowercase()} requests",
                                    color = Cream.copy(alpha = 0.6f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Auto-checking every 10 seconds",
                                    color = Cream.copy(alpha = 0.4f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentList, key = { it.id }) { req ->
                                RequestAdminCard(
                                    request = req,
                                    type = currentType,
                                    dateFormat = dateFormat,
                                    isProcessing = actionInProgressId == req.id,
                                    onApprove = { handleApprove(req, currentType) },
                                    onReject = { handleReject(req, currentType) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestAdminCard(
    request: RequestDto,
    type: String,
    dateFormat: SimpleDateFormat,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FeltCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: User info & time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF041C0E), CircleShape)
                            .border(1.dp, Gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = request.username ?: "Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = Cream,
                            fontWeight = FontWeight.Bold
                        )
                        if (!request.tableName.isNullOrBlank()) {
                            Text(
                                text = "Table: ${request.tableName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cream.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                Text(
                    text = if (request.createdAt > 0) dateFormat.format(Date(request.createdAt)) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Amount Row (if buy-in or exit)
            if (request.amount != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF041C0E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (type == "buy-in") "BUY-IN AMOUNT" else "CASHOUT AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gold.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${request.amount.toLocaleString()} chips",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (type == "buy-in") WinGreen else Color(0xFFFFB74D),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action Buttons (Approve & Reject)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reject Button
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = LoseRed,
                        containerColor = Color.Transparent
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(LoseRed, LoseRed.copy(alpha = 0.6f)))
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = LoseRed
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontWeight = FontWeight.Bold, color = LoseRed)
                }

                // Approve Button
                Button(
                    onClick = onApprove,
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun Number.toLocaleString(): String {
    return String.format(Locale.getDefault(), "%,d", this.toLong())
}
