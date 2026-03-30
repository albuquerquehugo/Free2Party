const {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

setGlobalOptions({maxInstances: 10});

/**
 * Triggers when a new document is created in the 'friendRequests' collection.
 * Sends a push notification to the receiver of the request.
 */
exports.onFriendRequestCreated = onDocumentCreated(
    "friendRequests/{requestId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) return;

      const requestData = snapshot.data();
      const receiverId = requestData.receiverId;
      const senderName = requestData.senderName;
      const senderEmail = requestData.senderEmail;

      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(receiverId).get();
        if (!userDoc.exists) return;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;

        const message = {
          token: fcmToken,
          data: {
            notificationId: event.params.requestId,
            type: "FRIEND_REQUEST_RECEIVED",
            titleLocKey: "notification_system_friend_request_title",
            bodyLocKey: "notification_system_friend_request_body",
            bodyLocArgs: JSON.stringify([senderName, senderEmail]),
            // Fallback for old app versions
            title: "New Friend Request",
            message:
              `${senderName} (${senderEmail}) sent you a friend request!`,
          },
          android: {priority: "high"},
        };

        await admin.messaging().send(message);
        logger.info(`Friend request notification sent to ${receiverId}`);
      } catch (error) {
        logger.error("Error sending notification:", error);
      }
    },
);

/**
 * Triggers when a new notification is created for a user
 * (except friend request decline or friend removal).
 * Sends a push notification to the current user.
 */
exports.onNotificationCreated = onDocumentCreated(
    "users/{userId}/notifications/{notificationId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) return;

      const notifData = snapshot.data();
      const userId = event.params.userId;

      if (notifData.type === "FRIEND_DECLINED" ||
          notifData.type === "FRIEND_REMOVED") {
        return;
      }

      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(userId).get();
        if (!userDoc.exists) return;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;

        const notificationId = event.params.notificationId;

        let titleLocKey = undefined;
        let bodyLocKey = undefined;
        let bodyLocArgs = undefined;

        if (notifData.type === "FRIEND_ADDED") {
          titleLocKey = "notification_system_friend_accepted_title";
          bodyLocKey = "notification_system_friend_accepted_body";
          const match = notifData.message.match(/(.*) \((.*)\) was added/);
          if (match) {
            bodyLocArgs = [match[1], match[2]];
          } else {
            bodyLocArgs = [notifData.message];
          }
        }

        const message = {
          token: fcmToken,
          data: {
            notificationId: notificationId,
            type: notifData.type || "GENERAL",
            titleLocKey: titleLocKey,
            bodyLocKey: bodyLocKey || "",
            bodyLocArgs: bodyLocArgs ? JSON.stringify(bodyLocArgs) : "",
            // Fallback for old app versions
            title: notifData.title || "",
            message: notifData.message || "",
          },
          android: {priority: "high"},
        };

        await admin.messaging().send(message);
        logger.info(
            `Push notification sent to ${userId} for ${notifData.type}`,
        );
      } catch (error) {
        logger.error("Error sending push notification:", error);
      }
    },
);

/**
 * Dismisses the push notification when the friend request is deleted
 */
exports.onFriendRequestDeleted = onDocumentDeleted(
    "friendRequests/{requestId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) return;
      const requestData = snapshot.data();
      const receiverId = requestData.receiverId;
      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(receiverId).get();
        if (!userDoc.exists) return;
        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;
        const message = {
          token: fcmToken,
          data: {action: "DISMISS", notificationId: event.params.requestId},
          android: {priority: "high"},
        };
        await admin.messaging().send(message);
      } catch (error) {
        logger.error("Error sending dismissal:", error);
      }
    },
);

/**
 * Dismisses the push notification when the in-app notification is deleted
 */
exports.onNotificationDeleted = onDocumentDeleted(
    "users/{userId}/notifications/{notificationId}",
    async (event) => {
      const userId = event.params.userId;
      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(userId).get();
        if (!userDoc.exists) return;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;

        const message = {
          token: fcmToken,
          data: {
            action: "DISMISS", notificationId: event.params.notificationId,
          },
          android: {priority: "high"},
        };
        await admin.messaging().send(message);
      } catch (error) {
        logger.error("Error sending dismissal:", error);
      }
    },
);

/**
 * Dismisses the notification when an in-app notification is updated
 * (marked as read)
 */
exports.onNotificationUpdated = onDocumentUpdated(
    "users/{userId}/notifications/{notificationId}",
    async (event) => {
      const beforeData = event.data.before.data();
      const afterData = event.data.after.data();
      if (beforeData.isRead || !afterData.isRead) return;

      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(event.params.userId).get();
        if (!userDoc.exists) return;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;

        const message = {
          token: fcmToken,
          data: {
            action: "DISMISS", notificationId: event.params.notificationId,
          },
          android: {priority: "high"},
        };

        await admin.messaging().send(message);
      } catch (error) {
        logger.error("Error sending dismissal:", error);
      }
    },
);
