package com.ai.assistance.operit.terminal

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommandExecutionEvent(
    val commandId: String,
    val sessionId: String,
    val outputChunk: String, // 命令执行过程量
    val isCompleted: Boolean, // 是否执行完毕
    val terminationReason: String? = null,
    val exitCode: Int? = null,
    val sessionExitCode: Int? = null,
    val screen: String? = null,
    /** [screen] was taken while a full-screen program held the display, so it is not in the output. */
    val screenFullscreen: Boolean = false
) : Parcelable

@Parcelize
data class SessionDirectoryEvent(
    val sessionId: String,
    val currentDirectory: String
) : Parcelable
