package com.tally.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tally.app.ui.detail.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = {
            // ponytail: only the watchlist button animates out — checkbox stays visible
            AnimatedVisibility(
                visible = !state.isWatched,
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = viewModel::onToggleWatchlist) {
                            Text(if (state.isWatchlisted) "Watchlisted" else "Add to Watchlist")
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val err = state.error
            if (err != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = err, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        state.backdropUrl?.let { url ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    item {
                        // ponytail: title row with watched checkbox pinned right — always visible
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.weight(1f),
                            )
                            WatchedCheckbox(
                                isWatched = state.isWatched,
                                rewatchCount = state.rewatchCount,
                                onCheckWatched = viewModel::onCheckWatched,
                                onUncheckWatched = viewModel::onUncheckWatched,
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val metaParts = mutableListOf<String>()
                            state.year?.let { metaParts.add(it.toString()) }
                            if (state.genres.isNotEmpty()) {
                                metaParts.add(state.genres.take(3).joinToString(", "))
                            }
                            state.runtime?.let { mins ->
                                metaParts.add("${mins}m")
                            }
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" \u00b7 "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val rating = state.rating
                            val votes = state.voteCount
                            if (rating != null && rating > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "\u2605 %.1f".format(rating),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (votes != null && votes > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${formatVotes(votes)} votes)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            state.synopsis?.let { synopsis ->
                                if (synopsis.isNotBlank()) {
                                    Text(
                                        text = synopsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    if (state.mediaType == "tv" && state.seasonLabels.isNotEmpty()) {
                        item {
                            val currentSeasonNum = state.episodes.firstOrNull()?.seasonNumber ?: (state.selectedSeasonIndex + 1)
                            val watchedInSeason = state.watchedEpisodes.filter { it.first == currentSeasonNum }.size
                            val totalInSeason = state.episodes.size
                            val allWatched = totalInSeason > 0 && watchedInSeason == totalInSeason

                            SeasonHeader(
                                seasonLabels = state.seasonLabels,
                                selectedIndex = state.selectedSeasonIndex,
                                onSeasonSelected = viewModel::onSeasonSelected,
                                watchedCount = watchedInSeason,
                                totalCount = totalInSeason,
                                allWatched = allWatched,
                                onToggleAll = { watchAll ->
                                    viewModel.onToggleSeasonWatched(currentSeasonNum, totalInSeason, watchAll)
                                },
                            )
                        }

                        items(state.episodes, key = { it.id }) { episode ->
                            val isWatched = (episode.seasonNumber to episode.episodeNumber) in state.watchedEpisodes
                            EpisodeItem(
                                episode = episode,
                                isWatched = isWatched,
                                onToggleWatched = {
                                    viewModel.onToggleEpisodeWatched(episode.seasonNumber, episode.episodeNumber)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchedCheckbox(
    isWatched: Boolean,
    rewatchCount: Int,
    onCheckWatched: () -> Unit,
    onUncheckWatched: (Boolean) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Checkbox(
            checked = isWatched,
            onCheckedChange = { checked ->
                if (checked) {
                    onCheckWatched()
                } else {
                    showMenu = true
                }
            },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (isWatched && rewatchCount > 1) {
            Text(
                text = "x$rewatchCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Not Watched") },
                onClick = {
                    onUncheckWatched(false)
                    showMenu = false
                },
            )
            DropdownMenuItem(
                text = { Text("Rewatched") },
                onClick = {
                    onUncheckWatched(true)
                    showMenu = false
                },
            )
        }
    }
}

@Composable
private fun SeasonHeader(
    seasonLabels: List<String>,
    selectedIndex: Int,
    onSeasonSelected: (Int) -> Unit,
    watchedCount: Int,
    totalCount: Int,
    allWatched: Boolean,
    onToggleAll: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { expanded = true }) {
                Text(text = seasonLabels.getOrElse(selectedIndex) { "Season ${selectedIndex + 1}" })
            }
            if (totalCount > 0) {
                TextButton(onClick = { onToggleAll(!allWatched) }) {
                    Text(
                        text = if (allWatched) "Unwatch All" else "Watch All ($watchedCount/$totalCount)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            seasonLabels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSeasonSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: com.tally.app.data.remote.model.TmdbEpisode,
    isWatched: Boolean,
    onToggleWatched: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = isWatched,
            onCheckedChange = { onToggleWatched() },
            modifier = Modifier.padding(end = 4.dp),
        )

        Text(
            text = "${episode.episodeNumber}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            episode.overview?.let { overview ->
                if (overview.isNotBlank()) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

internal fun formatVotes(votes: Int): String = when {
    votes >= 1_000_000 -> "%.1fM".format(votes / 1_000_000.0)
    votes >= 1_000 -> "%.1fk".format(votes / 1_000.0)
    else -> votes.toString()
}
