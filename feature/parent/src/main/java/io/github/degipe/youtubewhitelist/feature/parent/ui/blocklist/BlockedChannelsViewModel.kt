package io.github.degipe.youtubewhitelist.feature.parent.ui.blocklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import io.github.degipe.youtubewhitelist.core.data.repository.BlocklistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedChannelsUiState(
    val blockedChannels: List<BlockedChannel> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel(assistedFactory = BlockedChannelsViewModel.Factory::class)
class BlockedChannelsViewModel @AssistedInject constructor(
    private val blocklistRepository: BlocklistRepository,
    @Assisted private val profileId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(profileId: String): BlockedChannelsViewModel
    }

    private val _uiState = MutableStateFlow(BlockedChannelsUiState())
    val uiState: StateFlow<BlockedChannelsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            blocklistRepository.getAllBlockedChannels(profileId).collect { channels ->
                _uiState.update { it.copy(blockedChannels = channels, isLoading = false) }
            }
        }
    }

    fun unblockChannel(channel: BlockedChannel) {
        viewModelScope.launch {
            blocklistRepository.unblockChannel(profileId, channel.channelId)
        }
    }

    fun blockChannel(channelId: String, channelTitle: String, channelThumbnailUrl: String?) {
        viewModelScope.launch {
            blocklistRepository.blockChannel(profileId, channelId, channelTitle, channelThumbnailUrl)
        }
    }
}
