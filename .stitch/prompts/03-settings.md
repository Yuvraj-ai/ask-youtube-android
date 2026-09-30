Settings screen for AskYoutube, a mobile app that answers questions about a YouTube video using its transcript. The user is here to supply their own Gemini API key, and nothing has been configured yet.

**PLATFORM:** Mobile, portrait, native Android app

**PAGE STRUCTURE:**

A single scrolling column of labelled sections, separated by hairline dividers. NOT a grid of tiles. NOT cards. NOT a bottom tab bar.

1. **Top app bar:** A back arrow on the left, title "Settings" beside it.

2. **Gemini API key section:**
   - Section heading: "Gemini API key"
   - Helper text beneath the heading, muted, two lines: "Stored only on this device, encrypted. Get one at aistudio.google.com/app/apikey"
   - A single-line password text field below, showing masked dots as its value, labelled "Gemini API key"
   - A row of controls beneath the field: a filled primary button "Save", an outlined secondary button "Validate", and a plain text button "Show". The three controls sit in one row with even spacing and do not crowd each other.
   - A status pill beneath the controls: a small rounded chip containing a neutral circular icon and the text "No API key saved". The pill has a subtle tinted background.

3. **Retrieval section:**
   - Section heading: "Chunks to read"
   - Helper text beneath, muted, two lines: "How many transcript passages to send to Gemini for each question."
   - A large monospaced numeral "4" beneath the helper text, left-aligned.
   - A horizontal slider beneath the numeral, with discrete tick marks, the filled portion of the track reaching the "4" position. The thumb is a circle.

4. **Models section:**
   - Muted introductory line: "Model ids are editable in case Google retires one."
   - A single-line outlined text field labelled "Embedding model" containing the value "gemini-embedding-2", set in monospace.
   - A single-line outlined text field labelled "Chat model" containing the value "gemini-3.5-flash", set in monospace.

5. **About section:**
   - A small muted paragraph, no heading: "AskYoutube answers questions about a YouTube video using its transcript. It runs entirely on your phone: your API key never leaves the device, and there is no account and no server."

**CONTENT — use this exact copy** as listed above.

**STRUCTURAL NOTES:**

- Everything is one vertical scrolling column. No two-across layouts.
- Section headings are sentence case, medium weight, not uppercase, not letter-spaced.
- The model id values must render in a monospaced font to read as technical data.
- The status pill must be understated — it is a quiet status line, not an alert banner.
- Do NOT add a "Delete account", "Sign out", or "Danger zone" section.
- Do NOT add any statistics, usage numbers, plan names, or subscription tiers.
- Do NOT add a bottom tab bar or save button fixed to the bottom — the Save button belongs in the API key section.
