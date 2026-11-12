package com.cussou.parallax.ui.matches

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cussou.parallax.data.MatchTransfer
import com.cussou.parallax.domain.Match
import java.io.File
import java.io.IOException

// Match files going out through the system share sheet and coming in through the file picker (F-65).
// No server, no account: the file travels by whatever app the user picks (NF-04).

const val MATCH_FILE_TYPE = "application/json"

/** Above this, a file cannot be a match of this app: it is not read. */
const val MAX_IMPORT_BYTES = 1_000_000

/** Writes [match] to a file other apps can read, and returns its content URI. */
fun writeExportFile(context: Context, match: Match): Uri {
    val directory = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(directory, MatchTransfer.fileName(match))
    file.writeText(MatchTransfer.export(match))
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

/** The share sheet for an exported file: messaging, email, files… */
fun shareIntent(uri: Uri, chooserTitle: String): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = MATCH_FILE_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, chooserTitle)
}

/** The text of a picked file, or null when it cannot be read or is too large to be a match. */
fun readImportFile(context: Context, uri: Uri): String? = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
        if (bytes.size > MAX_IMPORT_BYTES) null else bytes.decodeToString()
    }
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
}

/** InputStream.readNBytes only exists from Android 13. */
private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8_192)
    while (buffer.size() < limit) {
        val read = read(chunk, 0, minOf(chunk.size, limit - buffer.size()))
        if (read < 0) break
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}
