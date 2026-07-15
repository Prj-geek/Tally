package com.tally.app.ui.importer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.importer.ImportProgress
import com.tally.app.data.importer.ImportSummary
import com.tally.app.data.importer.LiberatorImportRepository
import com.tally.app.data.importer.LiberatorMovie
import com.tally.app.data.importer.LiberatorShow
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject

sealed interface LiberatorImportUiState {
    data object Idle : LiberatorImportUiState
    data object ReadingFile : LiberatorImportUiState
    data class Matching(val progress: ImportProgress) : LiberatorImportUiState
    data class Applying(val progress: ImportProgress) : LiberatorImportUiState
    data class Done(val summary: ImportSummary) : LiberatorImportUiState
    data class Error(val message: String) : LiberatorImportUiState
}

@HiltViewModel
class LiberatorImportViewModel @Inject constructor(
    private val application: Application,
    private val importRepository: LiberatorImportRepository,
    private val authRepository: AuthRepository,
    private val json: Json,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiberatorImportUiState>(LiberatorImportUiState.Idle)
    val uiState: StateFlow<LiberatorImportUiState> = _uiState.asStateFlow()

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val status = authRepository.sessionStatus.first { it !is SessionStatus.Initializing }
            val uid = if (status is SessionStatus.Authenticated) status.session.user?.id else null
            if (uid == null) {
                _uiState.value = LiberatorImportUiState.Error("Not signed in")
                return@launch
            }

            _uiState.value = LiberatorImportUiState.ReadingFile

            try {
                val (shows, movies) = withContext(Dispatchers.IO) {
                    readZip(uri)
                }

                if (shows.isEmpty() && movies.isEmpty()) {
                    _uiState.value = LiberatorImportUiState.Error("No shows.json or movies.json found in zip")
                    return@launch
                }

                _uiState.value = LiberatorImportUiState.Matching(
                    ImportProgress("Matching", 0, shows.size + movies.size),
                )

                val prepared = importRepository.prepareImport(
                    shows = shows,
                    movies = movies,
                    onProgress = { progress ->
                        _uiState.update { LiberatorImportUiState.Matching(progress) }
                    },
                )

                _uiState.value = LiberatorImportUiState.Applying(
                    ImportProgress("Importing", 0, prepared.shows.size + prepared.episodes.size + prepared.movies.size),
                )

                val summary = importRepository.applyImport(
                    userId = uid,
                    prepared = prepared,
                    onProgress = { progress ->
                        _uiState.update { LiberatorImportUiState.Applying(progress) }
                    },
                )

                _uiState.value = LiberatorImportUiState.Done(summary)
            } catch (e: Exception) {
                _uiState.value = LiberatorImportUiState.Error(e.message ?: "Import failed")
            }
        }
    }

    private fun readZip(uri: Uri): Pair<List<LiberatorShow>, List<LiberatorMovie>> {
        var shows = listOf<LiberatorShow>()
        var movies = listOf<LiberatorMovie>()

        application.contentResolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/')
                    when {
                        name.equals("shows.json", ignoreCase = true) -> {
                            val text = BufferedReader(InputStreamReader(zip)).readText()
                            shows = json.decodeFromString(text)
                        }
                        name.equals("movies.json", ignoreCase = true) -> {
                            val text = BufferedReader(InputStreamReader(zip)).readText()
                            movies = json.decodeFromString(text)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        return shows to movies
    }

    fun reset() {
        _uiState.value = LiberatorImportUiState.Idle
    }
}
