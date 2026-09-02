package com.bankpoker.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.ui.components.GoldGradientButton
import com.bankpoker.app.ui.components.SectionHeader
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (String) -> Unit,
    onCreateServerGroupClick: () -> Unit = {}
) {
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val groups by viewModel.groups.collectAsState(initial = emptyList())

    val filteredGroups = remember(groups, searchQuery) {
        if (searchQuery.isBlank()) groups
        else groups.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GROUPS",
                            color = Cream,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FeltBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupSheet = true },
                modifier = Modifier.shadow(12.dp, CircleShape),
                containerColor = Gold,
                contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Group",
                    modifier = Modifier.size(28.dp)
                )
            }
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

                if (groups.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search groups...", color = Cream.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Gold
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Gold
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream,
                            cursorColor = Gold,
                            focusedContainerColor = FeltCard,
                            unfocusedContainerColor = FeltCard
                        )
                    )
                }

                if (groups.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "♠",
                            fontSize = 96.sp,
                            color = Gold.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO GROUPS YET",
                            style = MaterialTheme.typography.titleLarge,
                            color = Cream,
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to create a new group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Cream.copy(alpha = 0.6f)
                        )
                    }
                } else if (filteredGroups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No groups found",
                            color = Cream.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredGroups, key = { it.id }) { group ->
                            GroupCard(
                                group = group,
                                onClick = { onGroupClick(group.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupSheet) {
        CreateGroupBottomSheet(
            onDismiss = { showCreateGroupSheet = false },
            onCreate = { name, mode, onSuccess, onError ->
                viewModel.createGroup(name, mode, onSuccess, onError)
            }
        )
    }
}

@Composable
fun GroupCard(
    group: PlayerGroup,
    onClick: () -> Unit
) {
    val isOnline = group.mode == "ONLINE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                color = if (isOnline) Gold else Gold.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    text = if (isOnline) "🌐" else "👥",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = if (isOnline) WinGreen.copy(alpha = 0.2f) else Gold.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isOnline) WinGreen.copy(alpha = 0.5f) else Gold.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isOnline) "ONLINE GROUP" else "OFFLINE GROUP",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) WinGreen else Gold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, mode: String, onSuccess: (PlayerGroup) -> Unit, onError: (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var groupName by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("OFFLINE") } // "OFFLINE" or "ONLINE"
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var createdInviteCode by remember { mutableStateOf<String?>(null) }
    var createdGroupName by remember { mutableStateOf<String?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FeltCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Gold) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionHeader(title = "NEW POKER GROUP", suit = "♠")

            Spacer(modifier = Modifier.height(16.dp))

            // Live Preview Card
            Text(
                text = "CARD PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier
                    .width(190.dp)
                    .border(
                        width = 1.5.dp,
                        color = Gold.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FeltCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                            text = if (selectedMode == "ONLINE") "🌐" else "👥",
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = groupName.ifBlank { "Group Name" },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (groupName.isBlank()) Cream.copy(alpha = 0.4f) else Cream,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedMode == "ONLINE") "ONLINE" else "OFFLINE",
                        color = if (selectedMode == "ONLINE") WinGreen else Gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group Name Input
            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    if (error != null) error = null
                },
                label = { Text("Group Name", color = Cream.copy(alpha = 0.7f)) },
                placeholder = { Text("e.g. Friday Night Poker", color = Cream.copy(alpha = 0.4f)) },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = Gold.copy(alpha = 0.4f),
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = Gold,
                    focusedContainerColor = FeltBackground,
                    unfocusedContainerColor = FeltBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Online / Offline Mode Toggle directly in dialog
            Text(
                text = "GROUP TYPE",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OFFLINE Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = "OFFLINE" }
                        .border(
                            width = if (selectedMode == "OFFLINE") 2.dp else 1.dp,
                            color = if (selectedMode == "OFFLINE") Gold else Gold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "OFFLINE") Color(0xFF041C0E) else FeltBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎲 Offline (Local)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMode == "OFFLINE") Gold else Cream.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Admin manages all tables",
                            fontSize = 10.sp,
                            color = Cream.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ONLINE Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMode = "ONLINE" }
                        .border(
                            width = if (selectedMode == "ONLINE") 2.dp else 1.dp,
                            color = if (selectedMode == "ONLINE") Gold else Gold.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "ONLINE") Color(0xFF041C0E) else FeltBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🌐 Online (Server Sync)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMode == "ONLINE") Gold else Cream.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Live requests + invite code",
                            fontSize = 10.sp,
                            color = Cream.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = LoseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            GoldGradientButton(
                text = if (isSubmitting) "CREATING..." else "CREATE GROUP",
                onClick = {
                    val trimmed = groupName.trim()
                    if (trimmed.isBlank()) {
                        error = "Group name is required"
                        return@GoldGradientButton
                    }
                    isSubmitting = true
                    error = null

                    onCreate(
                        trimmed,
                        selectedMode,
                        { createdGroup ->
                            isSubmitting = false
                            if (selectedMode == "ONLINE" && createdGroup.inviteCode != null) {
                                createdInviteCode = createdGroup.inviteCode
                                createdGroupName = createdGroup.name
                                showInviteDialog = true
                            } else {
                                Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        { errorMsg ->
                            isSubmitting = false
                            error = errorMsg
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }

    // Success Invite Code Dialog for Online Groups
    if (showInviteDialog && createdInviteCode != null) {
        Dialog(onDismissRequest = {
            showInviteDialog = false
            onDismiss()
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
                        text = "♠ ONLINE GROUP CREATED ♠",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = createdGroupName ?: "Your Group",
                        style = MaterialTheme.typography.titleLarge,
                        color = Cream,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Share this invite code with players so they can join and send requests via Web or App:",
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
                            fontSize = 32.sp,
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
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
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
                                showInviteDialog = false
                                onDismiss()
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
