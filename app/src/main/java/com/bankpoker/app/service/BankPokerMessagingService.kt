package com.bankpoker.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bankpoker.app.BuildConfig
import com.bankpoker.app.MainActivity
import com.bankpoker.app.R
import com.bankpoker.app.data.remote.TokenManager
import com.bankpoker.app.data.remote.ApiClient
import com.bankpoker.app.repository.RemoteRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankPokerMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "BankPokerFCM"
        const val CHANNEL_ID = "bankpoker_alerts_channel"

        /**
         * Safely synchronize current FCM token on login or app start.
         * Gracefully handles placeholder Firebase configuration or missing Google Play Services.
         */
        fun syncCurrentToken(context: Context) {
            if (!BuildConfig.ENABLE_FCM) {
                Log.d(TAG, "FCM disabled via BuildConfig. Skipping token sync.")
                return
            }

            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        Log.d(TAG, "Fetched FCM token: $token")
                        sendTokenToServer(context, token)
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase unavailable or not configured. Graceful fallback active.", e)
            }
        }

        private fun sendTokenToServer(context: Context, fcmToken: String) {
            val tokenManager = TokenManager(context.applicationContext)
            if (!tokenManager.isLoggedIn()) {
                Log.d(TAG, "User not logged in yet. Token will sync on login.")
                return
            }

            val apiService = ApiClient.getApiService(tokenManager)
            val repository = RemoteRepository(apiService, tokenManager)

            CoroutineScope(Dispatchers.IO).launch {
                val result = repository.registerFcmToken(fcmToken)
                if (result.isSuccess) {
                    Log.d(TAG, "FCM token successfully registered with server.")
                } else {
                    Log.w(TAG, "FCM token registration failed on server: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken received: $token")
        if (BuildConfig.ENABLE_FCM) {
            sendTokenToServer(applicationContext, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "BankPoker Alert"
        val message = remoteMessage.notification?.body 
            ?: remoteMessage.data["message"] 
            ?: remoteMessage.data["body"] 
            ?: "New table action"

        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BankPoker Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "BankPoker table updates and alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
