package com.tally.app.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Profile(
    val id: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
)

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {
    val sessionStatus: Flow<SessionStatus> = supabase.auth.sessionStatus

    val currentUserId: String?
        get() = supabase.auth.currentSessionOrNull()?.user?.id

    suspend fun signInWithGoogle(idToken: String) {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
        ensureProfileExists()
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }

    private suspend fun ensureProfileExists() {
        val userId = currentUserId ?: return
        val existing = supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
        if (existing == null) {
            supabase.postgrest["profiles"].insert(
                mapOf("id" to userId)
            )
        }
    }
}
