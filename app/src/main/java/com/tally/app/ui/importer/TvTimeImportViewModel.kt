package com.tally.app.ui.importer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.app.data.auth.AuthRepository
import com.tally.app.data.importer.ImportProgress
import com.tally.app.data.importer.ImportSummary
import com.tally.app.data.importer.TvTimeImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream
import javax.inject.Inject

sealed interface TvTimeImportUiState {
    data object Idle : TvTimeImportUiState
    data object ReadingFile : TvTimeImportUiState
    data object Parsing : TvTimeImportUiState
    data class Matching(val processed: Int, val total: Int, val currentTitle: String) : TvTimeImportUiState
    data class Done(val summary: ImportSummary) : TvTimeImportUiState
    data class Error(val message: String) : TvTimeImportUiState
}

private const val EPISODES_CSV_NAME = "tracking-prod-records-v2.csv"
private const val MOVIES_CSV_NAME = "tracking-prod-records.csv"

@HiltViewModel
class TvTimeImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val importRepository: TvTimeImportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TvTimeImportUiState>(TvTimeImportUiState.Idle)
    val state: StateFlow<TvTimeImportUiState> = _state.asStateFlow()

    /** Pass the whole gdpr-data.zip the user downloaded from gdpr.tvtime.com. */
    fun onZipPicked(uri: Uri) {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _state.value = TvTimeImportUiState.Error("You need to be signed in to import.")
            return
        }

        viewModelScope.launch {
            _state.value = TvTimeImportUiState.ReadingFile

            val (episodesCsv, moviesCsv) = try {
                withContext(Dispatchers.IO) { extractCsvFiles(uri) }
            } catch (e: Exception) {
                null to null
            }

            if (episodesCsv == null || moviesCsv == null) {
                val missing = buildList {
                    if (episodesCsv == null) add(EPISODES_CSV_NAME)
                    if (moviesCsv == null) add(MOVIES_CSV_NAME)
                }.joinToString(", ")
                _state.value = TvTimeImportUiState.Error(
                    "Couldn't find $missing inside that zip. Make sure you picked the " +
                        "full gdpr-data.zip you downloaded from gdpr.tvtime.com, unmodified.",
                )
                return@launch
            }

            try {
                val summary = importRepository.import(userId, episodesCsv, moviesCsv) { progress ->
                    _state.value = when (progress) {
                        is ImportProgress.Parsing -> TvTimeImportUiState.Parsing
                        is ImportProgress.Matching -> TvTimeImportUiState.Matching(
                            progress.processed,
                            progress.total,
                            progress.currentTitle,
                        )
                        is ImportProgress.Done -> TvTimeImportUiState.Done(progress.summary)
                        is ImportProgress.Failed -> TvTimeImportUiState.Error(progress.message)
                    }
                }
                _state.value = TvTimeImportUiState.Done(summary)
            } catch (e: Exception) {
                _state.value = TvTimeImportUiState.Error(e.message ?: "Import failed unexpectedly.")
            }
        }
    }

    /** Reads the zip once, pulling out just the two CSVs we need by name. */
    private fun extractCsvFiles(uri: Uri): Pair<String?, String?> {
        var episodesCsv: String? = null
        var moviesCsv: String? = null

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/')
                    when (name) {
                        EPISODES_CSV_NAME -> episodesCsv = zip.bufferedReader().readText()
                        MOVIES_CSV_NAME -> moviesCsv = zip.bufferedReader().readText()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return episodesCsv to moviesCsv
    }

    fun reset() {
        _state.value = TvTimeImportUiState.Idle
    }
}
