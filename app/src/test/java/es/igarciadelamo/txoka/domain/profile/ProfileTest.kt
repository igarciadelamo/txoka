package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {
    private val profile = Profile(
        name = "Isidro García",
        phone = "+34600123456",
        email = "isidro@example.com",
        sharePhone = true,
        shareEmail = false,
    )

    @Test
    fun `only shared fields are exposed`() {
        assertEquals("+34600123456", profile.sharedPhone)
        assertNull(profile.sharedEmail)
    }

    @Test
    fun `builds initials from the first two words`() {
        assertEquals("IG", profile.initials)
        assertEquals("JM", Profile(name = "José María de la Cruz").initials)
        assertEquals("A", Profile(name = "  ana  ").initials)
        assertEquals("", Profile(name = "   ").initials)
    }
}
