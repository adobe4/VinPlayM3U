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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

    LaunchedEffect(playlistId) {
        viewModel.init(playlistId)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChannelsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChannelsEvent.PlayChannel -> {
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        putExtra(PlayerActivity.EXTRA_URL, event.url)
                        putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, event.name)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    // Load more when scrolling near the end
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val totalItems = uiState.channels.size
        if (totalItems > 0 && listState.firstVisibleItemIndex >= totalItems - 50) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName.ifBlank { "Channels" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.testAllLinks() }) {
                        Icon(Icons.Default.Science, contentDescription = "Test links")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Search channels...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            )

            // Filters row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group filter
                Box {
                    FilterChip(
                        selected = uiState.selectedGroup != null,
                        onClick = { showGroupFilter = true },
                        label = { Text(uiState.selectedGroup ?: "All Groups") }
                    )
                    DropdownMenu(
                        expanded = showGroupFilter,
                        onDismissRequest = { showGroupFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Groups") },
                            onClick = {
                                viewModel.filterByGroup(null)
                                showGroupFilter = false
                            }
                        )
                        uiState.groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = {
                                    viewModel.filterByGroup(group)
                                    showGroupFilter = false
                                }
                            )
                        }
                    }
                }

                // Kind filter
                Box {
                    FilterChip(
                        selected = uiState.selectedKind != null,
                        onClick = { showKindFilter = true },
                        label = { Text(uiState.selectedKind?.name ?: "All Types") }
                    )
                    DropdownMenu(
                        expanded = showKindFilter,
                        onDismissRequest = { showKindFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Types") },
                            onClick = {
                                viewModel.filterByKind(null)
                                showKindFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("LIVE") },
                            onClick = {
                                viewModel.filterByKind(ChannelKind.LIVE)
                                showKindFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("VOD") },
                            onClick = {
                                viewModel.filterByKind(ChannelKind.VOD)
                                showKindFilter = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("SERIES") },
                            onClick = {
                                viewModel.filterByKind(ChannelKind.SERIES)
                                showKindFilter = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress indicator for testing
            if (uiState.isTesting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        uiState.testProgress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Channel count
            Text(
                text = "${uiState.totalCount} channels",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Content
            when {
                uiState.isLoading -> {
                    LoadingSkeleton(count = 8)
                }
                uiState.channels.isEmpty() -> {
                    EmptyState(
                        title = "No Channels Found",
                        subtitle = "Try adjusting your search or filters"
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.channels, key = { it.id }) { channel ->
                            ChannelItem(
                                channel = channel,
                                onPlay = { viewModel.playChannel(channel.url, channel.name) },
                                onDelete = { viewModel.deleteChannel(channel.id) }
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
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
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        if (channel.tvgLogo.isNotBlank()) {
            AsyncImage(
                model = channel.tvgLogo,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Kind badge
                KindBadge(kind = channel.kind)
                if (channel.groupTitle.isNotBlank()) {
                    Text(
                        text = channel.groupTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                // Test status
                TestStatusIndicator(status = channel.testStatus)
            }
        }

        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun KindBadge(kind: ChannelKind) {
    val color = when (kind) {
        ChannelKind.LIVE -> ChannelLive
        ChannelKind.VOD -> ChannelVod
        ChannelKind.SERIES -> ChannelSeries
        ChannelKind.UNKNOWN -> ChannelUnknown
    }
    val label = when (kind) {
        ChannelKind.LIVE -> "LIVE"
        ChannelKind.VOD -> "VOD"
        ChannelKind.SERIES -> "SERIES"
        ChannelKind.UNKNOWN -> "?"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun TestStatusIndicator(status: TestStatus) {
    val color = when (status) {
        TestStatus.ONLINE -> Color(0xFF4CAF50)
        TestStatus.OFFLINE -> Color(0xFFF44336)
        TestStatus.TIMEOUT -> Color(0xFFFF9800)
        TestStatus.ERROR -> Color(0xFF9E9E9E)
        TestStatus.UNCHECKED -> Color.Transparent
    }
    if (status != TestStatus.UNCHECKED) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
