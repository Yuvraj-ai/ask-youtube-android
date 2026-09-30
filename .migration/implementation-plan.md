# Implementation plan — AskYoutube

Ordered so each step is independently checkable. Backend is empty by design
(decision execution_model), so this is a single-app plan.

## Phase 1 — Project scaffold
1. Gradle: settings.gradle.kts, version catalog, root + app build files, wrapper.
2. Pin AGP and Kotlin versions; confirm compileSdk 35/36 against the installed SDK.
3. Manifest: INTERNET permission only, cleartext disabled, app label, launcher icon.
4. Minimal MainActivity that compiles.

## Phase 2 — Pure domain logic (no Android, unit-testable)
5. `VideoId` — parse and validate a YouTube URL or bare id.
6. `Chunker` — 1000/100 overlapping split over Cues, preserving timings.
7. `VectorIndex` — cosine similarity, add, top-k.
8. `TranscriptParser` — captionTracks extraction and json3 event parsing.
Written against plain Kotlin + org.json so they run in JVM tests.

## Phase 3 — Network
9. OkHttp singleton with timeouts, a desktop UA, and cookie jar. The UA and
   cookie jar are load-bearing for YouTube, not cosmetic.
10. `GeminiClient` — embedText, generateAnswer, validateKey. Maps HTTP status
    codes to typed `AppError` values.
11. `YouTubeTranscriptFetcher` — watch page, track selection (manual English
    then ASR), json3 fetch, zero-length-body detection.

## Phase 4 — Config and orchestration
12. `SettingsStore` over EncryptedSharedPreferences.
13. `RagEngine` — index cache keyed by videoId, prompt construction,
    grounded answer generation. Carries the source's prompt intent verbatim.
14. `AppError` — typed failures with user-facing messages.

## Phase 5 — UI
15. Theme: Material 3, dynamic colour, dark mode.
16. `HomeViewModel` — StateFlow<HomeUiState>, event handling.
17. Home screen: video card, conversation, sources, input, progress, errors.
18. Settings screen: key entry with reveal + validate + clear, model ids, k.

## Phase 6 — Verification
19. JVM unit tests for phase 2 plus transcript parsing and URL parsing.
20. `assembleDebug` and `testDebugUnitTest`.
21. `detect_secrets.py` against the new repo as a regression gate.

## Build prerequisite

No JDK is installed on this machine. The Android SDK is present
(platforms 35 and 36, build-tools 34/35/36) but `java`/`javac`/`gradle` are
absent and `sudo` requires a password, so the build cannot be executed from
this session until the user runs:

```bash
sudo pacman -S jdk17-openjdk
```

Then `JAVA_HOME` must point at the resulting JDK for Gradle to find it.
Phases 1-6 code is complete without it; phases 6's build steps are blocked
on that one command.
