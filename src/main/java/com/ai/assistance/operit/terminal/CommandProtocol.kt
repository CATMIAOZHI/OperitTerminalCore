package com.ai.assistance.operit.terminal

import java.util.UUID

/** Local automation shells keep state; eval runs in that shell, not in a child. */
class CommandProtocol {
    val token = UUID.randomUUID().toString().replace("-", "")
    private val helper = "__operit_result_$token"
    private val startHelper = "__operit_start_$token"
    var installing = false
    var ready = false
    var activeToken: String? = null
    var envelope: String? = null
    /**
     * The shell printed the start marker of [activeToken]. Everything before it is the terminal
     * echoing the submitted line, which for multi-line commands spans many lines and PS2 prompts.
     */
    var started = false

    fun bootstrap(): String =
        "$helper() { local rc=\$1; builtin printf '\\033]633;Operit;%s;%s\\007' \"\$2\" \"\$rc\"; return \"\$rc\"; }; " +
            // Prints the start marker and hands the previous command's status on, so `$?` still works.
            "$startHelper() { local rc=\$?; builtin printf '\\033]633;OperitStart;%s\\007' \"\$1\"; return \"\$rc\"; }; " +
            "$helper 0 '$token'"

    fun command(text: String): String {
        activeToken = UUID.randomUUID().toString().replace("-", "")
        started = false
        return ("$startHelper '$activeToken'; " +
            "builtin eval -- '${text.replace("'", "'\\''")}'; $helper \"\$?\" '$activeToken'").also {
            envelope = it
        }
    }
}

/** OSC frame the shell prints right before running an automation command. */
val COMMAND_START_FRAME = Regex("\u001b]633;OperitStart;([a-f0-9]+)(?:\u0007|\u001b\\\\)")

/**
 * Frame complete escape sequences before rendering or parsing. PTY reads can split any sequence.
 * A malformed/unterminated control sequence is bounded and discarded, never returned as raw ANSI.
 */
class TerminalOutputFramer {
    private var pending = ""
    fun feed(chunk: String, accept: (String) -> Unit) {
        val text = pending + chunk
        pending = ""
        var start = 0
        var index = 0
        while (index < text.length) {
            if (text[index] != '\u001b') { index++; continue }
            if (index > start) accept(text.substring(start, index))
            val end = escapeEnd(text, index)
            if (end == null) {
                pending = text.substring(index).takeIf { it.length <= 8192 }.orEmpty()
                return
            }
            accept(text.substring(index, end))
            index = end
            start = end
        }
        if (start < text.length) accept(text.substring(start))
    }

    private fun escapeEnd(text: String, start: Int): Int? {
        if (start + 1 >= text.length) return null
        return when (text[start + 1]) {
            '[' -> (start + 2 until text.length).firstOrNull { text[it] in '@'..'~' }?.plus(1)
            ']', 'P', '^', '_' -> {
                var i = start + 2
                while (i < text.length) {
                    if (text[i] == '\u0007') return i + 1
                    if (text[i] == '\u001b' && i + 1 < text.length && text[i + 1] == '\\') return i + 2
                    i++
                }
                null
            }
            else -> start + 2
        }
    }
}
