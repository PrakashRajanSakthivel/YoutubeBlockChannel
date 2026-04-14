package io.github.degipe.youtubewhitelist.feature.kid.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.degipe.youtubewhitelist.core.common.result.AppResult
import io.github.degipe.youtubewhitelist.core.data.model.SearchResult
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

data class KidSearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = KidSearchViewModel.Factory::class)
class KidSearchViewModel @AssistedInject constructor(
    private val youTubeApiRepository: YouTubeApiRepository,
    private val blockedChannelRepository: BlockedChannelRepository,
    @Assisted private val profileId: String
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(profileId: String): KidSearchViewModel
    }

    private val blockedIds: StateFlow<Set<String>> = blockedChannelRepository.getAllIds()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // In-memory LRU cache: query -> (results, fetchedAt)
    private val searchCache = LinkedHashMap<String, Pair<List<SearchResult>, Long>>(20, 0.75f, true)
    private val cacheTtlMs = 5 * 60 * 1000L

    private val _uiState = MutableStateFlow(KidSearchUiState())
    val uiState: StateFlow<KidSearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    val query: StateFlow<String> = queryFlow.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .collectLatest { q ->
                    if (q.isBlank()) {
                        _uiState.value = KidSearchUiState(query = q)
                    } else {
                        performSearch(q)
                    }
                }
        }
    }

    private suspend fun performSearch(query: String) {
        val now = System.currentTimeMillis()
        val cached = searchCache[query]
        if (cached != null && (now - cached.second) < cacheTtlMs) {
            val filtered = cached.first.filter { it.channelId !in blockedIds.value }
            _uiState.value = KidSearchUiState(query = query, results = filtered)
            return
        }

        _uiState.value = KidSearchUiState(query = query, isLoading = true)

        when (val result = youTubeApiRepository.searchGlobal(query)) {
            is AppResult.Success -> {
                if (searchCache.size >= 20) {
                    searchCache.remove(searchCache.keys.first())
                }
                searchCache[query] = Pair(result.data, now)
                val filtered = result.data.filter { it.channelId !in blockedIds.value }
                _uiState.value = KidSearchUiState(query = query, results = filtered)
            }
            is AppResult.Error -> {
                _uiState.value = KidSearchUiState(query = query, error = result.message)
            }
        }
    }

    fun onQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun onClearQuery() {
        queryFlow.value = ""
    }
}
