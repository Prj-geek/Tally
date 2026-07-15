# Tally

A personal TV and movie tracking app for Android. Search for shows and movies, build watchlists, mark episodes as watched, and track your total watch time — all with offline-first local storage and optional cloud sync via Supabase.

**Phase 1 (v0.1.0):** Personal tracking only. Social features (friends, comments, activity feed) are planned for Phase 2.

## Features

- **Search** — Find movies and TV shows via TMDB, with debounced search and result caching
- **Detail pages** — Full metadata: synopsis, rating, genres, runtime, episode lists for TV
- **Watchlist** — Add/remove shows and movies with a single tap
- **Episode tracking** — Mark individual episodes watched, batch "Watch All" per season, chronological "watch previous" dialog
- **Auto-transition** — Shows move from "Currently Watching" to "Watched" when all episodes are checked
- **Library** — Split into Shows and Movies tabs with "Currently Watching" / "Watchlisted" sub-sections
- **Runtime stats** — Total watch time computed from tracked episodes and movies, displayed on profile
- **Profile** — Avatar from Supabase session, watched shows/movies horizontal rows, sync status chip (Syncing/N pending/Synced/Sync failed)
- **Cloud sync** — Offline-first bidirectional sync with Supabase (pending changes push when online)
- **TV Time import** — Import watch history and watchlisted shows/movies from a TV Time GDPR export CSV
- **Anime support** — Episode group override system for multi-cour anime with manually-verified season splits

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Serialization | Kotlinx Serialization |
| Images | Coil |
| Backend | Supabase (Auth, PostgreSQL, Edge Functions) |
| Auth | Google Sign-In via Supabase Auth |
| TMDB API | Proxied through Supabase Edge Function (key never ships in APK) |

## Setup

1. **Clone the repo**
   ```
   git clone https://github.com/your-username/Tally.git
   ```

2. **Create `local.properties`** in the project root with:
   ```
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-anon-key
   GOOGLE_WEB_CLIENT_ID=your-google-oauth-client-id.apps.googleusercontent.com
   ```

3. **Build**
   ```
   .\gradlew assembleDebug --no-daemon
   ```

4. **Supabase setup** — Enable Google Auth provider in your Supabase dashboard and add the OAuth client ID from Google Cloud Console.

## TMDB Attribution

This product uses the TMDb API but is not endorsed or certified by TMDb.

<a href="https://www.themoviedb.org/">
  <img src="https://www.themoviedb.org/assets/2/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg" alt="TMDB" width="120">
</a>

## Known Limitations

1. **No native anime category** — TMDB has no distinct "anime" media type. Anime content returns as regular movie/TV entries. A client-side heuristic is used to flag likely anime for episode group overrides.

2. **Multi-cour anime season collapse** — TMDB sometimes collapses multiple broadcast cours into a single season. Tally uses a manually-verified episode-group override system for flagged shows: auto-detection inserts unverified overrides into Supabase, which must be manually toggled to `verified = true` before the app uses the corrected season structure.

3. **"Currently Watching" edge cases** — Auto-transition between "Currently Watching" and "Watched" is based on episode counts. Edge cases remain: a show that was fully watched getting a new season, or weekly episodes that should trigger "Currently Watching" when new episodes air. These will be addressed in Phase 3.

4. **Upcoming section is placeholder** — The "Upcoming" tabs in the library require release date tracking, planned for Phase 3.

5. **TV Time import** — Only supports the single combined GDPR CSV export. Individual episode-level CSV files are not supported.
