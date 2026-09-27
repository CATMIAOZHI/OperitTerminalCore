package com.ai.assistance.operit.terminal.utils

/**
 * The GitHub project the terminal screens name and link to: the "project address" card and the
 * update check it offers.
 *
 * The default keeps the standalone upstream project so this module stays usable on its own. A
 * distribution calls [configure] once at startup to point the card and the check at its own
 * repository, which is why nothing here is written into the layouts or the strings.
 */
object TerminalProjectInfo {
    private const val DEFAULT_OWNER = "AAswordman"
    private const val DEFAULT_REPOSITORY = "OperitTerminal"

    @Volatile private var owner = DEFAULT_OWNER

    @Volatile private var repository = DEFAULT_REPOSITORY

    /** Which project to point at; blank values are ignored so a mistake cannot blank the card. */
    fun configure(owner: String, repository: String) {
        val newOwner = owner.trim()
        val newRepository = repository.trim()
        if (newOwner.isEmpty() || newRepository.isEmpty()) return
        this.owner = newOwner
        this.repository = newRepository
    }

    /** What the project address card shows, e.g. `CATMIAOZHI/OperitTerminal`. */
    val displayName: String
        get() = "$owner/$repository"

    val repositoryUrl: String
        get() = "https://github.com/$owner/$repository"

    val releasesUrl: String
        get() = "$repositoryUrl/releases"

    val tagsApiUrl: String
        get() = "https://api.github.com/repos/$owner/$repository/tags"
}
