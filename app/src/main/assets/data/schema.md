# I Ching Dataset Schema

Each dataset is an array of 64 hexagram objects. Required fields:

- key_primary: integer 0..63. 6-bit key derived from primary lines (yin=0, yang=1), bottom line is bit 0.
- king_wen: integer 1..64. Display order and numbering.
- name: string. Hexagram name as given in the source.
- judgment: string. King Wen text for the whole hexagram.
- description: string. Extended description/overview text for the whole hexagram.
- image: string. Symbolism text (Image) for the whole hexagram.
- lines: array of 6 strings. Line texts, ordered bottom (index 0) to top (index 5).
- lines_commentary: array of 6 strings. Line commentary (Image/line commentary), ordered bottom to top.

Optional fields:

- trigrams: { lower: string, upper: string }
- aliases: array of strings
- source: string (translation attribution)

Notes:

- The dataset must include all 64 entries and all keys must be unique.
- Translation text is derived exclusively from the local `index.php` source.
