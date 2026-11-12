package com.cussou.parallax.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.ImportResult
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.data.MatchTransfer
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.duplicate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What to tell the user once a file has been read (F-65). */
sealed interface ImportMessage {
    data class Imported(val matchName: String) : ImportMessage

    data object NotAMatch : ImportMessage

    data object TooNew : ImportMessage

    data object Unreadable : ImportMessage
}

/** The list of matches: create, edit, duplicate, delete (F-60), import (F-65). */
class MatchListViewModel(
    private val matchRepository: MatchRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    /** Null until the matches are loaded. */
    val matches: StateFlow<List<Match>?> =
        matchRepository.matches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _importMessage = MutableStateFlow<ImportMessage?>(null)

    /** The result of the last import, until the screen has shown it. */
    val importMessage: StateFlow<ImportMessage?> = _importMessage.asStateFlow()

    /** [text] is the content of the file the user picked. */
    fun onImport(text: String) {
        when (val result = MatchTransfer.import(text, newId)) {
            is ImportResult.Imported -> {
                save(result.match)
                _importMessage.value = ImportMessage.Imported(result.match.name)
            }
            ImportResult.NotAMatch -> _importMessage.value = ImportMessage.NotAMatch
            ImportResult.TooNew -> _importMessage.value = ImportMessage.TooNew
        }
    }

    /** The file could not be read at all (missing, too large, no permission). */
    fun onImportFailed() {
        _importMessage.value = ImportMessage.Unreadable
    }

    fun onImportMessageShown() {
        _importMessage.value = null
    }

    fun onMatchCreated(name: String, date: String?, notes: String) = save(
        Match(id = newId(), name = name.trim(), date = date, notes = notes.trim()),
    )

    fun onMatchEdited(match: Match, name: String, date: String?, notes: String) = save(
        match.copy(name = name.trim(), date = date, notes = notes.trim()),
    )

    /** [name] is the name of the copy; it comes from the screen because it is translated. */
    fun onMatchDuplicated(match: Match, name: String) = save(match.duplicate(name, newId))

    fun onMatchDeleted(match: Match) {
        viewModelScope.launch { matchRepository.delete(match.id) }
    }

    /** Undo of a deletion (F-66): the match comes back as it was. */
    fun onMatchRestored(match: Match) = save(match)

    private fun save(match: Match) {
        viewModelScope.launch { matchRepository.save(match) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MatchListViewModel((this[APPLICATION_KEY] as ParallaxApp).matchRepository) }
        }
    }
}
