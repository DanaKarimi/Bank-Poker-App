package com.bankpoker.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.bankpoker.app.BuildConfig
import com.bankpoker.app.data.remote.ServerConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val fileName: String,
    val apkUrl: String,
    val forceUpdate: Boolean,
    val whatsNew: List<String>,
    val sizeBytes: Long,
    val sizeMB: Double
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"

    private fun resolveFullUrl(baseUrl: String, relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            return relativeOrAbsolute
        }
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = if (relativeOrAbsolute.startsWith("/")) relativeOrAbsolute else "/$relativeOrAbsolute"
        return "$cleanBase$cleanPath"
    }

    suspend fun checkForUpdate(context: Context): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = ServerConfigManager.getInstance(context).getBaseUrl()
            if (baseUrl.isBlank()) return@withContext null
            val urlStr = resolveFullUrl(baseUrl, "/releases/version.json?t=${System.currentTimeMillis()}")
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Cache-Control", "no-cache")
            }
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val remoteCode = json.optInt("versionCode", 0)
            if (remoteCode <= BuildConfig.VERSION_CODE) {
                return@withContext null
            }

            val versionName = json.optString("versionName", "1.0")
            val fileName = json.optString("fileName", "BankPoker-v$versionName.apk")
            val rawApkUrl = json.optString("apkUrl", "/releases/$fileName")
            val fullApkUrl = resolveFullUrl(baseUrl, rawApkUrl)
            val forceUpdate = json.optBoolean("forceUpdate", true)
            val sizeBytes = json.optLong("sizeBytes", 0L)
            val sizeMB = json.optDouble("sizeMB", sizeBytes / (1024.0 * 1024.0))

            val whatsNewList = mutableListOf<String>()
            val arr = json.optJSONArray("whatsNew")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim()
                    if (item.isNotEmpty()) whatsNewList.add(item)
                }
            }

            AppReleaseInfo(
                versionName = versionName,
                versionCode = remoteCode,
                fileName = fileName,
                apkUrl = fullApkUrl,
                forceUpdate = forceUpdate,
                whatsNew = whatsNewList,
                sizeBytes = sizeBytes,
                sizeMB = sizeMB
            )
        } catch (e: Exception) {
            Log.w(TAG, "Update check skipped: ${e.message}")
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: AppReleaseInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { old -> if (old.name.endsWith(".apk")) old.delete() }
            val targetFile = File(updatesDir, release.fileName)

            val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code != 200) {
                conn.disconnect()
                return@withContext Result.failure(Exception("Download failed (HTTP $code)"))
            }

            val totalBytes = if (conn.contentLengthLong > 0L) conn.contentLengthLong else release.sizeBytes
            var downloaded = 0L
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()
            onProgress(1f)
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "APK download failed", e)
            Result.failure(e)
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
