package com.vinplay.m3u.ui.screens.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinplay.m3u.data.model.ImportProgress
import com.vinplay.m3u.data.parser.M3UParser
import com.vinplay.m3u.data.repository.ChannelRepository
import com.vinplay.m3u.data.repository.PlaylistRepository
import com.vinplay.m3u.network.M3UDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportUiState(
    val isImporting: Boolean = false,
    val progress: String = "",
    val totalChannels: Int = 0,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val m3uDownloader: M3UDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val parser = M3UParser()
    private var importJob: Job? = null

    fun importFromUrl(playlistId: Long, url: String) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.value = ImportUiState(isImporting = true, progress = "Downloading playlist...")
            try {
                val stream = withContext(Dispatchers.IO) { m3uDownloader.downloadStream(url) }
                parseAndInsert(playlistId, stream)
            } catch (e: Exception) {
                _uiState.value = ImportUiState(
                    error = "Download failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun importFromFile(playlistId: Long, uri: Uri) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.value = ImportUiState(isImporting = true, progress = "Reading file...")
            try {
                val stream = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                } ?: throw Exception("Cannot open file")
                parseAndInsert(playlistId, stream)
            } catch (e: Exception) {
                _uiState.value = ImportUiState(
                    error = "File read failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun importFromText(playlistId: Long, text: String) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _uiState.value = ImportUiState(isImporting = true, progress = "Parsing text...")
            try {
                val stream = withContext(Dispatchers.IO) {
                    text.byteInputStream()
                }
                parseAndInsert(playlistId, stream)
            } catch (e: Exception) {
                _uiState.value = ImportUiState(
                    error = "Parse failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun importFromUrlAndMerge(playlistId: Long, url: String) {
        // Same as importFromUrl — channels get appended to existing playlist
        importFromUrl(playlistId, url)
    }

    private suspend fun parseAndInsert(playlistId: Long, stream: java.io.InputStream) {
        val startOrder = channelRepository.getMaxOrderIndex(playlistId)
        var currentOrder = startOrder

        parser.parseStream(stream, playlistId).collect { batch ->
            _uiState.value = _uiState.value.copy(
                progress = "Importing ${batch.totalSoFar} channels...",
                totalChannels = batch.totalSoFar
            )
            withContext(Dispatchers.IO) {
                currentOrder = channelRepository.insertParsedBatch(
                    batch.channels, playlistId, currentOrder
                )
            }
        }

        // Update playlist timestamp
        playlistRepository.touchPlaylist(playlistId)

        _uiState.value = ImportUiState(success = true, progress = "Import complete!")
    }

    override fun onCleared() {
        super.onCleared()
        importJob?.cancel()
    }
}
