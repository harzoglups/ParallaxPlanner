package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.duplicate

sealed interface ImportResult {
    data class Imported(val match: Match) : ImportResult

    data object NotAMatch : ImportResult

    /** Written by a later version of the app: updating the app will read it. */
    data object TooNew : ImportResult
}

/**
 * Sharing a match between teammates or devices as a JSON file (F-65), in the same format as
 * local storage and the V2 web version (docs/04-architecture.md).
 */
object MatchTransfer {
    fun export(match: Match): String = MatchJson.encode(match)

    /**
     * Reads a shared file. The match gets new ids, so importing never replaces a match already
     * there, even the one it was exported from: it is added next to it.
     */
    fun import(text: String, newId: () -> String): ImportResult = try {
        val match = MatchJson.decode(text)
        ImportResult.Imported(match.duplicate(match.name, newId))
    } catch (e: UnsupportedMatchVersionException) {
        ImportResult.TooNew
    } catch (e: MatchFormatException) {
        ImportResult.NotAMatch
    }

    /** "Rimfire Cup #4" -> "Rimfire Cup 4.json": letters, digits, spaces, '-' and '_' only. */
    fun fileName(match: Match): String {
        val base = match.name
            .map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .take(MAX_FILE_NAME_LENGTH)
            .ifEmpty { "match" }
        return "$base.json"
    }

    private const val MAX_FILE_NAME_LENGTH = 60
}
