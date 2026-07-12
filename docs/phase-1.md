# Phase 1: Foundation & Core Tracking

## Progress

### Task 1: Project scaffold
- [x] completed

### Task 2: Supabase project setup
- [x] completed

### Task 3: Google Sign-In
- [x] completed

### Task 4: Room local database setup
- [x] completed

### Task 5: TMDB API integration (via Supabase Edge Function proxy)
- [x] completed

### Task 6: Search screen
- [x] completed

### Task 7: Detail screen
- [x] completed

### Task 8: Add to watchlist
- [x] completed

### Task 9: Mark episodes watched
- [x] completed

### Task 10: Library screens
- [x] completed

### Task 11: Background sync
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 12: Runtime & genre tracking
- [ ] **pending** — Store `runtime` on watch_history and `genres` on watchlist at mark-watched time. Room migration (v6), Supabase ALTER TABLE, update SupabaseModels. Compute total watch time on profile from watch_history. Genre breakdown for future stats.

---

## Code Log

| Date | File(s) | Description |
|---|---|---|
| 2026-07-09 | Project setup | Kotlin 2.3.0 + Compose project, Hilt DI, Navigation Compose skeleton with bottom nav (Library/Search/Profile tabs). Builds successfully. |
| 2026-07-09 | Dependencies & config | Upgraded to AGP 9.1.1, KGP 2.3.20, KSP 2.3.9, Hilt 2.59.2, Compose BOM 2026.06.01, Room 2.8.4, Gradle 9.3.1, compileSdk 37. Enabled config cache. Added @Preview to screens. |
| 2026-07-10 | `app/build.gradle.kts`, `local.properties` | Supabase project setup: enabled Google Auth, created SQL schema (profiles, media, watchlist, watch_history) with RLS policies, added SUPABASE_URL and SUPABASE_ANON_KEY to local.properties and buildConfig. |
| 2026-07-10 | `data/di/AppModule.kt`, `data/auth/AuthRepository.kt`, `ui/profile/ProfileViewModel.kt`, `ui/screens/ProfileScreen.kt` | Google Sign-In via Credential Manager. Profile screen shows "Sign in with Google" button → account picker → Supabase ID token exchange → auto-creates profile row on first login. Session persists via Supabase auto-refresh. GOOGLE_WEB_CLIENT_ID in local.properties. Dependencies: credentials:1.6.0, credentials-play-services-auth:1.6.0, googleid:1.2.0. Removed deprecated play-services-auth. |
| 2026-07-10 | `data/local/entity/*.kt`, `data/local/dao/*.kt`, `data/local/SyncStatus.kt`, `data/local/TallyDatabase.kt`, `data/di/AppModule.kt` | Room local database setup. Three entities: MediaEntity (cache, keyed by tmdbId), WatchlistEntity (with SyncStatus PENDING_ADD/UPDATE/DELETE/SYNCED, unique per userId+tmdbId), WatchHistoryEntity (with SyncStatus, unique per userId+tmdbId+season+episode). DAOs for search, filter by status, and sync query. Hilt-wired via AppModule. |
| 2026-07-10 | `supabase/functions/tmdb-proxy/index.ts`, `data/remote/api/TmdbApiService.kt`, `data/remote/model/TmdbSearchResult.kt`, `data/remote/model/TmdbMovieDetail.kt`, `data/remote/model/TmdbTvShowDetail.kt`, `data/remote/model/TmdbEpisode.kt`, `data/remote/TmdbRepository.kt`, `data/remote/TmdbImageUrl.kt`, `data/di/NetworkModule.kt` | TMDB API integration via Supabase Edge Function proxy. Edge Function proxies TMDB requests server-side (API key never in APK). Retrofit service calls the proxy endpoint. Data models for multi-search, movie detail, TV detail, season/episodes. TmdbImageUrl for image.tmdb.org. NetworkModule updated to point at Supabase Functions base URL with anon key auth header. |
| 2026-07-10 | `ui/search/SearchViewModel.kt`, `ui/screens/SearchScreen.kt` | Search screen rewritten for TMDB. Single /search/multi call returns movies+TV+anime (anime as TV). Debounce 400ms, LRU cache 50 entries. No custom scoring — TMDB's built-in sort handles relevance. Person results filtered out. |
| 2026-07-10 | `data/local/entity/*.kt`, `data/local/dao/*.kt`, `data/local/TallyDatabase.kt` | Renamed simklId → tmdbId across all entities and DAOs. Database bumped to version 2. Added mediaType and backdropUrl fields to MediaEntity. |
| 2026-07-10 | `app/build.gradle.kts`, `local.properties` | Removed SIMKL_CLIENT_ID from buildConfig and local.properties. Updated dependency comments. |
| 2026-07-10 | Deleted files | Removed SimklApiService.kt, SimklRepository.kt, SimklImageUrl.kt, SearchResult.kt, MovieDetail.kt, TvShowDetail.kt, TvEpisode.kt (Simkl-specific code fully removed). |
| 2026-07-10 | `docs/plan.md`, `docs/phase-1.md`, `docs/tech-stack-overview.md`, `docs/rules.md` | Updated all docs: Simkl → TMDB, removed Simkl Sync task, added Known Limitations section, updated Tech Stack and APIs tables. |
| 2026-07-10 | `supabase/functions/tmdb-proxy/index.ts` | Fixed query param forwarding: Edge Function now forwards all query params except `path` to TMDB (was embedding them in the `path` value, breaking `query=` param delivery). Auth changed from v3 `?api_key=` query param to v4 `Authorization: Bearer` header for TMDB auth. |
| 2026-07-10 | `app/src/main/java/com/tally/app/data/remote/model/TmdbSearchResult.kt`, `data/remote/api/TmdbApiService.kt`, `data/remote/TmdbRepository.kt` | Fixed "No results found" bug: added missing `@SerialName("media_type")` to `mediaType` field (TMDB returns snake_case, without annotation kotlinx.serialization silently set it to null, causing the person-filter to drop everything). Retrofit service and repository updated to send TMDB query params separately from the `path` param. |
| 2026-07-10 | `ui/screens/DetailScreen.kt`, `ui/detail/DetailViewModel.kt`, `navigation/Routes.kt`, `navigation/TallyNavHost.kt` | Detail screen: backdrop image, title, metadata (year/genre/runtime), star rating, synopsis, season selector dropdown, episode list. Uses SavedStateHandle for mediaType+id. Hilt-injected ViewModel fetches details + episodes via TmdbRepository. Navigation wired with navArgument. |
| 2026-07-10 | `data/remote/AnimeSeasonCollapseDetector.kt` | Auto-detection heuristic for TMDB's anime season-collapse issue. Flags shows where `originalLanguage == "ja"`, genres contain "Animation", `numberOfSeasons <= 1`, and `numberOfEpisodes > 26`. |
| 2026-07-10 | `data/remote/model/TmdbEpisodeGroup.kt` | DTOs for TMDB episode group API: `TmdbEpisodeGroupsResponse`, `TmdbEpisodeGroup`, `TmdbEpisodeGroupDetail`, `TmdbEpisodeGroupGroup`, `TmdbEpisodeGroupEpisode`. Maps the `/tv/{id}/episode_groups` and `/tv/episode_group/{id}` endpoints. |
| 2026-07-10 | `data/remote/TmdbRepository.kt` | Added `getEpisodeGroups(tvId)` and `getEpisodeGroupDetail(groupId)` methods to fetch episode group listings and detail via the Edge Function proxy. |
| 2026-07-10 | `data/remote/EpisodeGroupOverrideRepository.kt` | Core anime season-override system. Checks Supabase `anime_episode_group_overrides` table for verified overrides. For unverified candidates, auto-detects best episode group (prefers type 6/7 Production/TV, names containing "season"), inserts unverified row for admin review. `getShowSeasons(show)` returns override-aware season structure. `toTmdbEpisode()` maps group episodes to TMDB format. |
| 2026-07-10 | `data/remote/model/TmdbTvShowDetail.kt` | Added `originalLanguage` field (`@SerialName("original_language")`) for anime detection heuristic. |
| 2026-07-10 | `data/di/AppModule.kt` | Supabase client now includes Postgrest plugin for querying `anime_episode_group_overrides` table. |
| 2026-07-10 | `ui/screens/DetailScreen.kt` | Fixed double Scaffold padding: added `contentWindowInsets = WindowInsets(0, 0, 0, 0)` to inner Scaffold and `windowInsets = WindowInsets(0, 0, 0, 0)` to TopAppBar to prevent status bar inset duplication with outer Scaffold in MainActivity. |
| 2026-07-10 | `ui/detail/DetailViewModel.kt` | Rewrote for override-aware season switching. When `EpisodeGroupOverrideRepository.getShowSeasons()` returns non-empty groups (verified override), uses episode group structure instead of default TMDB seasons. `SeasonSelector` now works with string labels + index. Episode group data is cached in ViewModel — no API calls when switching seasons. |
| 2026-07-12 | `ui/detail/DetailViewModel.kt`, `ui/screens/DetailScreen.kt` | Added to watchlist (Task 8). ViewModel injects `WatchlistDao` + `AuthRepository`, exposes `isWatchlisted`, `isWatched`, `rewatchCount` in UI state. `onToggleWatchlist()` toggles watchlist entry, `onCheckWatched()` adds to watched (removes from watchlist), `onUncheckWatched(isRewatched)` handles uncheck menu (Not Watched / Rewatched with count starting at x2, incrementing). DetailScreen: watchlist button in bottom bar slides out via AnimatedVisibility when watched. Watched checkbox (Material Checkbox) pinned right of title row, always visible. DB version 3 with migration adding `rewatchCount` column. |
| 2026-07-12 | `ui/detail/DetailViewModel.kt`, `ui/screens/DetailScreen.kt` | Episode tracking (Task 9). ViewModel injects `WatchHistoryDao`, collects watched episodes as `Set<Pair<Int,Int>>` (seasonNum, episodeNum). `onToggleEpisodeWatched()` upserts/deletes per-episode. `onToggleSeasonWatched()` batch watches/unwatches all episodes in a season. `onWatchEpisodes(from, to)` batch watches a range. `onWatchAllPrevious(targetSeason, targetEpisode)` loads previous seasons from API and batch watches all. DetailScreen: Material Checkbox next to each episode, "Watch All (x/y)" / "Unwatch All" button in season header. When checking episode with unwatched predecessors (current or previous seasons), shows AlertDialog ("Yes, watch all" / "Just this one"). TV shows: title-row watched/rewatch checkbox removed, watchlist bottom bar hides when any episode checked. LazyColumn 80dp bottom padding. |
| 2026-07-12 | `WatchlistEntity.kt`, `TallyDatabase.kt`, `WatchlistDao.kt`, `LibraryViewModel.kt`, `LibraryScreen.kt`, `DetailViewModel.kt`, `TallyNavHost.kt` | Library screen (Task 10). WatchlistEntity: added mediaType, title, posterPath. DB v4 migration. WatchlistDao: getAll + getByMediaType queries. LibraryViewModel: loads watchlist grouped by mediaType, checks watch history for shows. LibraryScreen: PrimaryTabRow Shows/Movies tabs, poster+title list items. DetailViewModel saves metadata when adding to watchlist. Nav wired with onItemClick. |
| 2026-07-12 | `ShowsMoviesScreen.kt`, `ProfileScreen.kt`, `ProfileViewModel.kt`, `WatchlistDao.kt`, `TallyNavHost.kt` | Shows tab split into "Currently Watching" / "Watchlisted" sections with headers. Profile tab: added "Watched Movies" section (from watchlist status=watched) + "Watched Shows" placeholder. ProfileViewModel injects WatchlistDao/WatchHistoryDao, loads watched items via combine. ProfileScreen items navigate to detail. |
| 2026-07-12 | `WatchlistEntity.kt`, `TallyDatabase.kt`, `DetailViewModel.kt`, `LibraryViewModel.kt`, `ShowsMoviesScreen.kt`, `ProfileViewModel.kt` | DB v5: added `totalEpisodes`/`watchedEpisodes` columns to watchlist. DetailViewModel: `loadWatchlistStatus()` syncs totalEpisodes from API, `syncWatchedEpisodesCount()` recounts from watch_history and auto-transitions status (watchlist→watched when all episodes checked). All episode toggle methods call sync. `ensureInWatchlist()` sets totalEpisodes on creation. LibraryViewModel: `isFullyWatched` field on LibraryItem. ShowsMoviesScreen: fully-watched shows filtered out of Currently Watching/Watchlisted. ProfileViewModel: Watched Shows uses `watchedEpisodes >= totalEpisodes && totalEpisodes > 0` filter, removed watchHistoryDao dependency. Fixed missing `ensureInWatchlist()` in `onToggleSeasonWatched()`. |
|   2 0 2 6 - 0 7 - 1 3   |   ` S y n c M a n a g e r . k t ` ,   ` S u p a b a s e M o d e l s . k t ` ,   ` D e t a i l V i e w M o d e l . k t `   |   F i x e d   S u p a b a s e   s y n c   b u g s :   1 .   ` w a t c h l i s t `   u p s e r t   f a i l e d   s i l e n t l y   o n   u n i q u e   c o n s t r a i n t ;   a d d e d   ` o n C o n f l i c t   =   " u s e r _ i d , t m d b _ i d " `   a n d   ` . s e l e c t ( ) `   t o   s a v e   S u p a b a s e   I D   l o c a l l y .   2 .   ` w a t c h _ h i s t o r y `   d u p l i c a t e   m o v i e   r o w s   c a u s e d   b y   r a c e   c o n d i t i o n   i n   o f f l i n e / r a p i d   c h e c k s ;   a d d e d   p r e - p u s h   s t a t u s   v e r i f i c a t i o n   t o   a b o r t   p h a n t o m   i n s e r t s .   3 .   R e w a t c h   c l e a n u p   b u g :   " N o t   W a t c h e d "   n o w   e x p l i c i t l y   m a r k s   A L L   l o c a l   w a t c h   h i s t o r y   r o w s   f o r   a   m o v i e   a s   ` P E N D I N G _ D E L E T E `   i n s t e a d   o f   j u s t   t h e   f i r s t   o n e   r e t u r n e d   b y   S Q L i t e ,   e n s u r i n g   f u l l   r e m o t e   c l e a n u p .   |  
 