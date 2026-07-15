# Tally - Project Conventions

## Build
- Always use `.\gradlew assembleDebug --no-daemon` for builds (daemon causes shell hang)
- Build command: `cd E:\dev\Tally; .\gradlew assembleDebug --no-daemon 2>&1`
- SDK lives on E: (`ANDROID_HOME=E:\Android\Sdk`) — never touch C: for SDK stuff
- After big changes, run `.\gradlew clean assembleDebug --no-daemon` to avoid stale cache bloat
- Periodically clean Gradle cache: delete `$env:USERPROFILE\.gradle\caches\8.*` (old versions only, never delete current)
- Emulator snapshots bloat AVD — clear via AVD Manager when not needed

## Rules
- Complete one task at a time. Confirm it works, then wait for approval before the next.
- Test every change before asking for next task.
- When testing, include edge cases and what could go wrong. Don't just check the happy path.
- Test files only when there's logic worth testing (branching, calculations, state transitions).
- Do NOT test: UI, DAO, screen, module wiring, basic repository call-through.
- docs/ folder is for local reference only. Never commit or push changes to docs/.

## Testing Instructions
After every task, I will provide:
1. Exact build command
2. What to test and how
3. What should happen
4. What could go wrong

## Key Architecture
- Kotlin + Jetpack Compose + Hilt + Room + Supabase + TMDB API
- Bottom nav: Shows, Movies, Search, Profile
- Shows/Movies tabs: Watchlist/Upcoming sub-tabs
- Shows Watchlist split into "Currently Watching" / "Watchlisted"
- DB v6: watchlist has totalEpisodes, watchedEpisodes, genres; watch_history has runtime
- TV episode checking auto-adds to watchlist via ensureInWatchlist()
- syncWatchedEpisodesCount() auto-transitions status when all episodes watched
- Movie history uses null season/episode for dedupe (SQLite NULL != NULL in unique index)
- Clear data: soft-delete → syncAndWait → physical delete (deletes from Supabase)
- Sync chip on Profile: shows Syncing / N pending / Synced / Sync failed
- Profile: avatar from Supabase session, TopAppBar, ModalBottomSheet menu
- Episode group scanning: scanImportedEpisodeGroups() proposes overrides for anime season collapse
