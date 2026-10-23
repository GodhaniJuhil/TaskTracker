const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const CHANNEL_ID = "task_updates_high";
const DEAD_TOKEN_CODES = [
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
];

/** Maps a task write to a notification, or null when nothing worth notifying happened (e.g. delete). */
function describeChange(before, after) {
  if (!after) return null;
  if (!before) return { title: "New task", body: after.title };
  if (!before.isCompleted && after.isCompleted) return { title: "Task completed", body: after.title };
  if (before.title !== after.title || before.description !== after.description) {
    return { title: "Task updated", body: after.title };
  }
  return null;
}

/**
 * Pushes a notification to every other team member when a shared task is added, edited or completed.
 * The actor (`updatedBy`, enforced by the Firestore rules) is excluded.
 */
exports.notifyTeamOnTaskChange = onDocumentWritten("teams/{teamId}/tasks/{taskId}", async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  const change = describeChange(before, after);
  if (!change) return;

  const db = getFirestore();
  const team = await db.doc(`teams/${event.params.teamId}`).get();
  if (!team.exists) return;

  const recipients = (team.get("memberIds") || []).filter((uid) => uid !== after.updatedBy);
  if (recipients.length === 0) return;

  const profiles = await db.getAll(...recipients.map((uid) => db.doc(`users/${uid}`)));
  const withToken = profiles.filter((p) => p.exists && p.get("fcmToken"));
  if (withToken.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens: withToken.map((p) => p.get("fcmToken")),
    notification: { title: change.title, body: change.body },
    data: { title: change.title, body: change.body, taskId: event.params.taskId },
    android: {
      priority: "high",
      ttl: 60 * 1000,
      notification: { channelId: CHANNEL_ID, tag: event.params.taskId },
    },
  });

  // Responses are index-aligned with the tokens, so a dead token maps straight back to its profile.
  const cleanups = [];
  response.responses.forEach((r, i) => {
    if (r.error && DEAD_TOKEN_CODES.includes(r.error.code)) {
      cleanups.push(withToken[i].ref.update({ fcmToken: null }));
    }
  });
  await Promise.all(cleanups);
});
