package io.github.degipe.youtubewhitelist.core.auth.credential

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getYouTubeApiKey(): String = prefs.getString(KEY_YOUTUBE_API_KEY, "") ?: ""
    fun getGoogleClientId(): String = prefs.getString(KEY_GOOGLE_CLIENT_ID, "") ?: ""
    fun getGoogleClientSecret(): String = prefs.getString(KEY_GOOGLE_CLIENT_SECRET, "") ?: ""

    fun saveCredentials(youtubeApiKey: String, googleClientId: String, googleClientSecret: String) {
        prefs.edit()
            .putString(KEY_YOUTUBE_API_KEY, youtubeApiKey)
            .putString(KEY_GOOGLE_CLIENT_ID, googleClientId)
            .putString(KEY_GOOGLE_CLIENT_SECRET, googleClientSecret)
            .apply()
    }

    fun hasCredentials(): Boolean {
        return getGoogleClientId().isNotEmpty() && getGoogleClientSecret().isNotEmpty()
    }

    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "api_credentials_encrypted"
        private const val KEY_YOUTUBE_API_KEY = "youtube_api_key"
        private const val KEY_GOOGLE_CLIENT_ID = "google_client_id"
        private const val KEY_GOOGLE_CLIENT_SECRET = "google_client_secret"
    }
}
