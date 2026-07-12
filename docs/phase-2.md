# Phase 2: Social Features

## Progress

### Task 1: User profiles
- [x] completed

### Task 2: Favourites
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 3: Friend system
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 4: Comment system (database)
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 5: Comment UI
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 6: Spoiler toggle
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 7: Activity feed
- [ ] pending
- [ ] in progress
- [ ] completed

### Task 8: Watchlist visibility
- [ ] pending
- [ ] in progress
- [ ] completed

---

## Code Log

| Date | File(s) | Description |
|---|---|---|
| 2026-07-12 | `ProfileViewModel.kt`, `ProfileScreen.kt` | Profile screen: auth state display, "Watched Movies" section (watchlist status=watched), "Watched Shows" section (watchedEpisodes >= totalEpisodes && totalEpisodes > 0). Watched items navigate to detail screen. Removed watchHistoryDao dependency — uses watchlist columns directly. |
