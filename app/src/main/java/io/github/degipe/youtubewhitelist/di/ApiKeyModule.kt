package io.github.degipe.youtubewhitelist.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.degipe.youtubewhitelist.BuildConfig
import io.github.degipe.youtubewhitelist.core.auth.credential.CredentialStore
import io.github.degipe.youtubewhitelist.core.auth.di.GoogleClientId
import io.github.degipe.youtubewhitelist.core.auth.di.GoogleClientSecret
import io.github.degipe.youtubewhitelist.core.network.di.YouTubeApiKey

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {

    @Provides
    @YouTubeApiKey
    fun provideYouTubeApiKey(credentialStore: CredentialStore): String {
        val stored = credentialStore.getYouTubeApiKey()
        return stored.ifEmpty { BuildConfig.YOUTUBE_API_KEY }
    }

    @Provides
    @GoogleClientId
    fun provideGoogleClientId(credentialStore: CredentialStore): String {
        val stored = credentialStore.getGoogleClientId()
        return stored.ifEmpty { BuildConfig.GOOGLE_CLIENT_ID }
    }

    @Provides
    @GoogleClientSecret
    fun provideGoogleClientSecret(credentialStore: CredentialStore): String {
        val stored = credentialStore.getGoogleClientSecret()
        return stored.ifEmpty { BuildConfig.GOOGLE_CLIENT_SECRET }
    }
}
