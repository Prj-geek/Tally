# Phase 1: Foundation & Core Tracking

## Progress

### Task 1: Project scaffold
- [ ] pending
- [ ] in progress
- [x] completed

### Task 2: Supabase project setup
- [ ] pending
- [ ] in progress
- [x] completed

### Task 3: Google Sign-In
- [ ] pending
- [ ] in progress
- [x] completed

### Task 4: Room local database setup
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 5: Simkl API integration
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 6: Search screen
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 7: Detail screen
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 8: Add to watchlist
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 9: Mark episodes watched
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 10: Library screens
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 11: Background sync
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 12: Runtime tracking
- [ ] pending
- [ ] in progress
- [ ] completed

---

## Code Log

| Date | File(s) | Description |
|---|---|---|
| 2026-07-09 | Project setup | Kotlin 2.3.0 + Compose project, Hilt DI, Navigation Compose skeleton with bottom nav (Library/Search/Profile tabs). Builds successfully. |
| 2026-07-09 | Dependencies & config | Upgraded to AGP 9.1.1, KGP 2.3.20, KSP 2.3.9, Hilt 2.59.2, Compose BOM 2026.06.01, Room 2.8.4, Gradle 9.3.1, compileSdk 37. Enabled config cache. Added @Preview to screens. |
| 2026-07-10 | `app/build.gradle.kts`, `local.properties` | Supabase project setup: enabled Google Auth, created SQL schema (profiles, media, watchlist, watch_history) with RLS policies, added SUPABASE_URL and SUPABASE_ANON_KEY to local.properties and buildConfig. |
| 2026-07-10 | `data/di/AppModule.kt`, `data/auth/AuthRepository.kt`, `ui/profile/ProfileViewModel.kt`, `ui/screens/ProfileScreen.kt` | Google Sign-In via Credential Manager. Profile screen shows "Sign in with Google" button → account picker → Supabase ID token exchange → auto-creates profile row on first login. Session persists via Supabase auto-refresh. GOOGLE_WEB_CLIENT_ID in local.properties. Dependencies: credentials:1.6.0, credentials-play-services-auth:1.6.0, googleid:1.2.0. Removed deprecated play-services-auth. |
