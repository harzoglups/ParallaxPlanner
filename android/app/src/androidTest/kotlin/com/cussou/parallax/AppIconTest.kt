package com.cussou.parallax

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The launcher icon (docs/06-name-and-icons.md, implementation constraints). */
@RunWith(AndroidJUnit4::class)
class AppIconTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theAppDeclaresItsOwnAdaptiveIcon() {
        assertNotEquals("the app has no icon of its own", 0, context.applicationInfo.icon)
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue(icon is AdaptiveIconDrawable)
        icon as AdaptiveIconDrawable
        assertNotNull(icon.foreground)
        assertNotNull(icon.background)
        // Themed icons, Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) assertNotNull(icon.monochrome)
    }
}
