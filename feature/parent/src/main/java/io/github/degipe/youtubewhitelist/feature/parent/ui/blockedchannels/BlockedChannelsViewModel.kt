package io.github.degipe.youtubewhitelist.feature.parent.ui.blockedchannels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.model.BlockedChannel
import io.github.degipe.youtubewhitelist.core.data.model.ChannelSearchResult
import io.github.degipe.youtubewhitelist.core.data.repository.BlockedChannelRepository
import io.github.degipe.youtubewhitelist.core.data.repository.YouTubeApiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedChannelsUiState(
    val blockedChannels: List<BlockedChannel> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<ChannelSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BlockedChannelsViewModel @Inject constructor(
    private val blockedChannelRepository: BlockedChannelRepository,
    private val youTubeApiRepository: YouTubeApiRepository
) : ViewModel() {

    val blockedChannels: StateFlow<List<BlockedChannel>> =
        blockedChannelRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchState = MutableStateFlow(
        BlockedChannelsUiState()
    )
    val uiState: StateFlow<BlockedChannelsUiState> = _searchState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Keep blockedChannels in sync inside uiState
        viewModelScope.launch {
            blockedChannels.collect { channels ->
                _searchState.value = _searchState.value.copy(blockedChannels = channels)
            }
        }
        viewModelScope.launch {
            searchQueryFlow
                .debounce(400)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _searchState.value = _searchState.value.copy(
                            searchQuery = query,
                            searchResults = emptyList(),
                            isSearching = false,
                            searchError = null
                        )
                    } else {
                        performChannelSearch(query)
                    }
                }
        }
    }

    private suspend fun performChannelSearch(query: String) {
        _searchState.value = _searchState.value.copy(
            searchQuery = query,
            isSearching = true,
            searchError = null
        )
        when (val result = youTubeApiRepository.searchChannels(query)) {
            is AppResult.Success -> {
                _searchState.value = _searchState.value.copy(
                    searchResults = result.data,
                    isSearching = false
                )
            }
            is AppResult.Error -> {
                _searchState.value = _searchState.value.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    searchError = result.message
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.value = _searchState.value.copy(searchQuery = query)
        searchQueryFlow.value = query
    }

    fun blockChannel(channelId: String, channelName: String) {
        viewModelScope.launch {
            blockedChannelRepository.block(channelId, channelName)
        }
    }

    fun unblockChannel(channelId: String) {
        viewModelScope.launch {
            blockedChannelRepository.unblock(channelId)
        }
    }
}
