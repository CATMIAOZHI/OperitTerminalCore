package com.ai.assistance.operit.terminal

import org.junit.Assert.*
import org.junit.Test

class CommandProtocolTest {
    @Test fun `framing is independent of PTY byte boundaries`() {
        val text = "start\u001b[?1049h\u001b[2J\u001b]633;Operit;abc;7\u0007end"
        for (boundary in 0..text.length) {
            val frames = mutableListOf<String>()
            val framer = TerminalOutputFramer()
            framer.feed(text.take(boundary), frames::add)
            framer.feed(text.drop(boundary), frames::add)
            assertEquals(text, frames.joinToString(""))
            assertTrue(frames.contains("\u001b]633;Operit;abc;7\u0007"))
            assertTrue(frames.contains("\u001b[?1049h"))
        }
    }

    @Test fun `command quoting does not interpolate command contents in outer shell`() {
        val protocol = CommandProtocol()
        val envelope = protocol.command("printf '%s' \"\$HOME\"\nfalse # tail")
        assertTrue(envelope.startsWith("__operit_start_${protocol.token} '${protocol.activeToken}'; "))
        assertTrue(envelope.contains("; builtin eval -- 'printf '\\''%s'\\''"))
        assertFalse(envelope.contains('\u001b'))
        assertNotNull(protocol.activeToken)
        assertFalse(protocol.started)
        assertTrue(protocol.bootstrap().contains("return \"\$rc\""))
        assertTrue(protocol.bootstrap().contains("__operit_start_${protocol.token}() { local rc=\$?;"))
    }

    @Test fun `start marker is one frame that names the command`() {
        val marker = "\u001b]633;OperitStart;abc123\u0007"
        val frames = mutableListOf<String>()
        TerminalOutputFramer().feed("echoed line\r\n$marker" + "output\r\n", frames::add)
        assertTrue(frames.contains(marker))
        assertEquals("abc123", COMMAND_START_FRAME.matchEntire(marker)!!.groupValues[1])
    }
}
