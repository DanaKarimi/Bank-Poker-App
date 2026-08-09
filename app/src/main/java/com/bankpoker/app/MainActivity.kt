package com.bankpoker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bankpoker.app.data.local.BankPokerDatabase
import com.bankpoker.app.ui.navigation.AppNavigation
import com.bankpoker.app.ui.theme.BankPokerTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: BankPokerDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        database = BankPokerDatabase.getDatabase(this)
        
        setContent {
            BankPokerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        database = database
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Room database will be closed when app process is terminated
    }
}
