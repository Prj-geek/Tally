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
- [ ] **Room local database setup** — Define local tables mirroring the key user data (watchlist, watch history) with sync status columns
- [ ] **Simkl API integration** — Retrofit client setup, API key management, data models for shows/movies/episodes
- [ ] **Search screen** — Search for movies and TV shows via Simkl API, display results in a list with posters
- [ ] **Detail screen** — Show/movie detail page with synopsis, poster, rating, episode list (for TV shows), metadata
- [ ] **Add to watchlist** — Button to add show/movie to user's list with status: Watching, Plan to Watch, Watched
- [ ] **Mark episodes watched** — Tap to mark/unmark episodes; data written to Room instantly, queued for Supabase sync
- [ ] **Library screens** — Separate tabs for Watching, Plan to Watch, Watched, pulling from local DB with sync status indicator
- [ ] **Background sync** — Sync pending local changes to Supabase when online; handle conflicts (latest wins)
- [ ] **Runtime tracking** — Compute total watch time from watched episodes/movies and display on profile

**Delivery:** A functional tracker that works offline-first. User can sign in, search, add, watch, and see their stats.

---

## Phase 2: Social Features

**Goal:** Turn the app from a personal tracker into a community. Users can find friends, comment on anything, and see what others are watching.

### Tasks

- [ ] **User profiles** — Profile screen showing username, avatar, stats (total watch time, shows/movies watched), recent activity
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

- [ ] **Discover / Trending page** — Fetch trending movies and shows from Simkl API, curated sections (popular, top-rated, trending this week)
- [ ] **Upcoming releases calendar** — Shows/movies from the user's library with upcoming release dates, displayed in a calendar/timeline view
- [ ] **Poster picker** — "Change Poster" option on show/movie detail. Fetch alternatives from fanart.tv API, user picks one. Poster URL stored in Room and Supabase
- [ ] **Push notifications (FCM)** — Set up Firebase Cloud Messaging. Notify when a show in the user's library has a new episode airing
- [ ] **Upcoming notifications** — Remind user when a movie in their watchlist releases in theaters/streaming
- [ ] **Search filters** — Filter search by movies vs TV shows, sort by year, rating, genre

**Delivery:** The app now helps users discover content and stay notified without opening the app.

---

## Phase 4: Polish & Community Building

**Goal:** Add depth — rewatch tracking, ratings, achievements, and tools to keep the community healthy.

### Tasks

- [ ] **Rewatch tracking** — Mark an episode/movie as rewatch, counter increments, tracked in user stats
- [ ] **Ratings/reviews** — Rate shows and movies on a 1–10 scale. Optional written review alongside the rating
- [ ] **Custom lists** — Users can create custom lists (e.g., "Best Anime of 2024", "Weekend Watch Pile") and share them
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
| Cloud backend | Supabase (Auth, PostgreSQL, Realtime) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose |

## APIs Used

| API | Purpose |
|---|---|
| Simkl | Movie/TV metadata, search, episodes, trending |
| fanart.tv | Alternative poster artwork |
| Google Sign-In | Authentication (via Supabase) |
