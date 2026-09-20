package com.ai.assistance.operit.terminal

import org.junit.Assert.*
import org.junit.Test

class SessionProcessCleanupTest {
    @Test
    fun `stat with parentheses and spaces in comm keeps the correct session and start time`() {
        // pid, comm, state, ppid, pgrp, session, tty ... starttime
        val stat = "22175 (less (pager)) S 22174 22174 20512 " +
            List(15) { "0" }.joinToString(" ") + " 87654321 0"
        assertEquals(SessionProcessIdentity(22175, 20512, 87654321, "S"), parseSessionProcessStat(stat))
    }

    @Test
    fun `incomplete or malformed proc entries are not process identities`() {
        assertNull(parseSessionProcessStat(""))
        assertNull(parseSessionProcessStat("42 (gone) Z 1"))
        assertNull(parseSessionProcessStat("not a proc stat"))
    }
}
