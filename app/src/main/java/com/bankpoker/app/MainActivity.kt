package com.bankpoker.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bankpoker.app.data.local.BankPokerDatabase
import com.bankpoker.app.ui.components.AppUpdateDialog
import com.bankpoker.app.ui.navigation.AppNavigation
import com.bankpoker.app.ui.theme.BankPokerTheme
import com.bankpoker.app.update.AppReleaseInfo
import com.bankpoker.app.update.AppUpdateManager

class MainActivity : ComponentActivity() {
    private lateinit var database: BankPokerDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("MainActivity", "onCreate start")

        database = BankPokerDatabase.getDatabase(this)
        Log.d("MainActivity", "Database initialized")

        // Initialize ApiClient with persisted Base URL from SharedPreferences
        com.bankpoker.app.data.remote.ApiClient.initialize(this)
        Log.d("MainActivity", "ApiClient initialized")

        // Trigger safe FCM token synchronization
        com.bankpoker.app.service.BankPokerMessagingService.syncCurrentToken(this)

        setContent {
            Log.d("MainActivity", "setContent compose")
            BankPokerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var pendingUpdate by remember { mutableStateOf<AppReleaseInfo?>(null) }

                    LaunchedEffect(Unit) {
                        pendingUpdate = AppUpdateManager.checkForUpdate(this@MainActivity)
                    }

                    AppNavigation(
                        navController = navController,
                        database = database
                    )

                    pendingUpdate?.let { release ->
                        AppUpdateDialog(
                            release = release,
                            onDismiss = { pendingUpdate = null }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Room database will be closed when app process is terminated
    }
}
