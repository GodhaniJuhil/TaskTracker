package com.tasktracker.app.data.firebase

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching] for suspend code, but never swallows coroutine cancellation. */
internal suspend inline fun <T> firebaseCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
