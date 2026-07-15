package com.tally.app.ui.importer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiberatorImportScreen(
    onBack: () -> Unit,
    viewModel: LiberatorImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from TV Time") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                is LiberatorImportUiState.Idle -> {
                    Text(
                        "Select your TV Time Liberator zip file containing shows.json and/or movies.json",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { filePicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }) {
                        Text("Select zip file")
                    }
                }

                is LiberatorImportUiState.ReadingFile -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Reading file...")
                }

                is LiberatorImportUiState.Matching -> {
                    CircularProgressIndicator(
                        progress = { state.progress.fraction },
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("${state.progress.phase}: ${state.progress.current}/${state.progress.total}")
                }

                is LiberatorImportUiState.Applying -> {
                    CircularProgressIndicator(
                        progress = { state.progress.fraction },
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Importing: ${state.progress.current}/${state.progress.total}")
                }

                is LiberatorImportUiState.Done -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Text("Import complete", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            Text("Shows: ${state.summary.showsImported}")
                            Text("Episodes: ${state.summary.episodesImported}")
                            Text("Movies: ${state.summary.moviesImported}")
                        }
                        if (state.summary.showsSkipped.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Shows skipped (${state.summary.showsSkipped.size}):", color = MaterialTheme.colorScheme.error)
                            }
                            items(state.summary.showsSkipped) { title ->
                                Text("  $title", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (state.summary.moviesSkipped.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("Movies skipped (${state.summary.moviesSkipped.size}):", color = MaterialTheme.colorScheme.error)
                            }
                            items(state.summary.moviesSkipped.take(20)) { title ->
                                Text("  $title", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (state.summary.moviesSkipped.size > 20) {
                                item {
                                    Text("  ...and ${state.summary.moviesSkipped.size - 20} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(onClick = {
                                viewModel.reset()
                                onBack()
                            }) { Text("Done") }
                        }
                    }
                }

                is LiberatorImportUiState.Error -> {
                    Text("Import failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = { viewModel.reset() }) { Text("Try again") }
                }
            }
        }
    }
}
