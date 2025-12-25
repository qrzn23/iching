# AGENTS.md (Android / Kotlin / Jetpack Compose)

This file defines how **Codex-powered agents** must interact with this repository.
It exists to prevent combinatorial chaos, preserve semantic coherence, and keep the I Ching app deterministic, testable, and maintainable.

Agents must follow these rules strictly. If a requested change violates them, refuse or propose an alternative rather than silently introducing entropy.

---

## Project Overview
- Android I Ching (Yijing) casting + reading app
- Kotlin + Jetpack Compose (ONLY). No XML layouts.
- Offline-first content loaded from local JSON.
- Deterministic 3-coin casting with stored seed.

Non-goals:
- No networking, scraping, online translations, or AI-generated “canon”.
- No tarot/astrology mashups.
- No additional casting methods unless explicitly requested.

---

## Hard Constraints (Do Not Break)

### Tech stack
- Kotlin + Jetpack Compose only.
- No Godot code, assets, exports, or references.
- Avoid new dependencies unless necessary.

### Determinism
- Casting must be reproducible across devices.
- Support:
  - user-provided seed OR
  - generated seed stored with the record
- Same seed + same method => identical 6-line output (6/7/8/9).

### Data authority
- Hexagram texts are data, not logic.
- No hardcoded judgment/image/line texts in code.
- All canonical text must load from JSON in Android assets (or res/raw if chosen).
- If dataset missing/invalid: show a clear error state (no crash).

### Ordering systems
- Internal key uses 6-bit binary (bottom line = bit 0).
- UI may display King Wen numbering (1–64).
- Mapping between binary key and King Wen number must live in the dataset.

---

## Domain Model

### Line values (ints)
- 6 old yin (moving) -> changes to 7
- 7 young yang (stable)
- 8 young yin (stable)
- 9 old yang (moving) -> changes to 8

Hexagram lines:
- exactly 6 lines, indexed bottom -> top:
  index 0 = bottom, index 5 = top

Derived:
- primary_bits (yin=0, yang=1)
- changed_bits (movement applied)
- changing_lines indices where value is 6 or 9

Binary key:
- key_primary: 0..63
- key_changed: 0..63
Key generation must be pure and unit-tested.

---

## Casting Methods
- Only 3-coin method (id: coins3_v1)
- No additional methods unless explicitly requested.
- Casting/keying logic must be pure Kotlin functions (no Android/Compose dependency).

---

## Repository Structure (Android Studio)
Agents should use/maintain this structure (create if missing):

- app/src/main/java/<package>/
  - data/
    - HexagramEntry.kt (data classes)
    - HexagramRepository.kt (loads JSON from assets)
  - domain/
    - Casting.kt (pure functions: cast, apply changes, keying)
    - Models.kt (CastResult, etc.)
  - ui/
    - screens/ (Consult, Cast, Interpretation, Viewer)
    - components/ (HexagramGlyph, StyledButton, etc.)
    - theme/ (colors, typography, spacing)
- app/src/main/assets/
  - data/iching_en.json
  - schema.md (document JSON schema)

Persistence:
- app should store user-generated history locally (DataStore preferred, Room optional only if necessary).
- Store: timestamp, method id, seed, lines[6], derived keys, optional user note.
- No silent data loss; saves must be atomic/safe.

---

## UI/UX Rules
- Dark, low-clutter, near-monochrome palette.
- Empty space is intentional.
- Motion allowed only as subtle feedback (state transitions), not decorative.
- All buttons share one global style (monochrome/grey, consistent shape/padding).
- Consult entry screen: single primary action.
- Interpretation screen: hexagram glyph as axis + readable text panel.
- Viewer screen: manual trigram/hexagram composition.
- Avoid walls of text without structure; scrolling only for long text blocks.

---

## Testing Invariants (must exist)
Add unit tests (JUnit) under app/src/test:
- changed_value(6)=7, changed_value(9)=8, others unchanged
- key stability: same lines => same key
- determinism: same seed => same cast output (coins3_v1)
- moving lines detection

Tests must run headless (no Android instrumentation required for core logic).

---

## Performance + Safety
- No network calls.
- Load/parse JSON once and cache in repository.
- Fail gracefully with error UI if JSON missing/invalid.

---

## Definition of Done
A change is done when:
- App builds and runs on Android.
- Casting is deterministic and reproducible.
- Primary + changed hexagrams render correctly.
- Text loads offline from JSON.
- Unit tests pass.
