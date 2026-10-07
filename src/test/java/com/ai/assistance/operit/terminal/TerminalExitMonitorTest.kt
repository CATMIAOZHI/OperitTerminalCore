package com.ai.assistance.operit.terminal

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TerminalExitMonitorTest {
    @Test fun `exit waits for final output to drain`() = runBlocking {
        var output = ""
        val reader = launch { delay(20); output = "final output" }
        assertTrue(awaitTerminalExit({ false }, reader, drainMs = 1000))
        assertEquals("final output", output)
    }

    @Test fun `descendant holding output cannot block completion forever`() = runBlocking {
        val reader = launch { awaitCancellation() }
        try {
            assertFalse(withTimeout(1000) { awaitTerminalExit({ false }, reader, drainMs = 20) })
        } finally {
            reader.cancelAndJoin()
        }
    }

    @Test fun `active process monitor is cancellable`() = runBlocking {
        val reader = launch { awaitCancellation() }
        val monitor = launch { awaitTerminalExit({ true }, reader, pollMs = 10) }
        withTimeout(1000) { monitor.cancelAndJoin(); reader.cancelAndJoin() }
        assertTrue(monitor.isCancelled)
    }
}
