package com.cussou.parallax.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cussou.parallax.ParallaxApp
import com.cussou.parallax.data.MatchRepository
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.duplicate
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The list of matches: create, edit, duplicate, delete (F-60). */
class MatchListViewModel(
    private val matchRepository: MatchRepository,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    /** Null until the matches are loaded. */
    val matches: StateFlow<List<Match>?> =
        matchRepository.matches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
