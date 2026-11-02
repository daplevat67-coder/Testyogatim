package com.pulse.minimove

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Regression guard: the default empty completion overlay must not cover the entire screen. */
class UiOverlayRegressionTest {
    @Test
    fun hiddenOverlayStaysInvisibleAtStartup() {
        val asset = File("src/main/assets/index.html").readText()
        assertTrue("Overlay must start with hidden attribute", asset.contains("id=\"overlay\" hidden"))
        assertTrue("CSS must override .overlay display:grid when hidden", asset.contains("[hidden]{display:none!important}"))
        assertTrue("Empty overlay must never block the main UI", asset.contains(".overlay:empty{display:none!important}"))
    }
}
