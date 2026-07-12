# Tally - Project Conventions

## Build
- Always use `.\gradlew assembleDebug --no-daemon` for builds (daemon causes shell hang)
- Build command: `cd E:\dev\Tally; .\gradlew assembleDebug --no-daemon 2>&1`

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
- DB v5: watchlist has totalEpisodes, watchedEpisodes for episode tracking
- TV episode checking auto-adds to watchlist via ensureInWatchlist()
- syncWatchedEpisodesCount() auto-transitions status when all episodes watched
