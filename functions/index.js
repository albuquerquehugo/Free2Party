const {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");
const {setGlobalOptions} = require("firebase-functions");
const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");

admin.initializeApp();

setGlobalOptions({region: "us-east1", maxInstances: 10});

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

        const dataPayload = {
          notificationId: event.params.requestId,
          type: "FRIEND_REQUEST_RECEIVED",
          titleLocKey: "notification_friend_request_received_title",
          bodyLocKey: "notification_friend_request_received_body",
          bodyLocArgs: JSON.stringify([senderName, senderEmail]),
          // Explicitly empty fallbacks to force app-side localization
          title: "",
          message: "",
        };

        const message = {
          token: fcmToken,
          data: dataPayload,
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

      if (notifData.isSilent) {
        return;
      }

      try {
        const userDoc = await admin.firestore().collection("users")
            .doc(userId).get();
        if (!userDoc.exists) return;

        const fcmToken = userDoc.data().fcmToken;
        if (!fcmToken) return;

        const notificationId = event.params.notificationId;

        let titleLocKey = null;
        let bodyLocKey = null;
        let bodyLocArgs = null;

        // Extract name and email from message if it matches "Name (email) ..."
        const match = notifData.message.match(/([^()]+) \(([^()]+)\)/);
        let extractedArgs = null;

        if (match) {
          let cleanName = match[1].trim();
          // Strip prefixes so we send ONLY the name in bodyLocArgs
          const prefixes = [
            "Your friend request to ",
            "Seu pedido de amizade para ",
          ];
          for (const prefix of prefixes) {
            if (cleanName.includes(prefix)) {
              cleanName = cleanName.split(prefix).pop();
            }
          }
          extractedArgs = [cleanName.trim(), match[2].trim()];
        }

        switch (notifData.type) {
          case "FRIEND_ADDED":
            titleLocKey = "notification_friend_request_accepted_title";
            bodyLocKey = "notification_friend_request_accepted_body";
            bodyLocArgs = extractedArgs;
            break;
          case "FRIEND_DECLINED":
            titleLocKey = "notification_friend_request_declined_title";
            bodyLocKey =
              "notification_friend_request_declined_body";
            bodyLocArgs = extractedArgs;
            break;
          case "FRIEND_REMOVED":
            titleLocKey = "notification_friend_removed_title";
            bodyLocKey = "notification_friend_removed_body";
            bodyLocArgs = extractedArgs;
            break;
        }

        const dataPayload = {
          notificationId: notificationId,
          type: notifData.type || "GENERAL",
          // Explicitly empty fallbacks to force app-side localization
          title: "",
          message: "",
        };

        if (titleLocKey) dataPayload.titleLocKey = titleLocKey;
        if (bodyLocKey) dataPayload.bodyLocKey = bodyLocKey;
        if (bodyLocArgs) dataPayload.bodyLocArgs = JSON.stringify(bodyLocArgs);

        const message = {
          token: fcmToken,
          data: dataPayload,
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

/**
 * Triggers when a report is created or updated.
 * Creates an email document in the 'mail' collection to be sent by the
 * 'Trigger Email' extension.
 */
exports.onReportWritten = onDocumentWritten(
    "reports/{reportId}",
    async (event) => {
      const snapshot = event.data.after;
      if (!snapshot || !snapshot.exists) return;

      const reportData = snapshot.data();
      const reporterId = reportData.reporterId;
      const reportedUserId = reportData.reportedUserId;
      const reason = reportData.reason;

      try {
        // Fetch reporter and reported user details for a better email content
        const [reporterDoc, reportedDoc] = await Promise.all([
          admin.firestore().collection("users").doc(reporterId).get(),
          admin.firestore().collection("users").doc(reportedUserId).get(),
        ]);

        const getFullName = (doc) => {
          if (!doc.exists) return "Unknown User";
          const data = doc.data();
          return `${data.firstName || ""} ${data.lastName || ""}`.trim() ||
            "Unknown User";
        };

        const reporterName = getFullName(reporterDoc);
        const reporterEmail = reporterDoc.exists ?
            reporterDoc.data().email : "N/A";
        const reportedName = getFullName(reportedDoc);
        const reportedEmail = reportedDoc.exists ?
            reportedDoc.data().email : "N/A";

        // Map Portuguese reasons to English for the support email
        const reasonMap = {
          "Spam ou golpe": "Spam or scam",
          "Assédio ou bullying": "Harassment or bullying",
          "Conteúdo ou foto de perfil inapropriada":
            "Inappropriate content or profile picture",
          "Falsificação de identidade": "Impersonation",
          "Outro": "Other",
        };
        const englishReason = reasonMap[reason] || reason;

        const isNewReport = !event.data.before.exists;

        // Create the email document for the Trigger Email extension
        await admin.firestore().collection("mail").add({
          to: "support@free2party.app",
          message: {
            subject: `[${isNewReport ? "NEW" : "UPDATE"}] ` +
              `User Report - ${reportedName}`,
            html: `
              <h1>User Report ${isNewReport ? "Received" : "Updated"}</h1>
              <p>
                A user report has been ${isNewReport ? "created" : "updated"}.
              </p>
              <hr>
              <h3>Report Details:</h3>
              <ul>
                <li><strong>Reason:</strong> ${englishReason}</li>
              </ul>
              <h3>Reported User:</h3>
              <ul>
                <li><strong>Name:</strong> ${reportedName}</li>
                <li><strong>Email:</strong> ${reportedEmail}</li>
                <li><strong>UID:</strong> ${reportedUserId}</li>
              </ul>
              <h3>Reporter:</h3>
              <ul>
                <li><strong>Name:</strong> ${reporterName}</li>
                <li><strong>Email:</strong> ${reporterEmail}</li>
                <li><strong>UID:</strong> ${reporterId}</li>
              </ul>
            `,
          },
        });

        logger.info(
            `Report email task created for report ${event.params.reportId}`,
        );
      } catch (error) {
        logger.error("Error processing report email:", error);
      }
    },
);
