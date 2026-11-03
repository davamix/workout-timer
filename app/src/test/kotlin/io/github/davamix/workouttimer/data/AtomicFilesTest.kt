package io.github.davamix.workouttimer.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class AtomicFilesTest {
    @TempDir
    lateinit var dir: File

    private val target by lazy { File(dir, "w1.json") }

    private fun writeText(text: String) = writeAtomically(target) { it.write(text.toByteArray()) }

    @Test
    fun `creates a new file`() {
        writeText("hello")

        assertEquals("hello", target.readText())
        assertFalse(tempFileFor(target).exists())
    }

    @Test
    fun `fully replaces an existing file, even with shorter content`() {
        writeText("a much longer first version")

        writeText("short")

        assertEquals("short", target.readText())
        assertFalse(tempFileFor(target).exists())
    }

    @Test
    fun `writes to a temporary file next to the target, then moves it`() {
        writeText("old")
        var tempSeen = false

        writeAtomically(target) {
            tempSeen = tempFileFor(target).exists()
            assertEquals("old", target.readText(), "The target is untouched while writing")
            it.write("new".toByteArray())
        }

        assertTrue(tempSeen)
        assertEquals(File(dir, "w1.json.tmp"), tempFileFor(target))
        assertEquals("new", target.readText())
    }

    @Test
    fun `a failure while writing leaves the old file unchanged and no temporary file`() {
        writeText("old")

        assertThrows<IOException> {
            writeAtomically(target) {
                it.write("partial".toByteArray())
                throw IOException("disk full")
            }
        }

        assertEquals("old", target.readText())
        assertFalse(tempFileFor(target).exists())
    }

    @Test
    fun `a failure while writing a new file leaves no file at all`() {
        assertThrows<IOException> {
            writeAtomically(target) {
                it.write("partial".toByteArray())
                throw IOException("disk full")
            }
        }

        assertFalse(target.exists())
        assertFalse(tempFileFor(target).exists())
    }

    @Test
    fun `a temporary file that can't be created leaves the old file unchanged`() {
        writeText("old")
        tempFileFor(target).mkdir()

        assertThrows<IOException> { writeText("new") }

        assertEquals("old", target.readText())
    }
}
