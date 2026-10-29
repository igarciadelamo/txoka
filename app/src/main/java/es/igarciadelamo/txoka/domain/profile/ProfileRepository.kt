package es.igarciadelamo.txoka.domain.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Emits the stored profile, or null when onboarding has not been completed yet. */
    val profile: Flow<Profile?>

    suspend fun save(profile: Profile)
}
