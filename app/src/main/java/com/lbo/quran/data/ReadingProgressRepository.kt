package com.lbo.quran.data

import android.content.Context

class ReadingProgressRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("quran_reading_progress", Context.MODE_PRIVATE)

    fun getLastReadAyah(): String? {
        val id = prefs.getString(KEY_LAST_READ, null)
        return if (id.isNullOrBlank()) null else id
    }

    fun saveLastReadAyah(aId: String) {
        prefs.edit().putString(KEY_LAST_READ, aId).apply()
    }

    fun getBookmarks(): List<String> {
        val raw = prefs.getString(KEY_BOOKMARKS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun isBookmarked(aId: String): Boolean = aId in getBookmarks()

    fun addBookmark(aId: String) {
        val current = getBookmarks()
        if (aId in current) return
        val updated = current + aId
        prefs.edit().putString(KEY_BOOKMARKS, updated.joinToString(",")).apply()
    }

    fun removeBookmark(aId: String) {
        val updated = getBookmarks().filter { it != aId }
        prefs.edit().putString(KEY_BOOKMARKS, updated.joinToString(",")).apply()
    }

    fun toggleBookmark(aId: String): Boolean {
        return if (isBookmarked(aId)) {
            removeBookmark(aId)
            false
        } else {
            addBookmark(aId)
            true
        }
    }

    fun getLastReadTafsir(): Pair<String, Long>? {
        val raw = prefs.getString(KEY_TAFSIR_LAST_READ, null) ?: return null
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val id = parts[1].toLongOrNull() ?: return null
        return parts[0] to id
    }

    fun saveLastReadTafsir(language: String, tafsirId: Long) {
        prefs.edit().putString(KEY_TAFSIR_LAST_READ, "$language:$tafsirId").apply()
    }

    fun getTafsirBookmarks(): List<String> {
        val raw = prefs.getString(KEY_TAFSIR_BOOKMARKS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun toggleTafsirBookmark(language: String, tafsirId: Long): Boolean {
        val key = "$language:$tafsirId"
        val current = getTafsirBookmarks()
        return if (key in current) {
            prefs.edit().putString(KEY_TAFSIR_BOOKMARKS, (current - key).joinToString(",")).apply()
            false
        } else {
            prefs.edit().putString(KEY_TAFSIR_BOOKMARKS, (current + key).joinToString(",")).apply()
            true
        }
    }

    fun getSearchHistory(): List<String> {
        val raw = prefs.getString(KEY_SEARCH_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = getSearchHistory().filter { it != trimmed }
        val updated = (listOf(trimmed) + current).take(10)
        prefs.edit().putString(KEY_SEARCH_HISTORY, updated.joinToString("|")).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    companion object {
        private const val KEY_LAST_READ = "last_read_a_id"
        private const val KEY_BOOKMARKS = "bookmarked_a_ids"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_TAFSIR_LAST_READ = "last_read_tafsir_id"
        private const val KEY_TAFSIR_BOOKMARKS = "bookmarked_tafsir_ids"
    }
}
