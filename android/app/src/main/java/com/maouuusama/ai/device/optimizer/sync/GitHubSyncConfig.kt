package com.maouuusama.ai.device.optimizer.sync

import android.content.Context

class GitHubSyncConfig(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    var repository: String
        get() = prefs.getString(KEY_REPOSITORY, DEFAULT_REPOSITORY) ?: DEFAULT_REPOSITORY
        set(value) = prefs.edit().putString(KEY_REPOSITORY, value.trim()).apply()
    var branch: String
        get() = prefs.getString(KEY_BRANCH, DEFAULT_BRANCH) ?: DEFAULT_BRANCH
        set(value) = prefs.edit().putString(KEY_BRANCH, value.trim()).apply()
    companion object {
        const val DEFAULT_REPOSITORY = "Maouuusamaaa/AI-Device-Optimizer"
        const val DEFAULT_BRANCH = "main"
        private const val PREFS_NAME = "github_evidence_sync"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REPOSITORY = "repository"
        private const val KEY_BRANCH = "branch"
    }
}
