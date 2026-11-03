package io.github.davamix.workouttimer.data

import io.github.davamix.workouttimer.core.model.Exercise
import io.github.davamix.workouttimer.core.model.Round
import io.github.davamix.workouttimer.core.model.Workout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * The JSON format of a stored workout (plan D-4): one document per workout, versioned by `schemaVersion`.
 *
 * Version 1 mirrors the P2 model field by field, through its own classes, so renaming a model property can't
 * silently change the files. Optional values that aren't set are left out, so they stay linked to the workout
 * defaults after loading (specification §4.2). Any change to the format needs a new version: [decode] reads
 * `schemaVersion` first and decodes the rest with the classes for that version.
 */
internal object WorkoutFileFormat {
    /** The version [encode] writes, and the only one [decode] reads. */
    const val SCHEMA_VERSION = 1

    private val json =
        Json {
            // Readable with adb (D-4).
            prettyPrint = true
            // Unset optional values are left out; an explicit null still reads as unset.
            explicitNulls = false
        }

    /** The workout as a version 1 document. Every value is kept, including unused ones. */
    fun encode(workout: Workout): String = json.encodeToString(WorkoutFileV1.serializer(), workout.toFile())

    /**
     * Reads a document written by [encode]. Reading is strict: unknown fields, wrong types and numbers that don't fit
     * in an `Int` are all rejected.
     *
     * @throws WorkoutFormatException if [text] isn't a valid version 1 document. The message gives the reason.
     */
    fun decode(text: String): Workout {
        val document =
            try {
                json.parseToJsonElement(text)
            } catch (e: IllegalArgumentException) {
                throw WorkoutFormatException("Not valid JSON", e)
            }
        if (document !is JsonObject) throw WorkoutFormatException("Not a JSON object")

        val version = document["schemaVersion"] ?: throw WorkoutFormatException("schemaVersion is missing")
        val versionNumber =
            (version as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                ?: throw WorkoutFormatException("schemaVersion isn't an integer: $version")

        return when (versionNumber) {
            1 -> decodeV1(document)
            else -> throw WorkoutFormatException("Unsupported schemaVersion $versionNumber")
        }
    }

    private fun decodeV1(document: JsonObject): Workout =
        try {
            json.decodeFromJsonElement(WorkoutFileV1.serializer(), document).toModel()
        } catch (e: IllegalArgumentException) {
            // SerializationException and NumberFormatException are both IllegalArgumentExceptions.
            throw WorkoutFormatException("Not a valid version 1 workout", e)
        }
}

/** A stored workout that can't be read: the reason is in [message], and the parser's error, if any, in [cause]. */
internal class WorkoutFormatException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

// Version 1 classes. Property order is the order in the file, so schemaVersion comes first.

@Serializable
private data class WorkoutFileV1(
    val schemaVersion: Int,
    val id: String,
    val name: String,
    val defaultExerciseDurationSeconds: Int,
    val defaultExerciseRestSeconds: Int,
    val defaultRoundRestSeconds: Int,
    val rounds: List<RoundFileV1>,
)

@Serializable
private data class RoundFileV1(
    val id: String,
    val name: String,
    val restSeconds: Int? = null,
    val exercises: List<ExerciseFileV1>,
)

@Serializable
private data class ExerciseFileV1(
    val id: String,
    val name: String,
    val durationSeconds: Int? = null,
    val restSeconds: Int? = null,
)

private fun Workout.toFile() =
    WorkoutFileV1(
        schemaVersion = WorkoutFileFormat.SCHEMA_VERSION,
        id = id,
        name = name,
        defaultExerciseDurationSeconds = defaultExerciseDurationSeconds,
        defaultExerciseRestSeconds = defaultExerciseRestSeconds,
        defaultRoundRestSeconds = defaultRoundRestSeconds,
        rounds = rounds.map { it.toFile() },
    )

private fun Round.toFile() =
    RoundFileV1(
        id = id,
        name = name,
        restSeconds = restSeconds,
        exercises = exercises.map { it.toFile() },
    )

private fun Exercise.toFile() =
    ExerciseFileV1(
        id = id,
        name = name,
        durationSeconds = durationSeconds,
        restSeconds = restSeconds,
    )

private fun WorkoutFileV1.toModel() =
    Workout(
        id = id,
        name = name,
        defaultExerciseDurationSeconds = defaultExerciseDurationSeconds,
        defaultExerciseRestSeconds = defaultExerciseRestSeconds,
        defaultRoundRestSeconds = defaultRoundRestSeconds,
        rounds = rounds.map { it.toModel() },
    )

private fun RoundFileV1.toModel() =
    Round(
        id = id,
        name = name,
        restSeconds = restSeconds,
        exercises = exercises.map { it.toModel() },
    )

private fun ExerciseFileV1.toModel() =
    Exercise(
        id = id,
        name = name,
        durationSeconds = durationSeconds,
        restSeconds = restSeconds,
    )
