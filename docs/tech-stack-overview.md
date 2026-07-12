# Tech Stack Overview — Tally

This document explains every piece of technology going into the app in plain language.

---

## 1. Kotlin — The Language

The app is written in **Kotlin**. It's the modern language for Android apps (Google's own recommendation). Think of it as a cleaner, safer version of Java — less code, fewer bugs, more readable.

---

## 2. Jetpack Compose + Material 3 — The User Interface

**Jetpack Compose** is how the app's screens are built. Instead of dragging and dropping buttons in a designer, you write code that *describes* what the screen should look like. It's fast, modern, and reactive — when data changes, the UI updates automatically.

**Material 3** is Google's latest design system. It decides how things look (colors, shapes, animations) so the app feels modern and consistent without designing everything from scratch.

---

## 3. MVVM + Clean Architecture — The Structure

This is just a way of organizing code so it doesn't turn into a mess as the app grows.

- **UI Layer** — What the user sees and taps (Compose screens)
- **ViewModel Layer** — The brain. Decides what to show, what happens when you tap a button
- **Data Layer** — Where data comes from (database, internet)

These layers are kept separate so you can change one without breaking another.

---

## 4. Hilt — The Plumber

When apps get big, pieces of code need to talk to each other. Hilt automatically wires them together. You tell it "here's a database," and Hilt delivers it to whichever part of the app needs it — no manual passing around.

---

## 5. Retrofit + OkHttp — The Internet Messenger

These two work together to talk to the **TMDB API** (the service that gives us movie/TV show data) — but not directly. All calls route through a **Supabase Edge Function** that holds the API key server-side.

- **Retrofit** — Takes the Edge Function proxy URL and turns it into clean Kotlin functions. You call `search("Breaking Bad")` and it handles the rest.
- **OkHttp** — The low-level engine that actually sends requests over the internet. Retrofit rides on top of it. It also attaches the Supabase anon key to every request for Edge Function authentication.

---

## 6. Kotlinx Serialization — The Translator

Converting data between formats is boring and error-prone. This library automatically converts the JSON (a text format) that TMDB returns into proper Kotlin objects the app can use. You never have to manually parse a single line of JSON.

---

## 7. Coil — The Image Loader

Movie posters and show artwork are loaded from the internet. Coil fetches them, caches them on your phone (so they don't re-download every time), and displays them in the app. It's lightweight and built for Kotlin/Compose.

---

## 8. Room — The Local Database (on your phone)

**Room** is Google's official SQLite ORM for Android. It lives on the user's phone and stores two things:

1. **Metadata cache** — Once you search for "Breaking Bad," we save its episodes and posters locally. Next time, it loads instantly without hitting the internet.
2. **Your watch history** — When you mark an episode as watched, it saves locally *immediately* so the UI updates without waiting for the internet. Then it syncs to the cloud in the background.

This means the app feels fast and works even when you're offline.

---

## 9. Supabase — The Cloud Backend

Supabase is the service running in the cloud that handles everything shared between users. It replaces the need to write your own server.

### What Supabase does inside the app:

| Feature | What it handles |
|---|---|
| **Auth** | Google Sign-In. Verifies your identity |
| **PostgreSQL Database** | Stores comments, friend connections, user profiles, and synced watch history |
| **Realtime** | When a friend comments, it shows up live without refreshing |
| **Edge Functions** | Proxies TMDB API calls server-side so the API key never ships in the APK |

### Why Supabase and not Firebase?

Firestore (Firebase's database) is a NoSQL database — think of it like a giant folder of loose papers. It's fine for simple stuff but gets messy for features like "find all comments by my friends on movies I've watched."

Supabase uses PostgreSQL — a proper relational database — like a well-organized filing cabinet with labeled folders and cross-references. It's much better suited for:

- Friend relationships (who is friends with whom)
- Comments (which user wrote what on which movie)
- Activity feeds (what did my friends do recently)

PostgreSQL also has a feature called Row-Level Security — you can define rules like "users can only delete their own comments" directly in the database, making the app more secure without extra code.

---

## 10. FCM (Firebase Cloud Messaging) — Push Notifications

When a new episode of a show you're watching airs, FCM sends a notification to your phone. It's free and reliable.

---

## 11. Coroutines + Flow — Handling Multiple Tasks Smoothly

Android apps do many things at once (download data, load images, update the UI). Coroutines make this safe and simple — no freezing the UI while waiting for network.

Flow is like a TV channel that streams data continuously. When the database updates, Flow automatically tells the UI to refresh. You don't have to manually check for changes.

---

## 12. Navigation Compose — Moving Between Screens

Determines what happens when you tap "Back" or navigate from the Home screen to a Show Detail screen. It manages the back stack (history of screens you've visited) so the Android back button works naturally.

---

## How It All Fits Together

```
┌─────────────────────────────────────────────┐
│  User taps "Mark Watched" on an episode      │
└─────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│  1. Room (local DB)                          │
│     → Saves immediately                      │
│     → UI updates instantly (no waiting)      │
└─────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│  2. Background sync queue                    │
│     → If online: push to Supabase            │
│     → If offline: wait, retry when connected │
└─────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────┐
│  3. Supabase (cloud DB)                      │
│     → Updates watch history row             │
│     → Friends see it in activity feed       │
│     → Runtime gets added to your stats       │
└─────────────────────────────────────────────┘
```

---

## High-Level Data Flow

1. **Metadata** (episode names, posters, descriptions) → **TMDB API** (via Supabase Edge Function proxy) → cached in **Room** on your phone
2. **User data** (what you watched, your lists) → saved in **Room** first → synced to **Supabase**
3. **Social data** (comments, friends) → written directly to **Supabase** since it needs to be shared
4. **Stats** (total watch time) → computed from your own watch history, no separate sync needed
