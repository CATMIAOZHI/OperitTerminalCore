package com.ai.assistance.operit.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PtyProcessPolicyTest {
    @Test
    fun `live PTY process has no exit value`() {
        try {
            ptyProcessExitValue(PtyProcessProbeResult.ALIVE)
            fail("Expected a live process to reject exitValue")
        } catch (_: IllegalThreadStateException) {
            // Process.isAlive relies on this exact contract.
        }
    }

    @Test
    fun `dead PTY process exposes a completed exit value`() {
        assertEquals(0, ptyProcessExitValue(PtyProcessProbeResult.DEAD))
    }
}
