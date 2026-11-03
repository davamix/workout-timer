package io.github.davamix.workouttimer.data

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Added to a file's name to get the temporary file that [writeAtomically] fills before moving it into place. */
internal const val TEMP_SUFFIX = ".tmp"

/** The temporary file [writeAtomically] uses for [target]: same directory, same name plus [TEMP_SUFFIX]. */
internal fun tempFileFor(target: File): File = File(target.parentFile, target.name + TEMP_SUFFIX)

/**
 * Writes [target] so that it ends up either fully replaced or unchanged, even if the app or the device stops halfway
 * (plan §2.4):
 *
 * 1. [write] fills the temporary file ([tempFileFor]), which is then flushed and synced to disk, so a power cut
 *    can't leave an empty file after the move;
 * 2. the temporary file is moved over [target] in one atomic rename.
 *
 * If anything fails, the temporary file is deleted and the exception is rethrown. [target] is untouched.
 */
internal fun writeAtomically(
    target: File,
    write: (OutputStream) -> Unit,
) {
    val temp = tempFileFor(target)
    try {
        FileOutputStream(temp).use { out ->
            write(out)
            out.flush()
            out.fd.sync()
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
    } catch (e: Throwable) {
        temp.delete()
        throw e
    }
}
