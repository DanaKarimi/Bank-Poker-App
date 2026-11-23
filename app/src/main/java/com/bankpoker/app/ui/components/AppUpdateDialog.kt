package com.bankpoker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.bankpoker.app.ui.theme.*
import com.bankpoker.app.update.AppReleaseInfo
import com.bankpoker.app.update.AppUpdateManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun AppUpdateDialog(
    release: AppReleaseInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var needsPermissionHint by remember { mutableStateOf(false) }

    fun triggerInstall(file: File) {
        if (!AppUpdateManager.canInstallPackages(context)) {
            needsPermissionHint = true
            AppUpdateManager.openInstallPermissionSettings(context)
        } else {
            needsPermissionHint = false
            AppUpdateManager.launchApkInstaller(context, file)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!release.forceUpdate && !isDownloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !release.forceUpdate && !isDownloading,
            dismissOnClickOutside = !release.forceUpdate && !isDownloading
        ),
        containerColor = FeltCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("♠", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = if (release.forceUpdate) "Update Required" else "Update Available",
                        color = Gold,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                val sizeStr = String.format(Locale.US, "%.1f MB", release.sizeMB)
                Text(
                    text = "BankPoker v${release.versionName} • $sizeStr (${release.fileName})",
                    color = Cream.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (release.whatsNew.isNotEmpty()) {
                    Surface(
                        color = FeltBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "WHAT'S NEW",
                                color = Gold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                            release.whatsNew.forEach { bullet ->
                                Text(
                                    text = "• $bullet",
                                    color = Cream,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Gold,
                            trackColor = FeltBackground
                        )
                        Text(
                            text = "Downloading ${release.fileName}... ${(progress * 100).toInt()}%",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (needsPermissionHint) {
                    Text(
                        text = "Allow \"Install unknown apps\" for BankPoker in Settings, then tap Install Update again.",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = LoseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val existing = downloadedFile
                    if (existing != null && existing.exists()) {
                        triggerInstall(existing)
                        return@Button
                    }
                    isDownloading = true
                    progress = 0f
                    errorMessage = null
                    scope.launch {
                        val res = AppUpdateManager.downloadApk(context, release) { p ->
                            progress = p
                        }
                        isDownloading = false
                        if (res.isSuccess) {
                            val file = res.getOrNull()!!
                            downloadedFile = file
                            triggerInstall(file)
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Download failed. Please retry."
                        }
                    }
                },
                enabled = !isDownloading,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (downloadedFile != null) "Install Update" else "Download & Install Update",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            if (!release.forceUpdate && !isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = Cream.copy(alpha = 0.75f))
                }
            }
        }
    )
}
