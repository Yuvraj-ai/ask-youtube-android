# Design System: AskYoutube

## 1. Visual Theme & Atmosphere

A quiet reading surface. This is a tool for reading long-form speech and
checking what was actually said, so the interface should feel like a well-set
page of text: calm, high-contrast, generous line-height, and almost silent.

- **Density: 5 (balanced, text-led).** Enough air to scan a long answer; not so
  much that a single answer needs scrolling to read.
- **Variance: 2 (predictable, symmetric).** This is a utility, not a poster.
  Alignment is stable and left-ragged. No asymmetric editorial layouts.
- **Motion: 2 (restrained).** Motion only ever confirms that something happened
  — a state change, a result arriving. Nothing loops, nothing floats, nothing
  moves for decoration.

The single most important quality: **the answer text must be the loudest thing
on screen.** Every other element recedes.

## 2. Color Palette & Roles

Light theme:

- **Canvas** (`#FAFAF9`) — app background
- **Surface** (`#FFFFFF`) — answer cards, input fields, sheets
- **Surface Muted** (`#F4F4F5`) — inset regions, source list background
- **Ink** (`#18181B`) — primary text. Never pure black.
- **Ink Muted** (`#71717A`) — timestamps, labels, helper text
- **Ink Faint** (`#A1A1AA`) — placeholders, disabled states
- **Hairline** (`#E4E4E7`) — 1px dividers, card borders
- **Ember** (`#C2410C`) — the single accent. Send button, timestamp labels,
  active slider track, focus ring.

Dark theme:

- **Canvas** (`#09090B`) · **Surface** (`#18181B`) · **Surface Muted** (`#27272A`)
- **Ink** (`#FAFAF9`) · **Ink Muted** (`#A1A1AA`) · **Ink Faint** (`#71717A`)
- **Hairline** (`#3F3F46`) · **Ember** (`#FB923C`)

**Ember is the only accent.** Warm orange against neutral greys — deliberately
not the indigo/violet every AI-generated app reaches for. Maximum one accent
colour; saturation stays under 80%. No gradients on text, no glows, no coloured
shadows.

Errors and warnings are the *only* other permitted colours, and they appear
solely in error surfaces: a muted red (`#B91C1C` light / `#FCA5A5` dark) on a
very light red tint. Nothing decorative is ever red.

## 3. Typography Rules

- **Body / UI:** platform sans (Roboto). No bundled webfont — a 400 KB typeface
  download is not worth it for an app whose content is server-rendered prose.
  Hierarchy comes from weight and colour, not from size escalation.
- **Mono:** platform monospace, used **only** for timestamps (`12:04`), model
  identifiers (`gemini-embedding-2`), and the chunk-count value. This is the
  app's one typographic signature — technical metadata is set in mono so it
  reads as data, not prose.
- **Answer body:** 16sp / 26sp line-height. This is the only place generous
  leading matters, because it is the only place users read at length.
- **Labels:** 13sp, `Ink Muted`, sentence case. No ALL-CAPS labels, no
  letter-spaced eyebrow text.
- **Never** a serif. Never a display face. No oversized hero type — the largest
  text on any screen is the answer's own first line.

## 4. Component Stylings

- **Answer card:** white surface, 16dp radius, 1px hairline border, **no
  elevation shadow**. Answers sit in a conversation, not on a stack of floating
  cards. Internal padding 16dp.
- **Question bubble:** right-aligned, `Surface Muted` fill, 16dp radius with a
  4dp bottom-right corner to imply direction. Max width 80% of the row.
- **Send button:** 48dp circular, `Ember` fill, white arrow. The arrow is 20dp,
  not full-bleed — a heavy oversized glyph reads as a warning, not an action.
- **Inputs:** label above the field, helper text below in `Ink Muted`, 1px
  hairline border, 12dp radius. Focus ring is a 2dp `Ember` border. No floating
  labels. The chat input is inset with 16dp side margins and sits on a
  hairline-divided bar so it reads as attached to the conversation above it.
- **Sources:** a disclosure row beneath the answer reading
  "From the transcript (4)". When expanded, each source is a row with a mono
  timestamp and the passage beneath it in `Ink Muted`. No card per source.
- **Loading:** a thin 2dp indeterminate progress bar directly under the video
  field, plus a short status line. **No circular spinners** — a spinner in a
  reading app is visual noise. Label the stage: "Reading transcript…" then
  "Thinking…".
- **Empty state:** vertically centred in the available space, never top-anchored
  with dead space beneath. Title, one sentence of explanation, and — only when
  no API key is set — a single inline warning plus one button to Settings.
- **Slider:** Material 3 slider, track and thumb in `Ember`.
- **Status pill:** small, 8dp radius, tinted background. Green for a valid key,
  red tint for invalid, neutral for "not set". Icon plus text, never colour
  alone.

## 5. Layout Principles

- Mobile portrait, single column, 16dp horizontal margins throughout.
- Max content width 640dp, centred, so the answer does not run to 120
  characters on a tablet.
- Conversation is a scrolling list, newest at the bottom, with a divider
  between turns. Video field pinned above it, input bar pinned below.
- Vertical rhythm on a 4dp grid. 12dp between turns, 24dp between a turn and
  the next question.
- No element ever overlaps another. No absolutely positioned decoration.
- No three-across card rows. Settings is a single scrolling column of labelled
  sections separated by hairlines — not a grid of tiles.

## 6. Motion & Interaction

- **Only** animate `transform` and `opacity`. Nothing animates width, height,
  top, or left.
- Duration 150–250ms. Standard decelerate easing. No springs, no bounce, no
  overshoot — a bounce in a text tool reads as a toy.
- The sources disclosure fades and expands height. Nothing else animates.
- Lists and answers appear immediately. **No staggered cascade reveals** — the
  user asked a question and wants the answer, not a performance.
- **No perpetual motion of any kind.** No shimmer, no pulse, no floating
  elements, no infinite loops.

## 7. Anti-Patterns (Banned)

- No emojis anywhere.
- No purple, indigo, violet, or neon-blue accents. No glows, no outer shadows.
- No pure black (`#000000`) or pure white as a background; use the tokens.
- No gradient text, especially on headings.
- No circular loading spinners.
- No perpetual or looping animation.
- No drop shadows on cards in the conversation; borders only.
- No invented data. Never fabricate a statistic, a confidence score, a token
  count, or a relevance percentage next to a source. If a number is not real,
  omit it.
- No fake product names or placeholder people.
- No AI copywriting clichés: "Elevate", "Seamless", "Unleash", "Next-Gen",
  "Dive in", "Your gateway to".
- No "Scroll to explore", swipe hints, or scroll arrows.
- No top-anchored empty state leaving a dead zone beneath it.
- No `LABEL // YEAR` formatting.
- No letter-spaced uppercase eyebrow labels.
- No oversized send or submit glyphs.
