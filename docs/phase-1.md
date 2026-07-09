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
- [x] completed

### Task 5: Simkl API integration
- [ ] pending
- [ ] in progress
- [x] completed

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
| 2026-07-10 | `data/local/entity/*.kt`, `data/local/dao/*.kt`, `data/local/SyncStatus.kt`, `data/local/TallyDatabase.kt`, `data/di/AppModule.kt` | Room local database setup. Three entities: MediaEntity (cache, keyed by simklId), WatchlistEntity (with SyncStatus PENDING_ADD/UPDATE/DELETE/SYNCED, unique per userId+simklId), WatchHistoryEntity (with SyncStatus, unique per userId+simklId+season+episode). DAOs for search, filter by status, and sync query. Hilt-wired via AppModule. |
| 2026-07-10 | `data/di/NetworkModule.kt`, `data/remote/api/SimklApiService.kt`, `data/remote/model/*.kt`, `data/remote/SimklRepository.kt`, `data/remote/SimklImageUrl.kt` | Simkl API integration. Retrofit client with Hilt DI, OkHttp interceptor adding client_id/app-name/app-version/User-Agent to every request. Data models: SearchResult, MovieDetail, TvShowDetail, TvEpisode. SimklRepository wrapping API with automatic empty-array-to-null conversion for detail endpoints. Image URL helper for posters/fanart via wsrv.nl. SIMKL_CLIENT_ID in local.properties. |
