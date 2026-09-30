Ask questions about a YouTube video using its transcript. This is the answered state: a user has pasted a link, asked a question, and is reading a grounded answer with the transcript passages that support it.

**PLATFORM:** Mobile, portrait, native Android app

**PAGE STRUCTURE:**

1. **Top app bar:** App title "AskYoutube" on the left. A single settings gear icon button on the right. No tabs, no bottom navigation bar, no back arrow — this is the root screen.

2. **Video field region:** Directly beneath the app bar, a single-line outlined text field showing a pasted YouTube URL, labelled "YouTube link". Below it, a thin indeterminate progress bar and a short status line reading "Thinking…". This region is separated from the conversation by a hairline divider.

3. **Conversation area (the visual centre of the screen):** One completed exchange, scrolled so the newest content sits at the bottom.
   - **Question bubble:** Right-aligned, muted grey fill, rounded on three corners and square on the bottom-right. Reads "What did he say about the pricing change?".
   - **Answer card:** Full-width, left-aligned beneath the question. Contains a multi-paragraph answer of roughly 90 words. Preserve visible paragraph breaks — the answer must NOT be flattened into a single block, and must NOT be hard-wrapped at a fixed character width. The answer is the loudest, highest-contrast text on the screen.
   - **Sources disclosure:** A compact tappable row at the bottom of the answer card reading "From the transcript (4)", with a small chevron. It is shown expanded, revealing four source rows. Each source row is a monospaced timestamp ("12:04", "18:31", "24:09", "31:52") with a short transcript passage beneath it in smaller muted text. Sources are plain rows separated by hairlines — NOT cards.

4. **Input bar:** Pinned to the bottom, above the system navigation bar, separated from the conversation by a hairline divider. A rounded multiline text field showing the placeholder "Ask about this video", with a small circular send button to its right containing a modest arrow glyph. The send button is small and calm, not a large prominent circle.

**Empty space:** A second, earlier question-and-answer pair is partially visible above the primary exchange, scrolled so only its answer card's bottom edge shows, implying a longer conversation history.

**CONTENT — use this exact copy:**

- App title: "AskYoutube"
- Video field value: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
- Video field label: "YouTube link"
- Status line: "Thinking…"
- Question: "What did he say about the pricing change?"
- Answer: "Around the eighteen minute mark he walks through the pricing change in detail. He says the free tier moves from three projects to two, and that the change applies at the next billing cycle rather than immediately.

He is explicit that existing customers keep their current rate for twelve months, and that the annual plan is unaffected. When asked about grandfathered pricing he repeats that it holds for a year and that support will honour it without requiring an upgrade.

He closes the topic by noting that the change is intended to fund the storage work, and that seats on the team plan are unchanged."
- Sources disclosure: "From the transcript (4)"
- Source 1 timestamp: "12:04" — passage: "So the first thing to know is that we are changing how the free tier works starting next cycle."
- Source 2 timestamp: "18:31" — passage: "If you are on the free plan today you currently get three projects. Under the new structure that becomes two."
- Source 3 timestamp: "24:09" — passage: "To be clear, this is not a retroactive change. Nobody gets billed differently for a period they have already paid for."
- Source 4 timestamp: "31:52" — passage: "Annual plans are not affected by this, and team seats stay exactly where they are today."
- Input placeholder: "Ask about this video"

**STRUCTURAL NOTES:**

- Do NOT render any statistic, confidence percentage, relevance score, or token count anywhere.
- Do NOT add a bottom tab bar.
- Do NOT add a hero section, banner, or promotional card.
- Do NOT add a floating action button.
- The conversation must be a plain scrolling list with generous line-height on the answer text.
