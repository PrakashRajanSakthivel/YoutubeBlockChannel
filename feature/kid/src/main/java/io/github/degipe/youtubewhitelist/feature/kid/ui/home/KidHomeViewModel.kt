package io.github.degipe.youtubewhitelist.feature.kid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.model.PlaylistVideo
import io.github.degipe.youtubewhitelist.core.data.model.WhitelistItem
import io.github.degipe.youtubewhitelist.core.data.repository.BlocklistRepository
import io.github.degipe.youtubewhitelist.core.data.repository.KidProfileRepository
import io.github.degipe.youtubewhitelist.core.data.repository.WhitelistRepository
import io.github.degipe.youtubewhitelist.core.data.repository.YouTubeApiRepository
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerManager
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerStatus
import io.github.degipe.youtubewhitelist.core.data.timelimit.TimeLimitChecker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KidHomeUiState(
    val profileName: String = "",
    val channels: List<WhitelistItem> = emptyList(),
    val latestVideos: List<PlaylistVideo> = emptyList(),
    val recentVideos: List<WhitelistItem> = emptyList(),
    val playlists: List<WhitelistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingVideos: Boolean = false,
    val isEmpty: Boolean = false,
    val remainingTimeFormatted: String? = null,
    val isTimeLimitReached: Boolean = false,
    val isSleepTimerExpired: Boolean = false
)

@HiltViewModel(assistedFactory = KidHomeViewModel.Factory::class)
class KidHomeViewModel @AssistedInject constructor(
    private val whitelistRepository: WhitelistRepository,
    kidProfileRepository: KidProfileRepository,
    timeLimitChecker: TimeLimitChecker,
    sleepTimerManager: SleepTimerManager,
    blocklistRepository: BlocklistRepository,
    private val youTubeApiRepository: YouTubeApiRepository,
    @Assisted private val profileId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(profileId: String): KidHomeViewModel
    }

    private val _latestVideos = MutableStateFlow<List<PlaylistVideo>>(emptyList())
    private val _isLoadingVideos = MutableStateFlow(false)
    private var videosLoaded = false

    val uiState: StateFlow<KidHomeUiState> = combine(
        kidProfileRepository.getProfileById(profileId),
        whitelistRepository.getChannelsByProfile(profileId),
        whitelistRepository.getVideosByProfile(profileId),
        combine(
            whitelistRepository.getPlaylistsByProfile(profileId),
            timeLimitChecker.getTimeLimitStatus(profileId),
            sleepTimerManager.state,
            blocklistRepository.getBlockedChannelIdsFlow(profileId),
            combine(_latestVideos, _isLoadingVideos) { vids, loading -> vids to loading }
        ) { playlists, timeLimit, sleepState, blockedIds, videosPair ->
            FiveResult(playlists, timeLimit, sleepState, blockedIds, videosPair)
        }
    ) { profile, channels, videos, extra ->
        val blockedSet = extra.blockedIds.toSet()
        val filteredChannels = channels.filter { it.youtubeId !in blockedSet }
        val (latestVids, loadingVids) = extra.videosPair

        // Trigger video fetch when channels first arrive
        if (!videosLoaded && filteredChannels.isNotEmpty()) {
            videosLoaded = true
            fetchVideosFromChannels(filteredChannels)
        }

        KidHomeUiState(
            profileName = profile?.name ?: "",
            channels = filteredChannels,
            latestVideos = latestVids,
            recentVideos = videos,
            playlists = extra.playlists,
            isLoading = false,
            isLoadingVideos = loadingVids,
            isEmpty = filteredChannels.isEmpty() && videos.isEmpty() && extra.playlists.isEmpty() && latestVids.isEmpty(),
            remainingTimeFormatted = extra.timeLimitStatus.remainingSeconds?.let { formatRemaining(it) },
            isTimeLimitReached = extra.timeLimitStatus.isLimitReached,
            isSleepTimerExpired = extra.sleepState.status == SleepTimerStatus.EXPIRED
                    && extra.sleepState.profileId == profileId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = KidHomeUiState()
    )

    private fun fetchVideosFromChannels(channels: List<WhitelistItem>) {
        viewModelScope.launch {
            _isLoadingVideos.value = true
            val allVideos = channels.map { channel ->
                async {
                    // Derive uploads playlist ID: UC... → UU...
                    val playlistId = "UU" + channel.youtubeId.removePrefix("UC")
                    when (val result = youTubeApiRepository.getPlaylistItemsPage(playlistId, null)) {
                        is AppResult.Success -> result.data.videos
                        is AppResult.Error -> emptyList()
                    }
                }
            }.awaitAll()

            // Interleave videos from different channels
            val interleaved = interleaveVideos(allVideos)

            // Filter out Shorts by duration (≤60s)
            val videoIds = interleaved.map { it.videoId }
            val durations = when (val result = youTubeApiRepository.getVideoDurations(videoIds)) {
                is AppResult.Success -> result.data
                is AppResult.Error -> emptyMap()
            }
            val filtered = interleaved.filter { video ->
                val durationSec = durations[video.videoId]
                // Keep video if: duration unknown (API failed) OR duration > 60s
                durationSec == null || durationSec > 60
            }

            _latestVideos.value = filtered
            _isLoadingVideos.value = false
        }
    }

    private data class FiveResult(
        val playlists: List<WhitelistItem>,
        val timeLimitStatus: io.github.degipe.youtubewhitelist.core.data.timelimit.TimeLimitStatus,
        val sleepState: io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerState,
        val blockedIds: List<String>,
        val videosPair: Pair<List<PlaylistVideo>, Boolean>
    )

    private fun formatRemaining(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    companion object {
        /** Interleave videos round-robin across channels for a mixed feed */
        fun interleaveVideos(channelVideos: List<List<PlaylistVideo>>): List<PlaylistVideo> {
            val result = mutableListOf<PlaylistVideo>()
            val maxSize = channelVideos.maxOfOrNull { it.size } ?: 0
            for (i in 0 until maxSize) {
                for (videos in channelVideos) {
                    if (i < videos.size) result.add(videos[i])
                }
            }
            return result
        }
    }
}
