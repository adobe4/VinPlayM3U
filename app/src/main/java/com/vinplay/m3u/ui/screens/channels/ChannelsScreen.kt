package com.vinplay.m3u.ui.screens.channels

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatchPrediction
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vinplay.m3u.data.local.entity.ChannelEntity
import com.vinplay.m3u.data.model.ChannelKind
import com.vinplay.m3u.data.model.TestStatus
import com.vinplay.m3u.player.PlayerActivity
import com.vinplay.m3u.ui.components.EmptyState
import com.vinplay.m3u.ui.components.LoadingSkeleton
import com.vinplay.m3u.ui.theme.ChannelLive
import com.vinplay.m3u.ui.theme.ChannelSeries
import com.vinplay.m3u.ui.theme.ChannelUnknown
import com.vinplay.m3u.ui.theme.ChannelVod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    playlistId: Long,
    playlistName: String,
    onBack: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showGroupFilter by remember { mutableStateOf(false) }
    var showKindFilter by remember { mutableStateOf(false) }
    var showBatchMenu by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var replaceSearch by remember { mutableStateOf("") }
    var replaceWith by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf(false) }
    var moveGroupText by remember { mutableStateOf("") }

    LaunchedEffect(playlistId) { viewModel.init(playlistId) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChannelsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChannelsEvent.PlayChannel -> {
                    context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_URL, event.url)
                        putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, event.name)
                    })
                }
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        val total = uiState.channels.size
        if (total > 0 && listState.firstVisibleItemIndex >= total - 50) viewModel.loadMore()
    }

    // ── Edit Dialog ──
    uiState.editingChannel?.let { ch ->
        var editName by remember(ch.id) { mutableStateOf(ch.name) }
        var editUrl by remember(ch.id) { mutableStateOf(ch.url) }
        var editGroup by remember(ch.id) { mutableStateOf(ch.groupTitle) }
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("Edit Channel") },
            text = {
                Column {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editUrl, onValueChange = { editUrl = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editGroup, onValueChange = { editGroup = it }, label = { Text("Group") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.saveEdit(editName.trim(), editUrl.trim(), editGroup.trim()) }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { viewModel.cancelEdit() }) { Text("Cancel") } }
        )
    }

    // ── Replace Links Dialog ──
    if (showReplaceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Replace Links") },
            text = {
                Column {
                    OutlinedTextField(value = replaceSearch, onValueChange = { replaceSearch = it }, label = { Text("Search for") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = replaceWith, onValueChange = { replaceWith = it }, label = { Text("Replace with") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Example: replace 'old.server.com' with 'new.server.com'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (replaceSearch.isNotBlank()) { viewModel.replaceLinks(replaceSearch, replaceWith); showReplaceDialog = false }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { showReplaceDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Move to Group Dialog ──
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to Group") },
            text = {
                Column {
                    OutlinedTextField(value = moveGroupText, onValueChange = { moveGroupText = it }, label = { Text("Group name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (uiState.groups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Existing groups:", style = MaterialTheme.typography.labelSmall)
                        uiState.groups.take(10).forEach { g ->
                            TextButton(onClick = { moveGroupText = g }) { Text(g, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (moveGroupText.isNotBlank()) { viewModel.moveFilteredToGroup(moveGroupText.trim()); showMoveDialog = false }
                }) { Text("Move") }
            },
            dismissButton = { TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName.ifBlank { "Channels" }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    // Batch actions menu
                    IconButton(onClick = { showBatchMenu = true }) {
                        Icon(Icons.Default.Build, contentDescription = "Batch actions")
                    }
                    DropdownMenu(expanded = showBatchMenu, onDismissRequest = { showBatchMenu = false }) {
                        DropdownMenuItem(text = { Text("Test All Links") }, onClick = { showBatchMenu = false; viewModel.testAllLinks() },
                            leadingIcon = { Icon(Icons.Default.Science, null) })
                        DropdownMenuItem(text = { Text("Delete Filtered") }, onClick = { showBatchMenu = false; viewModel.deleteFiltered() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                        DropdownMenuItem(text = { Text("Move to Group...") }, onClick = { showBatchMenu = false; showMoveDialog = true; moveGroupText = "" },
                            leadingIcon = { Icon(Icons.Default.Groups, null) })
                        DropdownMenuItem(text = { Text("Replace Links...") }, onClick = { showBatchMenu = false; showReplaceDialog = true },
                            leadingIcon = { Icon(Icons.Default.FindReplace, null) })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Search channels...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            )

            // Filters
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    FilterChip(selected = uiState.selectedGroup != null, onClick = { showGroupFilter = true }, label = { Text(uiState.selectedGroup ?: "All Groups") })
                    DropdownMenu(expanded = showGroupFilter, onDismissRequest = { showGroupFilter = false }) {
                        DropdownMenuItem(text = { Text("All Groups") }, onClick = { viewModel.filterByGroup(null); showGroupFilter = false })
                        uiState.groups.forEach { g -> DropdownMenuItem(text = { Text(g) }, onClick = { viewModel.filterByGroup(g); showGroupFilter = false }) }
                    }
                }
                Box {
                    FilterChip(selected = uiState.selectedKind != null, onClick = { showKindFilter = true }, label = { Text(uiState.selectedKind?.name ?: "All Types") })
                    DropdownMenu(expanded = showKindFilter, onDismissRequest = { showKindFilter = false }) {
                        listOf(null to "All Types", ChannelKind.LIVE to "LIVE", ChannelKind.VOD to "VOD", ChannelKind.SERIES to "SERIES").forEach { (k, l) ->
                            DropdownMenuItem(text = { Text(l) }, onClick = { viewModel.filterByKind(k); showKindFilter = false })
                        }
                    }
                }
            }

            // Testing progress
            if (uiState.isTesting) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.testProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("${uiState.totalCount} channels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            // Channel list
            when {
                uiState.isLoading -> LoadingSkeleton(count = 8)
                uiState.channels.isEmpty() -> EmptyState(title = "No Channels", subtitle = "Import channels using the Import button on the playlist")
                else -> {
                    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        itemsIndexed(uiState.channels, key = { _, ch -> ch.id }) { index, channel ->
                            ChannelItem(
                                channel = channel,
                                index = index,
                                totalCount = uiState.channels.size,
                                onPlay = { viewModel.playChannel(channel.url, channel.name) },
                                onEdit = { viewModel.startEdit(channel) },
                                onDelete = { viewModel.deleteChannel(channel.id) },
                                onTest = { viewModel.testSingleChannel(channel.id, channel.url) },
                                onMoveUp = if (index > 0) ({ viewModel.moveChannel(index, index - 1) }) else null,
                                onMoveDown = if (index < uiState.channels.size - 1) ({ viewModel.moveChannel(index, index + 1) }) else null
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: ChannelEntity,
    index: Int,
    totalCount: Int,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reorder buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = { onMoveUp?.invoke() }, modifier = Modifier.size(24.dp), enabled = onMoveUp != null) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { onMoveDown?.invoke() }, modifier = Modifier.size(24.dp), enabled = onMoveDown != null) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
            }
        }

        // Logo / initials
        if (channel.tvgLogo.isNotBlank()) {
            AsyncImage(model = channel.tvgLogo, contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        } else {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text(channel.name.take(2).uppercase(), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindBadge(kind = channel.kind)
                if (channel.groupTitle.isNotBlank()) {
                    Text(channel.groupTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                }
                TestStatusDot(status = channel.testStatus)
            }
        }

        // Action buttons
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Edit", modifier = Modifier.size(18.dp)) }
        IconButton(onClick = onTest, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.BatchPrediction, contentDescription = "Test", modifier = Modifier.size(18.dp)) }
        IconButton(onClick = onPlay, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun KindBadge(kind: ChannelKind) {
    val (color, label) = when (kind) {
        ChannelKind.LIVE -> ChannelLive to "LIVE"
        ChannelKind.VOD -> ChannelVod to "VOD"
        ChannelKind.SERIES -> ChannelSeries to "SERIES"
        ChannelKind.UNKNOWN -> ChannelUnknown to "?"
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun TestStatusDot(status: TestStatus) {
    val color = when (status) {
        TestStatus.ONLINE -> Color(0xFF4CAF50)
        TestStatus.OFFLINE -> Color(0xFFF44336)
        TestStatus.TIMEOUT -> Color(0xFFFF9800)
        TestStatus.ERROR -> Color(0xFF9E9E9E)
        TestStatus.UNCHECKED -> Color.Transparent
    }
    if (status != TestStatus.UNCHECKED) {
        Box(modifier = Modifier.padding(start = 6.dp).size(7.dp).clip(CircleShape).background(color))
    }
}
