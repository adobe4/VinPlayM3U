package com.vinplay.m3u.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vinplay_prefs")

class PreferencesDataStore(private val context: Context) {

    companion object {
        private val KEY_DOWNLOAD_DIR = stringPreferencesKey("download_dir")
        private val KEY_LAST_IMPORT_URL = stringPreferencesKey("last_import_url")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val downloadDir: Flow<String?> = context.dataStore.data.map { it[KEY_DOWNLOAD_DIR] }
    val lastImportUrl: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_IMPORT_URL] }
    val themeMode: Flow<String?> = context.dataStore.data.map { it[KEY_THEME_MODE] }

    suspend fun setDownloadDir(dir: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_DIR] = dir }
    }

    suspend fun setLastImportUrl(url: String) {
        context.dataStore.edit { it[KEY_LAST_IMPORT_URL] = url }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }
}
