package com.bankpoker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.dto.NotificationItemDto
import com.bankpoker.app.repository.RemoteRepository
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTable: (String) -> Unit,
    onNavigateToGroup: (String) -> Unit,
    onNavigateToRequests: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager.getInstance(context) }
    val remoteRepository = remember {
        val service = ApiClient.getApiService(context, tokenManager)
        RemoteRepository(service, tokenManager)
    }

    var notifications by remember { mutableStateOf<List<NotificationItemDto>>(emptyList()) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var filterUnreadOnly by remember { mutableStateOf(false) }

    fun loadNotifications() {
        isLoading = true
        coroutineScope.launch {
            val result = remoteRepository.getNotifications()
            isLoading = false
            if (result.isSuccess) {
                val data = result.getOrNull()
                notifications = data?.notifications ?: emptyList()
                unreadCount = data?.unreadCount ?: 0
            }
        }
    }

    fun markRead(notification: NotificationItemDto) {
        if (!notification.read) {
            coroutineScope.launch {
                remoteRepository.markNotificationRead(notification.id)
                notifications = notifications.map {
                    if (it.id == notification.id) it.copy(read = true) else it
                }
                if (unreadCount > 0) unreadCount--
            }
        }
        val targetType = notification.payload?.targetType?.uppercase()
        val targetId = notification.payload?.targetId
        val groupId = notification.payload?.groupId

        when (targetType) {
            "TABLE" -> if (!targetId.isNullOrBlank()) onNavigateToTable(targetId)
            "GROUP" -> if (!targetId.isNullOrBlank()) onNavigateToGroup(targetId)
            "REQUEST" -> if (!groupId.isNullOrBlank()) onNavigateToRequests(groupId)
            else -> {
                if (!targetId.isNullOrBlank()) onNavigateToTable(targetId)
            }
        }
    }

    fun markAllRead() {
        coroutineScope.launch {
            remoteRepository.markAllNotificationsRead()
            notifications = notifications.map { it.copy(read = true) }
            unreadCount = 0
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val displayedNotifications = remember(notifications, filterUnreadOnly) {
        if (filterUnreadOnly) notifications.filter { !it.read } else notifications
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOTIFICATIONS",
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 18.sp
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Gold, shape = CircleShape)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
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
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { markAllRead() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark All Read",
                                tint = Gold
                            )
                        }
                    }
                    IconButton(
                        onClick = { loadNotifications() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !filterUnreadOnly,
                        onClick = { filterUnreadOnly = false },
                        label = { Text("All (${notifications.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Color.Black,
                            containerColor = FeltCard,
                            labelColor = Cream
                        )
                    )
                    FilterChip(
                        selected = filterUnreadOnly,
                        onClick = { filterUnreadOnly = true },
                        label = { Text("Unread ($unreadCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold,
                            selectedLabelColor = Color.Black,
                            containerColor = FeltCard,
                            labelColor = Cream
                        )
                    )
                }

                if (isLoading && notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold)
                    }
                } else if (displayedNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Cream.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (filterUnreadOnly) "No unread notifications" else "No notifications yet",
                                color = Cream.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedNotifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = { markRead(notification) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItemDto,
    onClick: () -> Unit
) {
    val payload = notification.payload
    val title = payload?.title ?: when (notification.type) {
        "table_created" -> "New Table Created"
        "member_joined" -> "New Member Joined"
        "claim" -> "Player Claimed"
        "request_to_me" -> "Pending Request"
        "settlement" -> "Settlement Update"
        else -> "Notification"
    }
    val message = payload?.message ?: ""
    val isUnread = !notification.read

    val (icon, iconBg) = when (notification.type) {
        "table_created" -> Icons.Default.Layers to Color(0xFF065F46)
        "member_joined" -> Icons.Default.PersonAdd to Color(0xFF1E3A8A)
        "claim" -> Icons.Default.CheckCircle to Color(0xFF78350F)
        "request_to_me" -> Icons.Default.HourglassBottom to Color(0xFF7C2D12)
        "settlement" -> Icons.Default.Payments to Color(0xFF047857)
        else -> Icons.Default.Notifications to Color(0xFF374151)
    }

    val timeFormatted = remember(notification.createdAt) {
        if (notification.createdAt != null && notification.createdAt > 0) {
            val now = System.currentTimeMillis()
            val diff = now - notification.createdAt
            when {
                diff < 60_000 -> "just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    sdf.format(Date(notification.createdAt))
                }
            }
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) FeltCard.copy(alpha = 0.95f) else FeltDark.copy(alpha = 0.8f)
        ),
        border = BorderStroke(
            1.dp,
            if (isUnread) Gold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if ((notification.count ?: 1) > 1) "$title (${notification.count}x)" else title,
                        color = if (isUnread) Gold else Cream,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Gold, CircleShape)
                        )
                    }
                }

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        color = Cream.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (timeFormatted.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = timeFormatted,
                        color = Cream.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
