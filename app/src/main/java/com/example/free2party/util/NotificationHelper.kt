package com.example.free2party.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.free2party.MainActivity
import com.example.free2party.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "f2p_social"
    private const val CHANNEL_NAME = "Social Notifications"
    private const val CHANNEL_DESC = "Notifications about friend requests and social activity"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }

    fun showNotification(
        context: Context,
        notificationId: String,
        title: String,
        message: String
    ) {
        Log.d(TAG, "Attempting to show notification: ID=$notificationId, Title=$title")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Missing POST_NOTIFICATIONS permission")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_ID", notificationId)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            notificationId.hashCode(), 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.free2party_foreground)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.free2party_noborder_transparent)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId.hashCode(), builder.build())
                Log.d(TAG, "Notification posted successfully: ${notificationId.hashCode()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }
    }

    fun dismissNotification(context: Context, notificationId: String) {
        Log.d(TAG, "Dismissing notification: $notificationId")
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId.hashCode())
        }
    }
}
