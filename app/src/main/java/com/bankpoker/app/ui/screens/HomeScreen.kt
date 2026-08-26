package com.bankpoker.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: PokerRepository,
    onQuickTableClick: () -> Unit,
    onGroupsClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = repository.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    snackbarHostState.showSnackbar("Backup exported successfully!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!json.isNullOrBlank()) {
                        repository.restoreBackupJson(json)
                        snackbarHostState.showSnackbar("Backup restored successfully!")
                    } else {
                        snackbarHostState.showSnackbar("Failed to read backup file.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message ?: "Invalid backup file"}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Gold
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(FeltCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Backup", color = Cream) },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("BankPoker_backup.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Backup", color = Cream) },
                                onClick = {
                                    showMenu = false
                                    showConfirmRestoreDialog = true
                                }
                            )
                        }
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hero Header
                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                        fontSize = 72.sp,
                        color = Gold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "BANK POKER",
                    style = MaterialTheme.typography.displaySmall,
                    color = Cream,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
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

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.5.dp)
                        .background(Gold, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.height(40.dp))

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

    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            containerColor = FeltCard,
            title = { Text("Restore Backup?", color = Gold, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This replaces ALL current data!",
                    color = Cream,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmRestoreDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = false }) {
                    Text("Cancel", color = Gold)
                }
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
            .height(210.dp)
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


