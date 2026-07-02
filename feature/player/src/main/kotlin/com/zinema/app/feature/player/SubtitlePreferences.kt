package com.zinema.app.feature.player

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.subtitleDataStore by preferencesDataStore(name = "subtitle_prefs")
private val KEY_LANGUAGE = stringPreferencesKey("preferred_subtitle_language")

/** Persists the user's preferred subtitle language (blueprint T-051). */
class SubtitlePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** null = subtitles off. */
    val preferredLanguage: Flow<String?> =
        context.subtitleDataStore.data.map { prefs -> prefs[KEY_LANGUAGE] }

    suspend fun setPreferredLanguage(languageCode: String?) {
        context.subtitleDataStore.edit { prefs ->
            if (languageCode == null) prefs.remove(KEY_LANGUAGE) else prefs[KEY_LANGUAGE] = languageCode
        }
    }
}
