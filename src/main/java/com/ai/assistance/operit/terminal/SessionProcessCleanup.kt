package com.ai.assistance.operit.terminal

import android.system.Os
import android.system.OsConstants
import android.system.ErrnoException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

internal data class SessionProcessIdentity(
    val pid: Int,
    val sessionId: Int,
    val startTime: Long,
    val state: String,
)

/** comm may contain spaces and parentheses; fields after its final ')' have fixed offsets. */
internal fun parseSessionProcessStat(text: String): SessionProcessIdentity? = runCatching {
    val end = text.lastIndexOf(')')
    require(end > 0)
    val fields = text.substring(end + 1).trim().split(Regex("\\s+"))
    SessionProcessIdentity(
        text.substringBefore(' ').toInt(),
        fields[3].toInt(),
        fields[19].toLong(),
        fields[0],
    )
}.getOrNull()

internal fun readSessionProcess(pid: Int): SessionProcessIdentity? =
    runCatching { parseSessionProcessStat(File("/proc/$pid/stat").readText()) }.getOrNull()

/**
 * Only for retiring a PTY, never for continuing to use its shell.
 * forkpty creates a new session. Signal PRoot first so its tracer can kill/reap tracees;
 * then kill remaining members of that same session, checking identities before each signal.
 */
internal suspend fun terminatePtySession(owner: SessionProcessIdentity, timeoutMs: Long): Boolean {
    if (owner.pid <= 1 || owner.sessionId != owner.pid) return false
    val knownMembers = mutableMapOf(owner.pid to owner)
    fun missing(pid: Int): Boolean = try {
        Os.kill(pid, 0)
        false
    } catch (error: ErrnoException) {
        error.errno == OsConstants.ESRCH
    }
    fun exited(member: SessionProcessIdentity): Boolean {
        val current = readSessionProcess(member.pid) ?: return missing(member.pid)
        return current.startTime != member.startTime || current.state in setOf("Z", "X")
    }
    fun members(): List<SessionProcessIdentity>? {
        val currentOwner = readSessionProcess(owner.pid)
        if (currentOwner == null && !missing(owner.pid)) return null
        if (currentOwner != null &&
            (currentOwner.startTime != owner.startTime || currentOwner.sessionId != owner.sessionId)) return null
        return File("/proc").listFiles()?.mapNotNull { entry ->
            entry.name.toIntOrNull()?.let(::readSessionProcess)
        }?.filter { it.sessionId == owner.sessionId && it.state != "Z" && it.state != "X" }
            ?.also { list -> list.forEach { knownMembers[it.pid] = it } }
    }
    fun allExited(): Boolean = members()?.isEmpty() == true && knownMembers.values.all(::exited)
    fun signal(member: SessionProcessIdentity, value: Int) {
        val current = readSessionProcess(member.pid) ?: return
        if (current.startTime == member.startTime && current.sessionId == owner.sessionId) {
            runCatching { Os.kill(member.pid, value) }
        }
    }
    val initial = members() ?: return false
    initial.filter { member ->
        val executable = runCatching {
            File("/proc/${member.pid}/cmdline").readText().substringBefore('\u0000').substringAfterLast('/')
        }.getOrNull()
        executable in setOf("proot", "proot.bin", "libproot.so")
    }.forEach { signal(it, OsConstants.SIGQUIT) }

    val graceful = withTimeoutOrNull((timeoutMs / 2).coerceAtLeast(1)) {
        while (!allExited()) delay(25)
        true
    }
    if (graceful == true) return true
    return withTimeoutOrNull((timeoutMs / 2).coerceAtLeast(1)) {
        while (true) {
            val remaining = members() ?: return@withTimeoutOrNull false
            if (remaining.isEmpty() && knownMembers.values.all(::exited)) return@withTimeoutOrNull true
            // Kill descendants before the leader, keeping PRoot alive as long as possible.
            remaining.sortedBy { it.pid == owner.pid }.forEach { signal(it, OsConstants.SIGKILL) }
            delay(25)
        }
        @Suppress("UNREACHABLE_CODE")
        false
    } ?: false
}
