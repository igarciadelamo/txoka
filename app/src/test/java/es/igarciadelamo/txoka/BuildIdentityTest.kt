package es.igarciadelamo.txoka

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildIdentityTest {
    @Test
    fun `preview uses a separate install identity`() {
        val expected = if (BuildConfig.BUILD_TYPE == "preview") "$OFFICIAL_PACKAGE.preview" else OFFICIAL_PACKAGE
        assertEquals(expected, BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `preview version is recognizable`() {
        assertEquals(BuildConfig.BUILD_TYPE == "preview", BuildConfig.VERSION_NAME.endsWith("-preview"))
    }

    @Test
    fun `preview retains release debugging restrictions`() {
        if (BuildConfig.BUILD_TYPE == "preview") assertFalse(BuildConfig.DEBUG)
    }

    private companion object {
        const val OFFICIAL_PACKAGE = "es.igarciadelamo.txoka"
    }
}
