# Security Review — Youtube-Video-Summeriser

Reviewed: 2026-09-30. Scope: source repo at `3b7db1a`, plus the proposed Android port.

## Source repository findings

`detect_secrets.py` returned **0 findings** — no hardcoded API keys, database URIs, or private keys.

| ID | Finding | Severity | Notes |
|----|---------|----------|-------|
| S1 | API key is accepted at runtime and held only in process memory. | Informational (good) | `main.py:9`. The key is never written to disk and there is no `.env`. The commented-out `load_dotenv` at `uany.py:10-12` is dead code and is correctly not used. |
| S2 | `readme.md:95` claims the key "is not saved anywhere". | Low | Accurate for the current code, but it is a claim about a prototype. It must be restated honestly for whatever the Android port does. |
| S3 | No input validation on the YouTube URL. | Low | An arbitrary string is handed to the transcript loader. Not exploitable server-side here, but on Android it must be validated before any network call. |

## Proposed Android port — trust boundaries

This is where the real decisions are. Two viable models, with different consequences:

### Model A — BYOK, on-device (user supplies their own Gemini key)

- The Gemini key is entered by the user in a Settings screen and stored **on the device**.
- All Gemini traffic originates from the device. No developer key exists anywhere.
- **The key is never in the APK, in source, or in any resource file.** It is a runtime secret belonging to one user.
- Storage: must be encrypted at rest. `EncryptedSharedPreferences` (AndroidX Security) or equivalent. Plain `SharedPreferences` is not acceptable.
- Residual risk, stated plainly: on a rooted device the key can be extracted. This is the accepted, normal trade-off of BYOK apps. It is materially better than the alternative of shipping a developer's key, which would leak to every user and every APK extraction.
- Requires the `INTERNET` permission. HTTPS only; cleartext must stay disabled.
- Backend required: **none**.

### Model B — proxied through a backend

- The Android app never holds a Gemini key. It calls your server; the server holds the key.
- **This contradicts the requested Settings screen**, which exists precisely so the user supplies a key. Under Model B there is nothing for the user to configure.
- Cost: hosting, and the Python/langchain/FAISS stack has to run somewhere.

## Rules for whichever model is approved

1. **Never** commit a Gemini key, never place one in `strings.xml`, `BuildConfig`, or any resource, and never bake one into the APK. `detect_secrets.py` should be run against the Android repo as a regression check.
2. If Model A: encrypt the stored key; redact it from logs; never include it in crash reports or analytics.
3. If Model A: validate the key before first use (one cheap call) rather than letting the first real request fail.
4. Allow the user to clear the key, and make that action actually delete it.
5. Both models: HTTPS only, no cleartext traffic, and no unnecessary permissions. `INTERNET` is the only permission the app actually needs — no storage, no camera, no location.
6. If a backend is ever added later, keys stay server-side and the APK never holds one.

## Open decision

`U3` in `analysis.md` — personal tool vs. product — determines A vs B. This is not inferred; it must be answered.
