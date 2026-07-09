package com.tally.app.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

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
    }

    suspend fun signOut() {
        supabase.auth.signOut()
    }
}
