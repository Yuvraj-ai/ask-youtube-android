First-run state for AskYoutube, a mobile app that answers questions about a YouTube video using its transcript. No video has been added yet, so the screen must guide a brand-new user to their first action.

**PLATFORM:** Mobile, portrait, native Android app

**PAGE STRUCTURE:**

1. **Top app bar:** App title "AskYoutube" on the left. A single settings gear icon button on the right. No tabs, no bottom navigation bar, no back arrow.

2. **Video field region:** Beneath the app bar, a single-line outlined text field with the label "YouTube link" and no value. Below it, a hairline divider.

3. **Empty state (the main content):** Vertically centred in the space between the divider and the input bar, occupying the optical centre of the screen. This is important — the empty state must NOT be top-aligned, and there must NOT be a large dead zone of empty background beneath it.
   - A short headline: "Ask any YouTube video"
   - One sentence of body copy beneath it, muted, wrapping to two lines: "Paste a link above, then ask a question. Answers come only from that video's transcript."
   - A single inline warning line beneath the body copy, in the error colour: "Add your Gemini API key in Settings to get started."
   - One small button beneath the warning: "Settings"

   These four elements form one tight centred group with even vertical spacing between them. The group is compact, not stretched across the full height.

4. **Input bar:** Pinned to the bottom, above the system navigation bar, separated from the content above by a hairline divider. A rounded multiline text field with the placeholder "Ask about this video", and a small circular send button to its right containing a modest arrow glyph.

**CONTENT — use this exact copy:**

- App title: "AskYoutube"
- Video field label: "YouTube link"
- Headline: "Ask any YouTube video"
- Body copy: "Paste a link above, then ask a question. Answers come only from that video's transcript."
- Warning: "Add your Gemini API key in Settings to get started."
- Button label: "Settings"
- Input placeholder: "Ask about this video"

**STRUCTURAL NOTES:**

- The empty state group must be optically centred in the available area, not pinned to the top.
- Do NOT add an illustration, mascot, illustration, stock photo, or decorative graphic. Type only.
- Do NOT add a bottom tab bar, floating action button, hero section, or promotional card.
- Do NOT add a second button such as "Learn more" or "Get an API key".
- Do NOT add any statistics, numbers, or feature bullet lists.
- The video link field and the input bar at the bottom should both look inactive and empty — this is a resting state.
