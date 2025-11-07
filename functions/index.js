const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendAlertNotification = functions.firestore
    .document('alerts/{alertId}')
    .onCreate(async (snap, context) => {
        const alertData = snap.data();
        if (!alertData) return null;

        const creatorHash = alertData.userHash;
        const subscribersSnap = await admin.firestore()
            .collection('users')
            .doc(creatorHash)
            .collection('subscribers')
            .get();

        const tokens = [];
        subscribersSnap.forEach(doc => {
            const token = doc.data().fcmToken;
            if (token) tokens.push(token);
        });

        if (tokens.length === 0) return null;

        const message = {
            notification: {
                title: "Nueva alerta de pánico",
                body: "Un usuario que seguís creó una alerta."
            },
            tokens: tokens
        };

        return admin.messaging().sendMulticast(message);
    });
