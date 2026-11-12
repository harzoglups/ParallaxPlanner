package com.cussou.parallax.data

import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The text is not a match file this version of the app can read. */
open class MatchFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The file was written by a later version of the app, with a format this one does not know. */
class UnsupportedMatchVersionException(version: Int) : MatchFormatException("Unsupported match file version $version")

/**
 * The match file format of docs/04-architecture.md. Local storage and export use the same one,
 * and it is platform-independent so that the V2 web version reads the same files (F-65).
 */
object MatchJson {
    const val SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        // A file written by a later version may carry fields this one does not know.
        ignoreUnknownKeys = true
    }

    fun encode(match: Match): String = json.encodeToString(MatchFileDto.serializer(), match.toFile())

    /** @throws MatchFormatException if [text] is not a valid match file. */
    fun decode(text: String): Match {
        val file = try {
            json.decodeFromString(MatchFileDto.serializer(), text)
        } catch (e: SerializationException) {
            throw MatchFormatException("Not a match file", e)
        } catch (e: IllegalArgumentException) {
            throw MatchFormatException("Not a match file", e)
        }
        if (file.schemaVersion > SCHEMA_VERSION) {
            throw UnsupportedMatchVersionException(file.schemaVersion)
        }
        return file.match.toDomain()
    }

    private fun Match.toFile() = MatchFileDto(
        schemaVersion = SCHEMA_VERSION,
        match = MatchDto(
            id = id,
            name = name,
            date = date,
            notes = notes,
            stages = stages.mapIndexed { index, stage ->
                StageDto(
                    id = stage.id,
                    name = stage.name,
                    order = index + 1,
                    notes = stage.notes,
                    rangeMinM = stage.rangeMinM,
                    rangeMaxM = stage.rangeMaxM,
                    forcedSettingM = stage.forcedSettingM,
                    targets = stage.targets.map { target ->
                        TargetDto(
                            label = target.label,
                            distanceM = target.distanceM,
                            size = target.size?.let { SizeDto(it.value, SIZE_UNIT_TOKENS.getValue(it.unit)) },
                        )
                    },
                )
            },
        ),
    )

    private fun MatchDto.toDomain() = Match(
        id = id,
        name = name,
        date = date?.also(::requireIsoDate),
        notes = notes,
        stages = stages.sortedBy { it.order }.map { stage ->
            Stage(
                id = stage.id,
                name = stage.name,
                notes = stage.notes,
                rangeMinM = stage.rangeMinM?.also { requirePositive(it, "rangeMinM") },
                rangeMaxM = stage.rangeMaxM?.also { requirePositive(it, "rangeMaxM") },
                forcedSettingM = stage.forcedSettingM?.also { requirePositive(it, "forcedSettingM") },
                targets = stage.targets.map { target ->
                    Target(
                        distanceM = target.distanceM.also { requirePositive(it, "distanceM") },
                        size = target.size?.let { size ->
                            val unit = SIZE_UNIT_TOKENS.fromToken(size.unit)
                                ?: throw MatchFormatException("Unknown size unit: ${size.unit}")
                            TargetSize(size.value.also { requirePositive(it, "size") }, unit)
                        },
                        label = target.label,
                    )
                },
            )
        },
    )

    private fun requirePositive(value: Double, field: String) {
        if (!(value > 0.0) || value.isInfinite()) throw MatchFormatException("Invalid $field: $value")
    }

    private fun requireIsoDate(date: String) {
        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw MatchFormatException("Invalid date: $date", e)
        }
    }
}

// The file layout. Fields with a default may be missing from a file; the others are required.

@Serializable
private data class MatchFileDto(val schemaVersion: Int, val match: MatchDto)

@Serializable
private data class MatchDto(
    val id: String,
    val name: String,
    val date: String? = null,
    val notes: String = "",
    val stages: List<StageDto>,
)

@Serializable
private data class StageDto(
    val id: String,
    val name: String,
    /** 1-based position in the match. */
    val order: Int,
    val notes: String = "",
    val rangeMinM: Double? = null,
    val rangeMaxM: Double? = null,
    val forcedSettingM: Double? = null,
    val targets: List<TargetDto> = emptyList(),
)

@Serializable
private data class TargetDto(val label: String = "", val distanceM: Double, val size: SizeDto? = null)

@Serializable
private data class SizeDto(val value: Double, val unit: String)
