package io.github.degipe.youtubewhitelist.feature.kid.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.model.PlaylistVideo
import io.github.degipe.youtubewhitelist.core.data.model.WhitelistItem
import io.github.degipe.youtubewhitelist.core.data.repository.WatchHistoryRepository
import io.github.degipe.youtubewhitelist.core.data.repository.WhitelistRepository
import io.github.degipe.youtubewhitelist.core.data.repository.YouTubeApiRepository
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerManager
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerStatus
import io.github.degipe.youtubewhitelist.core.data.timelimit.TimeLimitChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VideoPlayerUiState(
    val videoId: String = "",
    val videoTitle: String = "",
    val youtubeId: String = "",
    val siblingVideos: List<WhitelistItem> = emptyList(),
    val suggestedVideos: List<PlaylistVideo> = emptyList(),
    val currentIndex: Int = -1,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingSuggestions: Boolean = false,
    val error: String? = null,
    val remainingTimeFormatted: String? = null,
    val isTimeLimitReached: Boolean = false,
    val isSleepTimerExpired: Boolean = false
)

@HiltViewModel(assistedFactory = VideoPlayerViewModel.Factory::class)
class VideoPlayerViewModel @AssistedInject constructor(
    private val whitelistRepository: WhitelistRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val timeLimitChecker: TimeLimitChecker,
    private val sleepTimerManager: SleepTimerManager,
    private val youTubeApiRepository: YouTubeApiRepository,
    @Assisted("profileId") private val profileId: String,
    @Assisted("videoId") private val videoId: String,
    @Assisted("videoTitle") private val initialVideoTitle: String,
    @Assisted("channelTitle") private val channelTitle: String?
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("profileId") profileId: String,
            @Assisted("videoId") videoId: String,
            @Assisted("videoTitle") videoTitle: String,
            @Assisted("channelTitle") channelTitle: String?
        ): VideoPlayerViewModel
    }

    private val _uiState = MutableStateFlow(VideoPlayerUiState(videoId = videoId))
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var siblingsJob: Job? = null
    private var videoPool: List<PlaylistVideo> = emptyList()
    private val watchedIds = mutableSetOf<String>()

    init {
        loadVideo()
        loadSiblings()
        loadSuggestedVideos()
        observeTimeLimit()
        observeSleepTimer()
    }

    private fun loadVideo() {
        _uiState.value = _uiState.value.copy(
            videoTitle = initialVideoTitle,
            youtubeId = videoId,
            isLoading = false
        )
    }

    private fun loadSiblings() {
        if (channelTitle == null) return

        siblingsJob?.cancel()
        siblingsJob = viewModelScope.launch {
            whitelistRepository.getVideosByChannelTitle(profileId, channelTitle)
                .collect { siblings ->
                    val currentIdx = siblings.indexOfFirst { it.youtubeId == _uiState.value.youtubeId }
                    _uiState.value = _uiState.value.copy(
                        siblingVideos = siblings,
                        currentIndex = currentIdx,
                        hasNext = currentIdx >= 0 && currentIdx < siblings.size - 1,
                        hasPrevious = currentIdx > 0
                    )
                }
        }
    }

    private fun loadSuggestedVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSuggestions = true)
            val channels = whitelistRepository.getChannelsByProfile(profileId).first()
            if (channels.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoadingSuggestions = false)
                return@launch
            }

            val allVideos = channels.map { channel ->
                async {
                    val playlistId = "UU" + channel.youtubeId.removePrefix("UC")
                    when (val result = youTubeApiRepository.getPlaylistItemsPage(playlistId, null)) {
                        is AppResult.Success -> result.data.videos
                        is AppResult.Error -> emptyList()
                    }
                }
            }.awaitAll()

            // Flatten, shuffle, exclude current video
            val candidates = allVideos.flatten()
                .filter { it.videoId != _uiState.value.youtubeId }
                .shuffled()

            // Filter out Shorts (≤60s) by duration
            val videoIds = candidates.map { it.videoId }
            val durations = when (val result = youTubeApiRepository.getVideoDurations(videoIds)) {
                is AppResult.Success -> result.data
                is AppResult.Error -> emptyMap()
            }
            videoPool = candidates.filter { video ->
                val durationSec = durations[video.videoId]
                durationSec == null || durationSec > 60
            }
            watchedIds.add(_uiState.value.youtubeId)

            _uiState.value = _uiState.value.copy(
                suggestedVideos = pickSuggestions(),
                isLoadingSuggestions = false
            )
        }
    }

    private fun observeTimeLimit() {
        viewModelScope.launch {
            timeLimitChecker.getTimeLimitStatus(profileId).collect { status ->
                _uiState.value = _uiState.value.copy(
                    remainingTimeFormatted = status.remainingSeconds?.let { formatRemaining(it) },
                    isTimeLimitReached = status.isLimitReached
                )
            }
        }
    }

    private fun observeSleepTimer() {
        viewModelScope.launch {
            sleepTimerManager.state.collect { sleepState ->
                _uiState.value = _uiState.value.copy(
                    isSleepTimerExpired = sleepState.status == SleepTimerStatus.EXPIRED
                            && sleepState.profileId == profileId
                )
            }
        }
    }

    fun onVideoEnded(watchedSeconds: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            watchHistoryRepository.recordWatch(
                profileId = profileId,
                videoId = state.youtubeId,
                videoTitle = state.videoTitle,
                watchedSeconds = watchedSeconds
            )

            if (state.hasNext) {
                navigateToIndex(state.currentIndex + 1)
            }
        }
    }

    fun playNext() {
        val state = _uiState.value
        if (state.hasNext) {
            navigateToIndex(state.currentIndex + 1)
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.hasPrevious) {
            navigateToIndex(state.currentIndex - 1)
        }
    }

    fun playVideoAt(index: Int) {
        navigateToIndex(index)
    }

    fun playSuggestedVideo(video: PlaylistVideo) {
        watchedIds.add(video.videoId)
        _uiState.value = _uiState.value.copy(
            videoId = video.videoId,
            videoTitle = video.title,
            youtubeId = video.videoId,
            siblingVideos = emptyList(),
            suggestedVideos = pickSuggestions(excludeId = video.videoId)
        )
    }

    private fun pickSuggestions(excludeId: String? = null): List<PlaylistVideo> {
        val currentId = excludeId ?: _uiState.value.youtubeId
        val unwatched = videoPool
            .filter { it.videoId != currentId && it.videoId !in watchedIds }
            .shuffled()
        if (unwatched.size >= 10) return unwatched.take(10)
        // Not enough unwatched — recycle watched videos (except current)
        val recycled = videoPool
            .filter { it.videoId != currentId && it.videoId !in unwatched.map { u -> u.videoId }.toSet() }
            .shuffled()
        return (unwatched + recycled).take(10)
    }

    private fun navigateToIndex(index: Int) {
        val siblings = _uiState.value.siblingVideos
        if (index < 0 || index >= siblings.size) return

        val nextItem = siblings[index]
        _uiState.value = _uiState.value.copy(
            videoId = nextItem.youtubeId,
            videoTitle = nextItem.title,
            youtubeId = nextItem.youtubeId,
            currentIndex = index,
            hasNext = index < siblings.size - 1,
            hasPrevious = index > 0
        )
    }

    private fun formatRemaining(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
