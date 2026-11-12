package com.cussou.parallax

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cussou.parallax.data.MatchJson
import com.cussou.parallax.domain.Match
import com.cussou.parallax.domain.SizeUnit
import com.cussou.parallax.domain.Stage
import com.cussou.parallax.domain.Target
import com.cussou.parallax.domain.TargetSize
import com.cussou.parallax.ui.matches.MATCH_FILE_TYPE
import com.cussou.parallax.ui.matches.MAX_IMPORT_BYTES
import com.cussou.parallax.ui.matches.readImportFile
import com.cussou.parallax.ui.matches.shareIntent
import com.cussou.parallax.ui.matches.writeExportFile
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Export through the share sheet and import of a picked file, on a device (F-65). */
@RunWith(AndroidJUnit4::class)
class MatchFilesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val match = Match(
        id = "m1",
        name = "Rimfire Cup #4",
        date = "2026-12-14",
        stages = listOf(Stage("s", "Barricade", targets = listOf(Target(25.0, TargetSize(1.0, SizeUnit.MRAD), "T1")))),
    )

    @Test
    fun anExportedFileIsReadableThroughItsUriAndHoldsTheMatch() {
        val uri = writeExportFile(context, match)
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString().endsWith("Rimfire%20Cup%204.json"))
        assertEquals(match, MatchJson.decode(readImportFile(context, uri)!!))
    }

    @Test
    fun theShareSheetSendsTheFileWithReadPermission() {
        val uri = writeExportFile(context, match)
        val chooser = shareIntent(uri, "Share")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals(MATCH_FILE_TYPE, send.type)
        @Suppress("DEPRECATION")
        assertEquals(uri, send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun aFileTooLargeOrMissingIsNotRead() {
        val large = File(context.cacheDir, "large.json").apply { writeText("x".repeat(MAX_IMPORT_BYTES + 1)) }
        assertNull(readImportFile(context, Uri.fromFile(large)))
        large.delete()
        assertNull(readImportFile(context, Uri.fromFile(File(context.cacheDir, "missing.json"))))
    }
}
