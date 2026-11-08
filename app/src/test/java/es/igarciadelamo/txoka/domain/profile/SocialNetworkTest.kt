package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialNetworkTest {
    @Test
    fun `accepts a bare handle with or without the at sign`() {
        assertEquals("ana.ruiz", SocialHandle.normalize("ana.ruiz", SocialNetwork.Instagram))
        assertEquals("ana.ruiz", SocialHandle.normalize(" @ana.ruiz ", SocialNetwork.Instagram))
        assertEquals("isidro-garcia", SocialHandle.normalize("isidro-garcia", SocialNetwork.LinkedIn))
    }

    @Test
    fun `extracts the handle from a pasted profile link`() {
        assertEquals(
            "ana-ruiz",
            SocialHandle.normalize("https://www.linkedin.com/in/ana-ruiz/", SocialNetwork.LinkedIn),
        )
        assertEquals(
            "ana.ruiz",
            SocialHandle.normalize("https://instagram.com/ana.ruiz/?hl=es", SocialNetwork.Instagram),
        )
        assertEquals(
            "ana.ruiz",
            SocialHandle.normalize("www.instagram.com/ana.ruiz", SocialNetwork.Instagram),
        )
    }

    @Test
    fun `rejects a link from another network`() {
        assertNull(SocialHandle.normalize("https://instagram.com/ana.ruiz", SocialNetwork.LinkedIn))
        assertNull(SocialHandle.normalize("https://www.linkedin.com/in/ana", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("https://notlinkedin.com/in/ana", SocialNetwork.LinkedIn))
    }

    @Test
    fun `rejects empty and malformed handles`() {
        assertNull(SocialHandle.normalize("", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("   ", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("@", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("ana ruiz", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("ana/ruiz/extra/../", SocialNetwork.Instagram))
        assertNull(SocialHandle.normalize("a".repeat(61), SocialNetwork.LinkedIn))
    }

    @Test
    fun `builds the profile link and the display label`() {
        assertEquals(
            "https://www.linkedin.com/in/ana-ruiz",
            SocialHandle.url(SocialNetwork.LinkedIn, "ana-ruiz"),
        )
        assertEquals("https://www.instagram.com/ana.ruiz", SocialHandle.url(SocialNetwork.Instagram, "ana.ruiz"))
        assertEquals("ana-ruiz", SocialHandle.display(SocialNetwork.LinkedIn, "ana-ruiz"))
        assertEquals("@ana.ruiz", SocialHandle.display(SocialNetwork.Instagram, "ana.ruiz"))
    }
}
