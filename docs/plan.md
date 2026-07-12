# Tally — Development Plan

## Overview

A TV/movie tracking app with a community brain. Track what you watch, connect with friends, discuss episodes and movies — all without spoilers. Data is manual (you tell the app what you watched), works offline-first with cloud sync, and runs on Android using free services only.

---

## Phase 1: Foundation & Core Tracking

**Goal:** A working app where a user can sign in, search for shows/movies, add them to lists, and mark episodes as watched. All data syncs between the phone and cloud.

### Tasks

- [x] **Project scaffold** — Kotlin + Jetpack Compose project, Hilt setup, Navigation Compose skeleton (main screens, bottom nav)
- [x] **Supabase project setup** — Create Supabase project, enable Google Auth, design database schema (users, media, watchlist, watch history). **Note:** A separate OAuth client ID must be generated for the release (production) variant and added to the comma-separated list in the Google provider config on Supabase.
- [x] **Google Sign-In** — Authenticate via Supabase Auth via Credential Manager, create user profile row on first login
- [x] **Room local database setup** — Define local tables mirroring the key user data (watchlist, watch history) with sync status columns
- [x] **TMDB API integration (via Supabase Edge Function proxy)** — Retrofit client pointed at Supabase Edge Function, TMDB data models, image URL builder. All TMDB calls route through the Edge Function; the API key never ships in the APK.
- [x] **Search screen** — Search for movies and TV shows via TMDB's /search/multi endpoint, display results in a single mixed list with posters and type badges. Custom ranking logic removed — TMDB's built-in sort handles relevance + popularity.
- [x] **Detail screen** — Show/movie detail page with synopsis, poster, rating, episode list (for TV shows), metadata. Includes anime episode group override system — auto-detects TMDB's season-collapse for multi-cour anime, inserts unverified override for admin review in Supabase, and uses verified overrides to display proper season splits with episode group data.
- [x] **Add to watchlist** — Sticky bottom bar on detail screen with watchlist toggle button and watched checkbox. Watchlist toggle adds/removes silently. Watched checkbox marks as watched (removes from watchlist), with uncheck menu for "Not Watched" / "Rewatched" (rewatch counter increments). Bar slides out via AnimatedVisibility when watched is checked. DB stores rewatch count.
- [x] **Mark episodes watched** — Tap to mark/unmark episodes; per-episode checkbox tracking, season batch "Watch All"/"Unwatch All", "watch previous episodes" dialog for chronological watching. Auto-adds show to watchlist via `ensureInWatchlist()`. DB v5: `totalEpisodes`/`watchedEpisodes` columns on watchlist for auto-transition to "Watched Shows" in Profile.
- [x] **Library screens** — Split into Shows and Movies tabs. Shows tab: "Watchlist" section split into "Currently Watching" (shows with some watched episodes but not all) and "Watchlisted" (no watched episodes) sub-sections, plus "Upcoming" (placeholder — requires release date tracking). Movies tab: "Watchlist" and "Upcoming" (placeholder). Fully-watched shows auto-transition to Profile's "Watched Shows" via `totalEpisodes`/`watchedEpisodes` tracking. Unreleased content added by user goes directly to Upcoming, not Watchlist.
- [ ] **Background sync** — Sync pending local changes to Supabase when online; handle conflicts (latest wins)
- [ ] **Runtime tracking** — Compute total watch time from watched episodes/movies and display on profile

---

## Phase 2: Social Features

**Goal:** Turn the app from a personal tracker into a community. Users can find friends, comment on anything, and see what others are watching.

### Tasks

- [x] **User profiles** — Profile screen showing auth state, "Watched Movies" section (movies marked as watched), "Watched Shows" section (shows where `watchedEpisodes >= totalEpisodes && totalEpisodes > 0`). Watched items navigate to detail screen. Removed `watchHistoryDao` dependency — uses watchlist columns directly.
- [ ] **Favourites** — Default "Favourites" list for every user. Three-dot menu on detail screen with "Add to Favourites" / "Remove from Favourites" option. Favourites displayed as a section on the profile screen.
- [ ] **Friend system** — Search users, send friend requests, accept/reject, list friends
- [ ] **Comment system (database)** — Supabase tables for comments (shared, no local copy needed). Polymorphic: comment on a movie, a show, an episode, or an overall show page
- [ ] **Comment UI** — Write, edit, delete comments. Comment list with user avatar, timestamp, text
- [ ] **Spoiler toggle** — Optional spoiler flag when posting. Flagged comments are blurred with a "Show Spoiler" tap-to-reveal
- [ ] **Activity feed** — What your friends watched, commented on, added to their list. Real-time via Supabase Realtime
- [ ] **Watchlist visibility** — Option to make your watchlist public or private

**Delivery:** The app now has a social layer. You can add friends, discuss shows, and see a live feed.

---

## Phase 3: Discovery & Notifications

**Goal:** Help users find what to watch next and stay updated on shows they follow.

### Tasks

