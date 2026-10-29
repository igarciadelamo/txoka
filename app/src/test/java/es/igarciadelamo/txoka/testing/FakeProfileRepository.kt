package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProfileRepository(initial: Profile? = null) : ProfileRepository {
    private val state = MutableStateFlow(initial)

    var savedProfile: Profile? = null
        private set

    override val profile: Flow<Profile?> = state

    override suspend fun save(profile: Profile) {
        savedProfile = profile
        state.value = profile
    }
}
