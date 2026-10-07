package com.ai.assistance.operit.terminal

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Observe process death even when a descendant still holds the output descriptor open. */
internal suspend fun awaitTerminalExit(
    isAlive: () -> Boolean,
    reader: Job,
    pollMs: Long = 250L,
    drainMs: Long = 1_000L,
): Boolean {
    // Do not block waitpid: cancelling a session must also stop its observer.
    while (isAlive()) delay(pollMs)
    return withTimeoutOrNull(drainMs) { reader.join(); true } ?: false
}
