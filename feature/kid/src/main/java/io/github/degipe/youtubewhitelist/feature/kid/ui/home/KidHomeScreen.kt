package io.github.degipe.youtubewhitelist.feature.kid.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.degipe.youtubewhitelist.core.common.util.TvDetector
import io.github.degipe.youtubewhitelist.core.data.model.PlaylistVideo
import io.github.degipe.youtubewhitelist.core.data.model.WhitelistItem

@Composable
fun KidHomeScreen(
    viewModel: KidHomeViewModel,
    onParentAccess: () -> Unit,
    onSearchClick: () -> Unit,
    onChannelClick: (youtubeId: String, channelTitle: String, thumbnailUrl: String) -> Unit,
    onVideoClick: (videoId: String, videoTitle: String, channelTitle: String?) -> Unit,
    onPlaylistClick: (youtubeId: String, title: String, thumbnailUrl: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isTV = remember { TvDetector.isTV(context) }

    // Block back button in kid mode — only parent can exit via PIN
    BackHandler { /* Intentionally empty — prevents exiting kid mode */ }

    Scaffold(
        floatingActionButton = {
            // Hide parent access FAB on TV (no touch input / no PIN keyboard)
            if (!isTV) {
                FloatingActionButton(
                    onClick = onParentAccess,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Parent Mode"
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.isEmpty -> {
                    EmptyContent(
                        profileName = uiState.profileName,
                        modifier = Modifier.padding(padding)
                    )
                }
                else -> {
                    KidHomeContent(
                        uiState = uiState,
                        onSearchClick = onSearchClick,
                        onChannelClick = onChannelClick,
                        onVideoClick = onVideoClick,
                        onPlaylistClick = onPlaylistClick,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            // Time's Up overlay
            if (uiState.isTimeLimitReached) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Time's Up!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Your daily screen time is over.\nAsk a parent to continue.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isTV) {
                            Spacer(modifier = Modifier.height(24.dp))
                            FloatingActionButton(
                                onClick = onParentAccess,
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Parent Mode")
                            }
                        }
                    }
                }
            }

            // Good Night overlay (sleep timer expired)
            if (uiState.isSleepTimerExpired) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A1A).copy(alpha = 0.98f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF7B68EE)
                        )
                        Text(
                            text = "Good Night!",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFFB0B0D0)
                        )
                        Text(
                            text = "Time to sleep.\nSweet dreams!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFB0B0D0).copy(alpha = 0.7f)
                        )
                        if (!isTV) {
                            Spacer(modifier = Modifier.height(24.dp))
                            FloatingActionButton(
                                onClick = onParentAccess,
                                containerColor = Color(0xFF7B68EE)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Parent Mode",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(profileName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (profileName.isNotEmpty()) "Hi $profileName!" else "My Videos",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No whitelisted content yet. Ask a parent to add videos!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KidHomeContent(
    uiState: KidHomeUiState,
    onSearchClick: () -> Unit,
    onChannelClick: (youtubeId: String, channelTitle: String, thumbnailUrl: String) -> Unit,
    onVideoClick: (videoId: String, videoTitle: String, channelTitle: String?) -> Unit,
    onPlaylistClick: (youtubeId: String, title: String, thumbnailUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    // Adaptive columns: 2 on phone portrait, 3 on phone landscape/small tablet, 4+ on TV/large
    val columnCount = when {
        screenWidthDp >= 960 -> 5
        screenWidthDp >= 720 -> 4
        screenWidthDp >= 480 -> 3
        else -> 2
    }
    val isWide = screenWidthDp >= 480
    val spacing = if (isWide) 16.dp else 10.dp
    val horizontalPadding = if (isWide) 24.dp else 12.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Greeting + Search — full width
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (uiState.profileName.isNotEmpty()) "Hi ${uiState.profileName}!" else "My Videos",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            }
        }

        // Remaining time chip — full width
        uiState.remainingTimeFormatted?.let { remaining ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Time remaining: $remaining",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Channels — horizontal scrollable row (compact) — full width
        if (uiState.channels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Channels",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(uiState.channels, key = { it.id }) { channel ->
                            CompactChannelChip(
                                channel = channel,
                                onClick = { onChannelClick(channel.youtubeId, channel.title, channel.thumbnailUrl) }
                            )
                        }
                    }
                }
            }
        }

        // Latest Videos from channels — 2-column grid
        if (uiState.latestVideos.isNotEmpty() || uiState.isLoadingVideos) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Latest Videos",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (uiState.isLoadingVideos && uiState.latestVideos.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            items(uiState.latestVideos, key = { it.videoId }) { video ->
                LatestVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.videoId, video.title, video.channelTitle) }
                )
            }
        }

        // Individually whitelisted videos — full width row
        if (uiState.recentVideos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Videos",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(uiState.recentVideos, key = { it.id }) { video ->
                            WhitelistVideoCard(
                                video = video,
                                onClick = { onVideoClick(video.youtubeId, video.title, video.channelTitle) }
                            )
                        }
                    }
                }
            }
        }

        // Playlists — full width row
        if (uiState.playlists.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 16.dp)
                    ) {
                        items(uiState.playlists, key = { it.id }) { playlist ->
                            WhitelistVideoCard(
                                video = playlist,
                                onClick = { onPlaylistClick(playlist.youtubeId, playlist.title, playlist.thumbnailUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactChannelChip(
    channel: WhitelistItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isFocused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick(); true
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = channel.thumbnailUrl,
            contentDescription = channel.title,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channel.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LatestVideoCard(
    video: PlaylistVideo,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFocused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick(); true
                } else false
            }
    ) {
        Column {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WhitelistVideoCard(
    video: WhitelistItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .width(200.dp)
            .then(
                if (isFocused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onClick(); true
                } else false
            }
    ) {
        Column {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                video.channelTitle?.let { channelTitle ->
                    Text(
                        text = channelTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
