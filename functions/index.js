const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const CHANNEL_ID = "task_updates_high";
const DEAD_TOKEN_CODES = [
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
];
// Landing page opened from the invitation email; the app registers a matching intent filter.
const INVITE_LINK_BASE = "https://tasktracker-e84c1.web.app/invite";

const escapeHtml = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

/** The `mail` document for the "Trigger Email from Firestore" extension. All user-supplied text is escaped. */
function invitationEmail(invite) {
  const link = `${INVITE_LINK_BASE}?email=${encodeURIComponent(invite.receiverEmail)}`;
  const role = String(invite.role || "MEMBER").toLowerCase();
  const subject = `${invite.senderEmail} invited you to ${invite.workspaceName} on Task Tracker`;
  const text =
    `${invite.senderEmail} invited you to join "${invite.workspaceName}" on Task Tracker as ${role}.

` +
    `To accept:
` +
    `1. Open ${link} and install or open the Task Tracker app.
` +
    `2. Create an account using this email address (${invite.receiverEmail}).
` +
    `3. Verify your email from the message we send you.
` +
    `4. Open the Invites tab and tap Accept.
`;
  const html =
    `<p><b>${escapeHtml(invite.senderEmail)}</b> invited you to join <b>${escapeHtml(invite.workspaceName)}</b> ` +
    `on Task Tracker as <b>${escapeHtml(role)}</b>.</p>` +
    `<p>To accept:</p><ol>` +
    `<li><a href="${escapeHtml(link)}">Open the invitation link</a> and install or open the Task Tracker app.</li>` +
    `<li>Create an account using this email address (<b>${escapeHtml(invite.receiverEmail)}</b>).</li>` +
    `<li>Verify your email from the message we send you.</li>` +
    `<li>Open the <b>Invites</b> tab and tap <b>Accept</b>.</li></ol>`;
  return { to: invite.receiverEmail, message: { subject, text, html }, createdAt: FieldValue.serverTimestamp() };
}

/**
 * Records a feed entry for each user and pushes it to their device. Every notification document
 * has a matching FCM push, so the feed and the push notifications never drift apart.
 */
async function notifyUsers(userIds, { title, body, type, workspaceId, tag }) {
  if (userIds.length === 0) return;
  const db = getFirestore();

  await Promise.all(
    userIds.map((userId) =>
      db.collection("notifications").add({
        userId,
        title,
        body,
        workspaceId,
        type,
        isRead: false,
        createdAt: Date.now(),
      })
    )
  );

  const profiles = await db.getAll(...userIds.map((uid) => db.doc(`users/${uid}`)));
  const withToken = profiles.filter((p) => p.exists && p.get("fcmToken"));
  if (withToken.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens: withToken.map((p) => p.get("fcmToken")),
    notification: { title, body },
    data: { title, body, type, workspaceId },
    android: {
      priority: "high",
      ttl: 60 * 1000,
      notification: { channelId: CHANNEL_ID, ...(tag ? { tag } : {}) },
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
}

async function displayName(uid) {
  if (!uid) return "Someone";
  const profile = await getFirestore().doc(`users/${uid}`).get();
  return profile.get("displayName") || profile.get("email") || "Someone";
}

/** Maps a task write to a feed entry: created, completed, updated or deleted. */
function describeChange(before, after) {
  if (!before && after) return { title: "Task created", verb: "created", task: after };
  if (before && !after) return { title: "Task deleted", verb: "deleted", task: before };
  if (!before.isCompleted && after.isCompleted) return { title: "Task completed", verb: "completed", task: after };
  if (before.title !== after.title || before.description !== after.description) {
    return { title: "Task updated", verb: "updated", task: after };
  }
  return null;
}

/**
 * Notifies every other workspace member when a task is created, updated, completed or deleted.
 * The actor is `updatedBy` (enforced by the Firestore rules; the client stamps it before a delete),
 * and is excluded from the recipients.
 */
exports.notifyWorkspaceOnTaskChange = onDocumentWritten("workspaces/{workspaceId}/tasks/{taskId}", async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  const change = describeChange(before, after);
  if (!change) return;

  const { workspaceId, taskId } = event.params;
  const db = getFirestore();
  const workspace = await db.doc(`workspaces/${workspaceId}`).get();
  if (!workspace.exists) return; // the workspace itself is gone

  const actorId = change.task.updatedBy;
  const recipients = (workspace.get("members") || []).filter((uid) => uid !== actorId);
  if (recipients.length === 0) return;

  const actor = await displayName(actorId);
  await notifyUsers(recipients, {
    title: change.title,
    body: `${actor} ${change.verb} "${change.task.title}" in ${workspace.get("name")}`,
    type: "TASK_UPDATE",
    workspaceId,
    tag: taskId,
  });
});

/**
 * New (or re-sent) invitation: notify the receiver in-app and by push if they have an account,
 * otherwise email them a link to register. Answered invitation: tell the sender.
 */
exports.onInvitationWritten = onDocumentWritten("invitations/{inviteId}", async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  if (!after) return;

  const db = getFirestore();
  // A re-sent invitation keeps its PENDING status but gets a new createdAt, so it counts as new too.
  const becamePending =
    after.status === "PENDING" && (!before || before.status !== "PENDING" || before.createdAt !== after.createdAt);

  if (becamePending) {
    const match = await db.collection("users").where("email", "==", after.receiverEmail).limit(1).get();
    if (!match.empty) {
      await notifyUsers([match.docs[0].id], {
        title: "Workspace invitation",
        body: `${after.senderEmail} invited you to join ${after.workspaceName}`,
        type: "INVITE",
        workspaceId: after.workspaceId,
        tag: event.params.inviteId,
      });
    } else {
      // Picked up by the "Trigger Email from Firestore" extension (collection `mail`).
      await db.collection("mail").add(invitationEmail(after));
    }
    return;
  }

  if (before && before.status === "PENDING" && (after.status === "ACCEPTED" || after.status === "DECLINED")) {
    const sender = await db.collection("users").where("email", "==", after.senderEmail).limit(1).get();
    if (sender.empty) return;
    await notifyUsers([sender.docs[0].id], {
      title: after.status === "ACCEPTED" ? "Invitation accepted" : "Invitation declined",
      body: `${after.receiverEmail} ${after.status === "ACCEPTED" ? "joined" : "declined"} ${after.workspaceName}`,
      type: "INVITE",
      workspaceId: after.workspaceId,
      tag: event.params.inviteId,
    });
  }
});
