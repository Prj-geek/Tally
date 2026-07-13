package com.tally.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tally.app.BuildConfig
import com.tally.app.ui.profile.AuthState
import com.tally.app.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch

private const val MAX_PREVIEW_ITEMS = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onItemClick: (tmdbId: Int, mediaType: String) -> Unit = { _, _ -> },
    onImportTvTime: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsState()
    val watchedState by viewModel.watchedState.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()
    val isSyncing by viewModel.syncManager.isSyncing.collectAsState()
    val pendingCount by viewModel.syncManager.pendingCount.collectAsState()
    val lastSyncError by viewModel.syncManager.lastSyncError.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAllMovies by remember { mutableStateOf(false) }
    var showAllShows by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) viewModel.loadWatched()
    }

    val signIn: () -> Unit = {
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.signInWithGoogle(credential.idToken)
            } catch (e: Exception) {
                Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear Data") },
            text = { Text("This will permanently delete all your watch history and watchlist data. This cannot be undone.") },
            confirmButton = {
                OutlinedButton(onClick = {
                    showClearDataDialog = false
                    viewModel.clearData()
                }) { Text("Clear") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                TextButtonItem("Import from TV Time") {
                    showMenu = false
                    onImportTvTime()
                }
                Spacer(Modifier.height(8.dp))
                TextButtonItem("Clear Data") {
                    showMenu = false
                    showClearDataDialog = true
                }
                Spacer(Modifier.height(8.dp))
                TextButtonItem("Sign Out") {
                    showMenu = false
                    viewModel.signOut()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    when (val state = authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is AuthState.SignedOut -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Sign in to sync your data", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = signIn) { Text("Sign in with Google") }
            }
        }

        is AuthState.SignedIn -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Profile") },
                    navigationIcon = {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(36.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop,
                        )
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                    },
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Sync status chip
                    item {
                        SyncStatusChip(
                            isSyncing = isSyncing,
                            pendingCount = pendingCount,
                            lastSyncError = lastSyncError,
                            onTap = { viewModel.syncManager.sync() },
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // Stats
                    item {
                        val parts = mutableListOf<String>()
                        if (watchedState.movieWatchTimeMinutes > 0) {
                            parts += "Movie runtime: ${watchedState.movieWatchTimeMinutes / 60}h ${watchedState.movieWatchTimeMinutes % 60}m"
                        }
                        if (watchedState.tvWatchTimeMinutes > 0) {
                            parts += "TV runtime: ${watchedState.tvWatchTimeMinutes / 60}h ${watchedState.tvWatchTimeMinutes % 60}m"
                        }
                        if (watchedState.watchedMovieCount > 0 || watchedState.watchedTvCount > 0) {
                            parts += "Movies watched: ${watchedState.watchedMovieCount} | Shows watched: ${watchedState.watchedTvCount}"
                        }
                        if (parts.isNotEmpty()) {
                            Text(
                                text = parts.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Watched Movies
                    item {
                        SectionHeader(
                            title = "Watched Movies",
                            showAll = showAllMovies,
                            onToggle = { showAllMovies = !showAllMovies },
                        )
                    }
                    if (watchedState.watchedMovies.isEmpty()) {
                        item {
                            Text(
                                "No watched movies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        item {
                            val items = if (showAllMovies) watchedState.watchedMovies
                                else watchedState.watchedMovies.take(MAX_PREVIEW_ITEMS)
                            LazyRow {
                                items(items, key = { "wm_${it.tmdbId}" }) { item ->
                                    WatchedPoster(
                                        item = item,
                                        onClick = { onItemClick(item.tmdbId.toInt(), "movie") },
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }

                    // Watched Shows
                    item {
                        SectionHeader(
                            title = "Watched Shows",
                            showAll = showAllShows,
                            onToggle = { showAllShows = !showAllShows },
                        )
                    }
                    if (watchedState.watchedShows.isEmpty()) {
                        item {
                            Text(
                                "No watched shows",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        item {
                            val items = if (showAllShows) watchedState.watchedShows
                                else watchedState.watchedShows.take(MAX_PREVIEW_ITEMS)
                            LazyRow {
                                items(items, key = { "ws_${it.tmdbId}" }) { item ->
                                    WatchedPoster(
                                        item = item,
                                        onClick = { onItemClick(item.tmdbId.toInt(), "tv") },
                                    )
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
private fun SyncStatusChip(
    isSyncing: Boolean,
    pendingCount: Int,
    lastSyncError: String?,
    onTap: () -> Unit,
) {
    val label = when {
        isSyncing -> "Syncing..."
        lastSyncError != null && pendingCount > 0 -> "Sync failed - $pendingCount pending"
        lastSyncError != null -> "Sync failed"
        pendingCount > 0 -> "$pendingCount pending"
        else -> "Synced"
    }
    val color = when {
        isSyncing -> MaterialTheme.colorScheme.primary
        lastSyncError != null -> MaterialTheme.colorScheme.error
        pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun SectionHeader(title: String, showAll: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (showAll) "Less" else "All",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (showAll) "Less" else "All",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WatchedPoster(item: com.tally.app.ui.profile.WatchedItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(item.posterUrl).crossfade(true).build(),
            contentDescription = item.title,
            modifier = Modifier
                .width(80.dp)
                .height(120.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TextButtonItem(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(text) }
}
