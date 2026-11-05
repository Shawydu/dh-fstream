package dev.fstream.tv

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun packageName() {
        assertEquals("dev.fstream.tv", InstrumentationRegistry.getInstrumentation().targetContext.packageName)
    }
}
