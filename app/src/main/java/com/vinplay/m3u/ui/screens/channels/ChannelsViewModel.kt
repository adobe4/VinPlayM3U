package com.vinplay.m3u.ui.screens.channels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinplay.m3u.data.local.entity.ChannelEntity
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.TestStatus
import com.vinplay.m3u.data.repository.ChannelRepository
import com.vinplay.m3u.data.repository.PlaylistRepository
import com.vinplay.m3u.network.LinkTester
import com.vinplay.m3u.player.PlayerActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChannelsUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val selectedGroup: String? = null,
    val selectedKind: ChannelKind? = null,
    val groups: List<String> = emptyList(),
    val isTesting: Boolean = false,
    val testProgress: String = "",
    val error: String? = null
)

sealed class ChannelsEvent {
    data class ShowSnackbar(val message: String) : ChannelsEvent()
    data class PlayChannel(val url: String, val name: String) : ChannelsEvent()
}

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val playlistRepository: PlaylistRepository,
    private val linkTester: LinkTester
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChannelsEvent>()
    val events = _events.asSharedFlow()

    private var playlistId: Long = 0
    private val pageSize = 200
    private var currentOffset = 0
    private var hasMore = true
    private var loadJob: Job? = null
    private var testJob: Job? = null

    fun init(playlistId: Long) {
        this.playlistId = playlistId
        loadGroups()
        loadChannels(refresh = true)
    }

    fun loadChannels(refresh: Boolean = false) {
        if (refresh) {
            currentOffset = 0
            hasMore = true
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val state = _uiState.value
                val results = withContext(Dispatchers.IO) {
                    channelRepository.searchChannels(
                        playlistId = playlistId,
                        query = state.searchQuery.ifBlank { null },
                        groupFilter = state.selectedGroup,
                        kindFilter = state.selectedKind?.name,
                        limit = pageSize,
                        offset = currentOffset
                    )
                }

                val total = withContext(Dispatchers.IO) {
                    channelRepository.countChannels(
                        playlistId = playlistId,
                        query = state.searchQuery.ifBlank { null },
                        groupFilter = state.selectedGroup,
                        kindFilter = state.selectedKind?.name
                    )
                }

                if (refresh) {
                    _uiState.value = _uiState.value.copy(
                        channels = results,
                        totalCount = total,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        channels = _uiState.value.channels + results,
                        totalCount = total,
                        isLoadingMore = false
                    )
                }

                currentOffset += results.size
                hasMore = results.size >= pageSize
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message
                )
            }
        }
    }

    fun loadMore() {
        if (!hasMore || _uiState.value.isLoadingMore) return
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        loadChannels()
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadChannels(refresh = true)
    }

    fun filterByGroup(group: String?) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        loadChannels(refresh = true)
    }

    fun filterByKind(kind: ChannelKind?) {
        _uiState.value = _uiState.value.copy(selectedKind = kind)
        loadChannels(refresh = true)
    }

    private fun loadGroups() {
        viewModelScope.launch {
            channelRepository.getGroups(playlistId).collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }
        }
    }

    fun deleteChannel(channelId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                channelRepository.softDelete(channelId)
            }
            _events.emit(ChannelsEvent.ShowSnackbar("Channel moved to trash"))
            loadChannels(refresh = true)
        }
    }

    fun deleteFilteredChannels() {
        viewModelScope.launch {
            val state = _uiState.value
            val toDelete = state.channels.map { it.id }
            if (toDelete.isEmpty()) return@launch
            withContext(Dispatchers.IO) {
                channelRepository.softDeleteBatch(playlistId, toDelete)
            }
            _events.emit(ChannelsEvent.ShowSnackbar("${toDelete.size} channels deleted"))
            loadChannels(refresh = true)
        }
    }

    fun testAllLinks() {
        testJob?.cancel()
        testJob = viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isTesting = true, testProgress = "Testing links...")

            val urls = withContext(Dispatchers.IO) {
                channelRepository.getAllActiveChannels(playlistId)
                    .map { it.id to it.url }
            }

            val batchSize = 50
            var tested = 0

            urls.chunked(batchSize).forEach { batch ->
                val results = linkTester.testLinks(batch)
                withContext(Dispatchers.IO) {
                    results.forEach { result ->
                        channelRepository.updateTestStatus(
                            result.channelId, result.status, result.statusCode
                        )
                    }
                }
                tested += batch.size
                _uiState.value = _uiState.value.copy(
                    testProgress = "Testing... $tested/${urls.size}"
                )
            }

            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testProgress = "Test complete: $tested links checked"
            )
            _events.emit(ChannelsEvent.ShowSnackbar("Test complete: $tested links checked"))
            loadChannels(refresh = true)
        }
    }

    fun renameGroup(oldGroup: String, newGroup: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                channelRepository.renameGroup(playlistId, oldGroup, newGroup)
            }
            _events.emit(ChannelsEvent.ShowSnackbar("Group renamed"))
            loadChannels(refresh = true)
        }
    }

    fun playChannel(url: String, name: String) {
        viewModelScope.launch {
            _events.emit(ChannelsEvent.PlayChannel(url, name))
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
