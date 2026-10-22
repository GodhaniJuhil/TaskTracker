package com.tasktracker.app.data.remote

/**
 * Reference for the FCM HTTP v1 payload the server (or Firebase Console "custom data") must send so
 * notifications arrive immediately and wake the device while the app is backgrounded or killed.
 *
 * - `notification` lets the system render the message if the app process is dead.
 * - `data` carries title/body so [onMessageReceived] can render the same message with the app's
 *   high-priority channel and screen wake-up.
 * - `android.priority = "high"` is what bypasses Doze batching; `data.priority` mirrors it for the app.
 * - `channel_id` must match the client channel `task_updates_high`.
 */
object FcmPayloadExample {

    fun json(token: String, title: String, body: String): String = """
        {
          "message": {
            "token": "$token",
            "notification": {
              "title": "$title",
              "body": "$body"
            },
            "data": {
              "title": "$title",
              "body": "$body",
              "priority": "high"
            },
            "android": {
              "priority": "high",
              "ttl": "60s",
              "notification": {
                "channel_id": "task_updates_high",
                "notification_priority": "PRIORITY_MAX",
                "default_sound": true,
                "default_vibrate_timings": true,
                "visibility": "PUBLIC"
              }
            }
          }
        }
    """.trimIndent()
}
