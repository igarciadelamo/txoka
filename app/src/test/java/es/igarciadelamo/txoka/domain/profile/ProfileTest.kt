package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {
    private val profile = Profile(
        name = "Isidro García",
        phone = "+34600123456",
        email = "isidro@example.com",
    )

    @Test
    fun `builds initials from the first two words`() {
        assertEquals("IG", profile.initials)
        assertEquals("JM", Profile(name = "José María de la Cruz").initials)
        assertEquals("A", Profile(name = "  ana  ").initials)
        assertEquals("", Profile(name = "   ").initials)
    }
}
