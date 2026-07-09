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
    val error: String? = null,
    val editingChannel: ChannelEntity? = null,
    val showMoveDialog: Boolean = false,
    var moveTargetGroup: String = ""
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

    // ── Edit channel ──
    fun startEdit(channel: ChannelEntity) {
        _uiState.value = _uiState.value.copy(editingChannel = channel)
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(editingChannel = null)
    }

    fun saveEdit(name: String, url: String, groupTitle: String) {
        val ch = _uiState.value.editingChannel ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                channelRepository.updateChannel(
                    ch.copy(name = name, url = url, groupTitle = groupTitle)
                )
            }
            _uiState.value = _uiState.value.copy(editingChannel = null)
            _events.emit(ChannelsEvent.ShowSnackbar("Channel updated"))
            loadChannels(refresh = true)
        }
    }

    // ── Delete ──
    fun deleteChannel(channelId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { channelRepository.softDelete(channelId) }
            _events.emit(ChannelsEvent.ShowSnackbar("Channel moved to trash"))
            loadChannels(refresh = true)
        }
    }

    fun deleteFiltered() {
        viewModelScope.launch {
            val ids = _uiState.value.channels.map { it.id }
            if (ids.isEmpty()) return@launch
            withContext(Dispatchers.IO) { channelRepository.softDeleteBatch(playlistId, ids) }
            _events.emit(ChannelsEvent.ShowSnackbar("${ids.size} channels deleted"))
            loadChannels(refresh = true)
        }
    }

    // ── Move to group ──
    fun showMoveDialog() {
        _uiState.value = _uiState.value.copy(showMoveDialog = true, moveTargetGroup = "")
    }

    fun cancelMove() {
        _uiState.value = _uiState.value.copy(showMoveDialog = false)
    }

    fun moveFilteredToGroup(newGroup: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                channelRepository.renameGroup(playlistId, _uiState.value.selectedGroup, newGroup)
            }
            _uiState.value = _uiState.value.copy(showMoveDialog = false)
            _events.emit(ChannelsEvent.ShowSnackbar("Moved to group: $newGroup"))
            loadChannels(refresh = true)
        }
    }

    // ── Replace links ──
    fun replaceLinks(searchTerm: String, replacement: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val all = channelRepository.getAllActiveChannels(playlistId)
                all.filter { it.url.contains(searchTerm, ignoreCase = true) }.forEach { ch ->
                    channelRepository.updateChannel(
                        ch.copy(url = ch.url.replace(searchTerm, replacement, ignoreCase = true))
                    )
                }
            }
            _events.emit(ChannelsEvent.ShowSnackbar("Links replaced"))
            loadChannels(refresh = true)
        }
    }

    // ── Reorder ──
    fun moveChannel(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.channels.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _uiState.value = _uiState.value.copy(channels = list)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                list.forEachIndexed { idx, ch ->
                    channelRepository.updateOrder(ch.id, idx)
                }
            }
        }
    }

    // ── Test links ──
    fun testSingleChannel(channelId: Long, url: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { linkTester.testLinks(listOf(channelId to url)) }
            result.firstOrNull()?.let { res ->
                withContext(Dispatchers.IO) {
                    channelRepository.updateTestStatus(res.channelId, res.status, res.statusCode)
                }
                _events.emit(ChannelsEvent.ShowSnackbar("Test: ${res.status.name}"))
                loadChannels(refresh = true)
            }
        }
    }

    fun testAllLinks() {
        testJob?.cancel()
        testJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testProgress = "Testing links...")
            val urls = withContext(Dispatchers.IO) {
                channelRepository.getAllActiveChannels(playlistId).map { it.id to it.url }
            }
            val batchSize = 50
            var tested = 0
            urls.chunked(batchSize).forEach { batch ->
                val results = linkTester.testLinks(batch)
                withContext(Dispatchers.IO) {
                    results.forEach { r -> channelRepository.updateTestStatus(r.channelId, r.status, r.statusCode) }
                }
                tested += batch.size
                _uiState.value = _uiState.value.copy(testProgress = "Testing... $tested/${urls.size}")
            }
            _uiState.value = _uiState.value.copy(isTesting = false, testProgress = "")
            _events.emit(ChannelsEvent.ShowSnackbar("Tested $tested links"))
            loadChannels(refresh = true)
        }
    }

    // ── Play ──
    fun playChannel(url: String, name: String) {
        viewModelScope.launch { _events.emit(ChannelsEvent.PlayChannel(url, name)) }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
