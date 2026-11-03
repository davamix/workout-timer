package io.github.davamix.workouttimer.data

import io.github.davamix.workouttimer.core.model.Workout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class WorkoutFileFormatTest {
    private fun roundTrip(workout: Workout) = WorkoutFileFormat.decode(WorkoutFileFormat.encode(workout))

    private fun encodedObject(workout: Workout): JsonObject = Json.parseToJsonElement(WorkoutFileFormat.encode(workout)).jsonObject

    @Test
    fun `the 4_4 example survives a round trip`() {
        assertEquals(exampleWorkout, roundTrip(exampleWorkout))
    }

    @Test
    fun `a one-exercise workout survives a round trip`() {
        assertEquals(singleExerciseWorkout, roundTrip(singleExerciseWorkout))
    }

    @Test
    fun `unset values stay unset and own values stay set, even when equal to the default`() {
        val workout =
            workout(
                round(
                    "R",
                    exercise("Linked"),
                    exercise("Own", duration = 40, rest = 15),
                    rest = 60,
                ),
                round("Last", exercise("End")),
                defaultDuration = 40,
                defaultExerciseRest = 15,
                defaultRoundRest = 60,
            )

        val loaded = roundTrip(workout)

        val (linked, own) = loaded.rounds[0].exercises
        assertNull(linked.durationSeconds)
        assertNull(linked.restSeconds)
        assertEquals(40, own.durationSeconds)
        assertEquals(15, own.restSeconds)
        assertEquals(60, loaded.rounds[0].restSeconds)
        assertNull(loaded.rounds[1].restSeconds)
        assertEquals(workout, loaded)
    }

    @Test
    fun `unused values are kept`() {
        // The last exercise of a round and the last round have rests that never run (§4.2), but they're stored.
        val workout =
            workout(
                round("First", exercise("A"), exercise("Last of first", rest = 99)),
                round("Last", exercise("B", rest = 7), exercise("Last of all", rest = 98), rest = 77),
            )

        val loaded = roundTrip(workout)

        assertEquals(99, loaded.rounds[0].exercises[1].restSeconds)
        assertEquals(98, loaded.rounds[1].exercises[1].restSeconds)
        assertEquals(77, loaded.rounds[1].restSeconds)
        assertEquals(workout, loaded)
    }

    @Test
    fun `names with special characters survive a round trip`() {
        val workout =
            workout(
                round(
                    "Ronda \"uno\" \\ con\ttab",
                    exercise("Sentadillas búlgaras", id = "e1"),
                    exercise("💪 Flexiones", id = "e2"),
                    id = "r1",
                ),
                name = "Línea\nnueva / ñ · 日本語",
                id = "w1",
            )

        assertEquals(workout, roundTrip(workout))
    }

    @Test
    fun `a fixed version 1 document decodes to the expected workout`() {
        assertEquals(exampleWorkout, WorkoutFileFormat.decode(GOLDEN_V1))
    }

    @Test
    fun `encoding gives the fixed version 1 document`() {
        assertEquals(Json.parseToJsonElement(GOLDEN_V1), Json.parseToJsonElement(WorkoutFileFormat.encode(exampleWorkout)))
    }

    @Test
    fun `encoding writes schemaVersion 1 first`() {
        val document = encodedObject(singleExerciseWorkout)

        assertEquals("schemaVersion", document.keys.first())
        assertEquals("1", document["schemaVersion"].toString())
        assertEquals(1, WorkoutFileFormat.SCHEMA_VERSION)
    }

    @Test
    fun `encoding leaves out unset values`() {
        val round = encodedObject(singleExerciseWorkout)["rounds"]!!.jsonArray.single().jsonObject
        val exercise = round["exercises"]!!.jsonArray.single().jsonObject

        assertEquals(setOf("id", "name", "exercises"), round.keys)
        assertEquals(setOf("id", "name"), exercise.keys)
    }

    @Test
    fun `an explicit null reads as an unset value`() {
        val text =
            VALID_MINIMAL
                .replace(""""name": "R"""", """"name": "R", "restSeconds": null""")
                .replace(""""durationSeconds": 30""", """"durationSeconds": null, "restSeconds": null""")

        val loaded = WorkoutFileFormat.decode(text)

        assertNull(loaded.rounds.single().restSeconds)
        assertNull(
            loaded.rounds
                .single()
                .exercises
                .single()
                .durationSeconds,
        )
        assertNull(
            loaded.rounds
                .single()
                .exercises
                .single()
                .restSeconds,
        )
    }

    @Test
    fun `the minimal document used for the damaged cases is valid`() {
        val loaded = WorkoutFileFormat.decode(VALID_MINIMAL)

        assertEquals("w1", loaded.id)
        assertEquals(
            30,
            loaded.rounds
                .single()
                .exercises
                .single()
                .durationSeconds,
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("damagedDocuments")
    fun `a damaged document is rejected with its reason`(
        case: String,
        text: String,
        reason: String,
    ) {
        val error = assertThrows<WorkoutFormatException> { WorkoutFileFormat.decode(text) }

        assertTrue(error.message!!.startsWith(reason), "$case: expected \"$reason…\", got \"${error.message}\"")
    }

    @Test
    fun `an unsupported version is reported as such, whatever the rest of the document`() {
        val error = assertThrows<WorkoutFormatException> { WorkoutFileFormat.decode("""{"schemaVersion": 2, "x": []}""") }

        assertEquals("Unsupported schemaVersion 2", error.message)
    }

    companion object {
        /** The §4.4 example ([exampleWorkout]) in format version 1. If this test has to change, the format changed. */
        private val GOLDEN_V1 =
            """
            {
                "schemaVersion": 1,
                "id": "w-example",
                "name": "Example",
                "defaultExerciseDurationSeconds": 40,
                "defaultExerciseRestSeconds": 15,
                "defaultRoundRestSeconds": 60,
                "rounds": [
                    {
                        "id": "r-warm-up",
                        "name": "Warm-up",
                        "restSeconds": 30,
                        "exercises": [
                            { "id": "e-jumping-jacks", "name": "Jumping jacks", "durationSeconds": 30 },
                            { "id": "e-arm-circles", "name": "Arm circles", "durationSeconds": 10, "restSeconds": 5 },
                            { "id": "e-high-knees", "name": "High knees" }
                        ]
                    },
                    {
                        "id": "r-core",
                        "name": "Core",
                        "exercises": [
                            { "id": "e-plank", "name": "Plank", "durationSeconds": 60, "restSeconds": 30 },
                            { "id": "e-crunches", "name": "Crunches" }
                        ]
                    }
                ]
            }
            """.trimIndent()

        /** A small valid document. Each damaged case below changes one thing in it. */
        private const val VALID_MINIMAL =
            """{"schemaVersion": 1, "id": "w1", "name": "Legs", "defaultExerciseDurationSeconds": 40, """ +
                """"defaultExerciseRestSeconds": 15, "defaultRoundRestSeconds": 60, "rounds": [{"id": "r1", "name": "R", """ +
                """"exercises": [{"id": "e1", "name": "Squats", "durationSeconds": 30}]}]}"""

        private fun damaged(
            from: String,
            to: String,
        ): String {
            require(from in VALID_MINIMAL) { "'$from' isn't in the minimal document" }
            return VALID_MINIMAL.replace(from, to)
        }

        private const val NOT_JSON = "Not valid JSON"
        private const val NOT_OBJECT = "Not a JSON object"
        private const val NOT_INTEGER = "schemaVersion isn't an integer"
        private const val NOT_V1 = "Not a valid version 1 workout"

        /**
         * Every damaged case of SPEC.md §4.3 that the format can see, with the start of the expected reason. A wrong
         * `id` is the repository's check.
         */
        @JvmStatic
        fun damagedDocuments(): List<Arguments> =
            listOf(
                Arguments.of("empty file", "", NOT_JSON),
                Arguments.of("truncated", VALID_MINIMAL.take(VALID_MINIMAL.length / 2), NOT_JSON),
                Arguments.of("JSON array", "[]", NOT_OBJECT),
                Arguments.of("JSON null", "null", NOT_OBJECT),
                Arguments.of("schemaVersion missing", damaged(""""schemaVersion": 1, """, ""), "schemaVersion is missing"),
                Arguments.of("schemaVersion as text", damaged(""""schemaVersion": 1""", """"schemaVersion": "1""""), NOT_INTEGER),
                Arguments.of("schemaVersion not whole", damaged(""""schemaVersion": 1""", """"schemaVersion": 1.5"""), NOT_INTEGER),
                Arguments.of("schemaVersion null", damaged(""""schemaVersion": 1""", """"schemaVersion": null"""), NOT_INTEGER),
                Arguments.of(
                    "newer schemaVersion",
                    damaged(""""schemaVersion": 1""", """"schemaVersion": 2"""),
                    "Unsupported schemaVersion 2",
                ),
                Arguments.of("name missing", damaged(""""name": "Legs", """, ""), NOT_V1),
                Arguments.of("name null", damaged(""""name": "Legs"""", """"name": null"""), NOT_V1),
                Arguments.of("name of the wrong type", damaged(""""name": "Legs"""", """"name": 3"""), NOT_V1),
                Arguments.of("default missing", damaged(""""defaultRoundRestSeconds": 60, """, ""), NOT_V1),
                Arguments.of("rounds not a list", VALID_MINIMAL.substringBefore(""""rounds"""") + """"rounds": {}}""", NOT_V1),
                Arguments.of(
                    "exercises missing",
                    damaged(""", "exercises": [{"id": "e1", "name": "Squats", "durationSeconds": 30}]""", ""),
                    NOT_V1,
                ),
                Arguments.of(
                    "duration too big for an Int",
                    damaged(""""durationSeconds": 30""", """"durationSeconds": 3000000000"""),
                    NOT_V1,
                ),
                Arguments.of("duration in exponent form", damaged(""""durationSeconds": 30""", """"durationSeconds": 1e12"""), NOT_V1),
                Arguments.of("duration not whole", damaged(""""durationSeconds": 30""", """"durationSeconds": 2.5"""), NOT_V1),
                Arguments.of("unknown field", damaged(""""id": "w1"""", """"id": "w1", "colour": "red""""), NOT_V1),
                Arguments.of("unknown field in an exercise", damaged(""""name": "Squats"""", """"name": "Squats", "reps": 10"""), NOT_V1),
            )
    }
}