- [ ] **Discover / Trending page** — Fetch trending movies and shows from TMDB via Edge Function proxy, curated sections (popular, top-rated, trending this week)
- [ ] **Upcoming releases calendar** — Shows/movies from the user's library with upcoming release dates, displayed in a calendar/timeline view
- [ ] **Poster picker** — "Change Poster" option on show/movie detail. Fetch all available posters from TMDB's `/movie/{id}/images` or `/tv/{id}/images` endpoint, display in a grid for user selection.
- [ ] **Trailer link** — Show YouTube trailer button on detail screen. For movies, link to the movie trailer. For TV shows, link to the Season 1 trailer. Fetch via TMDB's `/movie/{id}/videos` or `/tv/{id}/videos` endpoint, filter for YouTube trailers.
- [ ] **Cast data** — Display cast list on detail screen with character name, actor name, and profile photo. Fetch via TMDB's `/movie/{id}/credits` or `/tv/{id}/credits` endpoint.
- [ ] **Push notifications (FCM)** — Set up Firebase Cloud Messaging. Notify when a show in the user's library has a new episode airing
- [ ] **Upcoming notifications** — Remind user when a movie in their watchlist releases in theaters/streaming
- [ ] **Search filters** — Filter search by movies vs TV shows, sort by year, rating, genre

**Delivery:** The app now helps users discover content and stay notified without opening the app.

---

## Phase 4: Polish & Community Building

**Goal:** Add depth — rewatch tracking, ratings, achievements, and tools to keep the community healthy.

### Tasks

- [ ] **Rewatch tracking & Diary UI** — Mark an episode/movie as rewatch, counter increments, tracked in user stats. Includes a Diary UI to view, edit, and delete specific individual watch dates.
- [ ] **Ratings/reviews** — Rate shows and movies on a 1–10 scale. Optional written review alongside the rating
- [ ] **Stats dashboard** — Deeper stats: hours watched per month, most-watched genre, completion rate, streaks
- [ ] **Achievements / badges** — Milestones (watched 100 episodes, 10 movies in a week, added 5 friends). Stored in Supabase, displayed on profile
- [ ] **Moderation tools** — Report inappropriate comments, block users. Basic admin panel for removing flagged content
- [ ] **Onboarding flow** — First-launch screens explaining the app: manual tracking, spoiler system, social features

**Delivery:** A feature-rich community app that rewards engagement and keeps itself clean.

---

## Future Ideas (not in scope yet)

- iOS/web version (Flutter migration or rewrite)
- Integration with streaming services for automatic tracking
- Watch parties / synchronized viewing
- Recommendations engine based on your history
- Dark mode themes and customization

---

## Data Model Roadmap

The following columns need to be added for stats/analytics features:

| Column | Table | Phase | When created | Notes |
|--------|-------|-------|-------------|-------|
| `genres` TEXT | watchlist | Phase 1 (Runtime) | At mark-watched / episode check | Comma-separated genre IDs from TMDB, denormalized for stats breakdown |
| `runtime` INTEGER | watch_history | Phase 1 (Runtime) | At mark-watched / episode check | Minutes from TMDB, enables total watch time computation without API calls |
| `rating` INTEGER | watchlist | Phase 4 (Ratings) | User action | 1-10 scale, nullable |

Each requires: Room migration (ALTER TABLE + new DB version), Supabase ALTER TABLE, update to SupabaseModels DTO.

---

## Known Limitations

1. **No native anime category** — TMDB has no distinct "anime" media type. Anime content returns as regular movie/tv entries. Currently unresolved — needs a future product decision (client-side heuristic filtering vs. second data source like Jikan/AniList).

2. **TV/season modeling** — TMDB's TV/season structure can split anthology series (e.g. American Horror Story) differently than expected. A client-side alias map or fallback data source may be needed if this becomes a common complaint.

3. **Anime episode group overrides require manual approval** — The auto-detection inserts unverified overrides into Supabase. They must be manually toggled `verified = true` in the Supabase Table Editor before the app uses the corrected season structure. This is intentional — episode groups can be ambiguous and need a one-time human sanity check.

4. **"Currently Watching" auto-transition** — Shows auto-move between "Currently Watching" and "Watched Shows" based on `totalEpisodes`/`watchedEpisodes` counts. Remaining edge cases: (a) a show with all episodes watched gets a new season premiere → should move from "Watched" back to "Currently Watching", (b) weekly episodes — show should move to "Currently Watching" when unwatched aired episodes exist. Requires release date storage + periodic checks. Planned for Phase 3 Upcoming Releases Calendar task.

---

## Tech Stack Summary

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Serialization | Kotlinx Serialization |
| Images | Coil |
| Local DB | Room (SQLite) |
| Cloud backend | Supabase (Auth, PostgreSQL, Realtime, Edge Functions) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |

## APIs Used

| API | Purpose |
|---|---|
| TMDB (via Supabase Edge Function proxy) | Movie/TV metadata, search, episodes, trending, images, videos, credits |
| Google Sign-In | Authentication (via Supabase) |
