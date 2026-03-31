package io.github.degipe.youtubewhitelist.feature.kid.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.data.model.WhitelistItem
import io.github.degipe.youtubewhitelist.core.data.repository.BlocklistRepository
import io.github.degipe.youtubewhitelist.core.data.repository.KidProfileRepository
import io.github.degipe.youtubewhitelist.core.data.repository.WhitelistRepository
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerManager
import io.github.degipe.youtubewhitelist.core.data.sleep.SleepTimerStatus
import io.github.degipe.youtubewhitelist.core.data.timelimit.TimeLimitChecker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class KidHomeUiState(
    val profileName: String = "",
    val channels: List<WhitelistItem> = emptyList(),
    val recentVideos: List<WhitelistItem> = emptyList(),
    val playlists: List<WhitelistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val remainingTimeFormatted: String? = null,
    val isTimeLimitReached: Boolean = false,
    val isSleepTimerExpired: Boolean = false
)

@HiltViewModel(assistedFactory = KidHomeViewModel.Factory::class)
class KidHomeViewModel @AssistedInject constructor(
    whitelistRepository: WhitelistRepository,
    kidProfileRepository: KidProfileRepository,
    timeLimitChecker: TimeLimitChecker,
    sleepTimerManager: SleepTimerManager,
    blocklistRepository: BlocklistRepository,
    @Assisted private val profileId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(profileId: String): KidHomeViewModel
    }

    val uiState: StateFlow<KidHomeUiState> = combine(
        kidProfileRepository.getProfileById(profileId),
        whitelistRepository.getChannelsByProfile(profileId),
        whitelistRepository.getVideosByProfile(profileId),
        whitelistRepository.getPlaylistsByProfile(profileId),
        combine(
            timeLimitChecker.getTimeLimitStatus(profileId),
            sleepTimerManager.state,
            blocklistRepository.getBlockedChannelIdsFlow(profileId)
        ) { timeLimit, sleepState, blockedIds -> Triple(timeLimit, sleepState, blockedIds) }
    ) { profile, channels, videos, playlists, combined ->
        val (timeLimitStatus, sleepState, blockedIds) = combined
        val blockedSet = blockedIds.toSet()
        val filteredChannels = channels.filter { it.youtubeId !in blockedSet }
        KidHomeUiState(
            profileName = profile?.name ?: "",
            channels = filteredChannels,
            recentVideos = videos,
            playlists = playlists,
            isLoading = false,
            isEmpty = filteredChannels.isEmpty() && videos.isEmpty() && playlists.isEmpty(),
            remainingTimeFormatted = timeLimitStatus.remainingSeconds?.let { formatRemaining(it) },
            isTimeLimitReached = timeLimitStatus.isLimitReached,
            isSleepTimerExpired = sleepState.status == SleepTimerStatus.EXPIRED
                    && sleepState.profileId == profileId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = KidHomeUiState()
    )

    private fun formatRemaining(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
