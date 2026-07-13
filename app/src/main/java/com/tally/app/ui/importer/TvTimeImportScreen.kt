package com.tally.app.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import com.tally.app.data.importer.ImportSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvTimeImportScreen(
    onBack: () -> Unit,
    viewModel: TvTimeImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let(viewModel::onZipPicked) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Import from TV Time") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                is TvTimeImportUiState.Idle -> {
                    Text(
                        "Pick the gdpr-data.zip file you downloaded from gdpr.tvtime.com. " +
                            "Don't unzip it — Tally reads both the episode and movie " +
                            "history straight out of the zip.",
                    )
                    Button(
                        onClick = { filePicker.launch("application/zip") },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Choose gdpr-data.zip")
                    }
                }

                is TvTimeImportUiState.ReadingFile -> {
                    CircularProgressIndicator()
                    Text("Reading file...", modifier = Modifier.padding(top = 16.dp))
                }

                is TvTimeImportUiState.Parsing -> {
                    CircularProgressIndicator()
                    Text("Parsing your watch history...", modifier = Modifier.padding(top = 16.dp))
                }

                is TvTimeImportUiState.Matching -> {
                    val progress = if (s.total > 0) s.processed / s.total.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text("Matching \"${s.currentTitle}\" against TMDB…")
                    Text("${s.processed} / ${s.total}")
                }

                is TvTimeImportUiState.Done -> {
                    ImportSummaryView(s.summary, onDone = onBack, onReset = viewModel::reset)
                }

                is TvTimeImportUiState.Error -> {
                    Text("Something went wrong: ${s.message}")
                    Button(onClick = viewModel::reset, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSummaryView(
    summary: ImportSummary,
    onDone: () -> Unit,
    onReset: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Import complete")
        Text("Shows matched: ${summary.showsMatched}")
        Text("Episodes imported: ${summary.episodesImported}")
        Text("Movies matched: ${summary.moviesMatched}")
        if (summary.watchlistedShowsMatched > 0 || summary.watchlistedMoviesMatched > 0) {
            Text("Watchlisted shows added: ${summary.watchlistedShowsMatched}")
            Text("Watchlisted movies added: ${summary.watchlistedMoviesMatched}")
        }

        val allUnmatched = summary.showsUnmatched + summary.moviesUnmatched +
            summary.watchlistedShowsUnmatched + summary.watchlistedMoviesUnmatched
        if (allUnmatched.isNotEmpty()) {
            Text(
                "${allUnmatched.size} title(s) couldn't be matched automatically — " +
                    "you can add these manually via Search:",
                modifier = Modifier.padding(top = 16.dp),
            )
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(allUnmatched) { item ->
                    Text("• ${item.title}${if (item.isShow) " (show)" else " (movie)"}")
                }
            }
        }

        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) {
            Text("Done")
        }
    }
}
