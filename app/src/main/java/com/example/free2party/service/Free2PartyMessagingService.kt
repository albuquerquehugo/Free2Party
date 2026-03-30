package com.example.free2party.service

import android.annotation.SuppressLint
import android.util.Log
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
                message = body
            )
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveTitle(data: Map<String, String>): String? {
        val titleLocKey = data["titleLocKey"]
        if (!titleLocKey.isNullOrEmpty()) {
            val resId =
                applicationContext.resources.getIdentifier(titleLocKey, "string", packageName)
            if (resId != 0) return applicationContext.getString(resId)
        }
        return data["title"]
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveBody(data: Map<String, String>): String? {
        val bodyLocKey = data["bodyLocKey"]
        if (!bodyLocKey.isNullOrEmpty()) {
            val resId =
                applicationContext.resources.getIdentifier(bodyLocKey, "string", packageName)
            if (resId != 0) {
                val bodyLocArgsJson = data["bodyLocArgs"]
                return if (!bodyLocArgsJson.isNullOrEmpty()) {
                    try {
                        val jsonArray = JSONArray(bodyLocArgsJson)
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
        }
        return data["message"]
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
