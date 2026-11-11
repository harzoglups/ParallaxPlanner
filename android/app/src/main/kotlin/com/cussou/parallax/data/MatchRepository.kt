package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The user's matches, stored locally only (NF-04): one JSON file per match in [directory],
 * in the format of the export (docs/04-architecture.md). All of them are kept in memory:
 * a shooter has a handful of matches, each a few kilobytes.
 */
class MatchRepository(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    /** Null until the files have been read. */
    private val loaded = MutableStateFlow<List<Match>?>(null)

    /** Every match: dated ones first, most recent first, then the others by name. */
    val matches: Flow<List<Match>> = flow {
        ensureLoaded()
        emitAll(loaded.filterNotNull())
    }

    suspend fun get(id: String): Match? = ensureLoaded().firstOrNull { it.id == id }

    /** Creates the match, or replaces the one with the same id. */
    suspend fun save(match: Match) = change { current ->
        withContext(ioDispatcher) { write(match) }
        current.filter { it.id != match.id } + match
    }

    suspend fun delete(id: String) = change { current ->
        withContext(ioDispatcher) { fileFor(id).delete() }
        current.filter { it.id != id }
    }

    private suspend fun change(transform: suspend (List<Match>) -> List<Match>) {
        ensureLoaded()
        mutex.withLock { loaded.value = transform(loaded.value.orEmpty()).sortedWith(LIST_ORDER) }
    }

    private suspend fun ensureLoaded(): List<Match> = mutex.withLock {
        loaded.value ?: withContext(ioDispatcher) { readAll() }.sortedWith(LIST_ORDER).also { loaded.value = it }
    }

    private fun readAll(): List<Match> =
        directory.listFiles { file -> file.extension == EXTENSION }.orEmpty().mapNotNull { file ->
            // A file that cannot be read is skipped and left in place: it must not hide the others.
            try {
                MatchJson.decode(file.readText())
            } catch (e: MatchFormatException) {
                null
            }
        }

    /** Written to a temporary file first, so a crash never leaves a half-written match. */
    private fun write(match: Match) {
        directory.mkdirs()
        val target = fileFor(match.id)
        val temporary = File(directory, "${target.name}.tmp")
        temporary.writeText(MatchJson.encode(match))
        if (!temporary.renameTo(target)) {
            target.delete()
            check(temporary.renameTo(target)) { "Cannot write ${target.name}" }
        }
    }

    /** Ids become file names: anything outside letters, digits, '-' and '_' is replaced. */
    private fun fileFor(id: String) = File(directory, "${id.replace(UNSAFE_IN_FILE_NAME, "_")}.$EXTENSION")

    private companion object {
        const val EXTENSION = "json"
        val UNSAFE_IN_FILE_NAME = Regex("[^A-Za-z0-9_-]")

        val LIST_ORDER: Comparator<Match> =
            compareBy<Match> { it.date == null }
                .thenByDescending { it.date.orEmpty() }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
