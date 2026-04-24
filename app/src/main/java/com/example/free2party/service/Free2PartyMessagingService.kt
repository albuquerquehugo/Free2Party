package com.example.free2party.service

import android.annotation.SuppressLint
import android.util.Log
import com.example.free2party.R
import com.example.free2party.data.repository.SettingsRepository
import com.example.free2party.data.repository.UserRepositoryImpl
import com.example.free2party.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException

class Free2PartyMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val dataPayload = remoteMessage.data

        if (dataPayload.isNotEmpty()) {
            val action = dataPayload["action"]
            val notificationId =
                dataPayload["notificationId"] ?: System.currentTimeMillis().toString()

            if (action == "DISMISS") {
                Log.d(TAG, "Action DISMISS received for $notificationId")
                NotificationHelper.dismissNotification(applicationContext, notificationId)

                serviceScope.launch {
                    val settingsRepository = SettingsRepository(applicationContext)
                    settingsRepository.clearShownNotification(notificationId)
                }
                return
            }

            val title = resolveTitle(dataPayload) ?: "Free2Party"
            val body = resolveBody(dataPayload) ?: ""

            serviceScope.launch {
                val settingsRepository = SettingsRepository(applicationContext)
                settingsRepository.markNotificationAsShown(notificationId)
                Log.d(TAG, "Notification $notificationId marked as shown in DataStore")
            }

            NotificationHelper.showNotification(
                context = applicationContext,
                notificationId = notificationId,
                title = title,
                message = body,
            )
        }
    }

    private fun getManualResId(key: String?): Int {
        return when (key) {
            "notification_friend_request_received_title" -> R.string.notification_friend_request_received_title
            "notification_friend_request_received_body" -> R.string.notification_friend_request_received_body
            "notification_friend_request_accepted_title" -> R.string.notification_friend_request_accepted_title
            "notification_friend_request_accepted_body" -> R.string.notification_friend_request_accepted_body
            "notification_friend_request_declined_title" -> R.string.notification_friend_request_declined_title
            "notification_friend_request_declined_body" -> R.string.notification_friend_request_declined_body
            "notification_friend_removed_title" -> R.string.notification_friend_removed_title
            "notification_friend_removed_body" -> R.string.notification_friend_removed_body
            else -> 0
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveTitle(data: Map<String, String>): String? {
        val locKey = data["titleLocKey"] ?: return data["title"]

        val resId = getManualResId(locKey).takeIf { it != 0 }
            ?: applicationContext.resources.getIdentifier(
                locKey,
                "string",
                applicationContext.packageName
            )

        return if (resId != 0) applicationContext.getString(resId) else data["title"]
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveBody(data: Map<String, String>): String? {
        val locKey = data["bodyLocKey"] ?: return data["message"]

        val resId = getManualResId(locKey).takeIf { it != 0 }
            ?: applicationContext.resources.getIdentifier(
                locKey,
                "string",
                applicationContext.packageName
            )

        if (resId == 0) return data["message"]

        val argsJson = data["bodyLocArgs"]
        return if (!argsJson.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(argsJson)
                val args = Array(jsonArray.length()) { i -> jsonArray.getString(i) }
                applicationContext.getString(resId, *args)
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing bodyLocArgs", e)
                applicationContext.getString(resId)
            }
        } else {
            applicationContext.getString(resId)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userRepository = UserRepositoryImpl(
            auth = Firebase.auth,
            db = Firebase.firestore,
            storage = Firebase.storage
        )
        serviceScope.launch { userRepository.updateFcmToken(token) }
    }

    companion object {
        private const val TAG = "F2PMessagingService"
    }
}
