package io.github.degipe.youtubewhitelist.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object SignIn : Route
    @Serializable data object PinSetup : Route
    @Serializable data object PinEntry : Route
    @Serializable data object PinChange : Route
    @Serializable data object ProfileCreation : Route
    @Serializable data class KidHome(val profileId: String) : Route
    @Serializable data object ParentDashboard : Route
    @Serializable data class WhitelistManager(val profileId: String) : Route
    @Serializable data class WebViewBrowser(val profileId: String) : Route
    @Serializable data class ChannelDetail(
        val profileId: String,
        val channelId: String,
        val channelTitle: String,
        val channelThumbnailUrl: String
    ) : Route
    @Serializable data class VideoPlayer(
        val profileId: String,
        val videoId: String,
        val videoTitle: String = "",
        val channelTitle: String? = null
    ) : Route
    @Serializable data class KidSearch(val profileId: String) : Route
    @Serializable data class SleepMode(val profileId: String) : Route
    @Serializable data object ProfileSelector : Route
    @Serializable data class ProfileEdit(val profileId: String) : Route
    @Serializable data class WatchStats(val profileId: String) : Route
    @Serializable data class ExportImport(val parentAccountId: String) : Route
    @Serializable data class PlaylistDetail(
        val profileId: String,
        val playlistId: String,
        val playlistTitle: String,
        val playlistThumbnailUrl: String
    ) : Route
    @Serializable data object About : Route
    @Serializable data object CredentialSettings : Route
    @Serializable data object BlockedChannels : Route
    /** Shown after PinSetup on a fresh install: user chooses "Create profile" or "Import from phone" */
    @Serializable data object DeviceSetupChoice : Route
    /** TV: shows a QR code and waits for the phone to send profile data */
    @Serializable data object QrReceive : Route
    /** Phone: opens camera to scan TV's QR code, then sends profile data */
    @Serializable data object QrSend : Route
}
