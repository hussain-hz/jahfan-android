# Arabic text repair (beta.7)

The viewer's Add text action previously created a standard-font FreeText appearance. Helvetica's WinAnsi encoding cannot represent Arabic, so it drew question marks. Source paragraph editing also assumed that subset PDF fonts retained usable Arabic shaping tables and rejected mixed-style bidirectional rows.

## Changes

- Embed reproducible, OFL-licensed Noto Sans Arabic static Regular/Bold faces. Preserve full OpenType shaping tables; keep Arabic text as editable Unicode text, not a raster image.
- Build Arabic FreeText appearances with contextual shaping, bidirectional ordering, glyph positioning, ToUnicode and ActualText. The same path covers viewer text, fill/sign text, restyling and saved appearances.
- Measure Arabic wrapping with the actual embedded font. Keep combining marks together and retain explicit line breaks.
- Edit source paragraphs through the normal transaction path. Use a complete Arabic face when the source subset cannot shape the replacement; preserve per-run colors and weights and context across style boundaries.
- Keep ASCII numeric expressions such as dates in left-to-right order inside Arabic rows, without storing directional controls in document text.
- Fit wider Arabic inline replacements within existing explicit rows, anchored to their original right edge, instead of overlapping independent fields below.
- Keep untouched source rows and their original drawing operators, preventing a replacement font's metrics from reflowing unrelated lines.
- Preserve undo/redo, page rotation, save and reopen behavior. Removed the previous uniform-paragraph delete-and-recreate workaround.

## Validation

Host regression coverage includes plain Arabic, diacritics, mixed Arabic/English/digits, multiple lines, viewer comments, fill/sign text, source line and paragraph editing, style changes, rotated pages, undo/redo, and saved/reopened render equality. Private supplied documents are read from an environment variable; neither their contents nor generated copies belong in this repository. PDF outputs were also rendered with MuPDF for visual inspection.

Validation results: cargo check --locked -p pdfcraft-android passed. Host suites: Android API 27, fonts 73, editing 170, annotations 42, automation 35 tests passed. The private regression also passed after independently replacing each Arabic row. Android unit tests: 60 passed; lint: 0 errors and the 2 existing API-compatibility warnings. Reproduce native tests after sourcing tools/prepare-arabic-fonts.ps1 with the pinned craft-fonts checkout:

```
cargo +stable test --locked -p pdfcraft-android -p pdfcraft-fonts -p pdfcraft-edit -p pdfcraft-annot -p pdfcraft-automation --lib
```

Font generation: tools/generate-arabic-static-fonts.py (fonttools 4.60.1). Source and output SHA-256 hashes are pinned; licences and attribution are committed.

## Practical limits

A subset font cannot supply glyphs it never embedded. When a complete compatible source font is unavailable, the edited Arabic run uses Noto Sans Arabic and may differ visually from the original font. Scanned or outlined text has no editable character layer and still requires OCR. These changes do not claim support for every arbitrary PDF construction or every Unicode script.

## beta.8 Android integration correction

The Android paragraph loader previously fed the flattened `text` field into the inline editor, while native regression tests used `editor_text`. A single typed change therefore removed explicit source row breaks and caused mixed-style paragraphs to reflow. Android now loads `editor_text` verbatim (including whitespace and line breaks), falling back to `text` only if the field is absent or null. Commit, list and formatting actions consequently use the same row-preserving draft that the core expects. This corrects saved-document layout; the typing overlay itself remains a plain text field rather than a full WYSIWYG editor.

Validation on the emulator: opened a separate copy of the supplied PDF, inspected the actual Android EditText (8 explicit rows), inserted one character in the name row, committed through the UI, and pulled the saved PDF. Independent MuPDF rendering at 2x found all changed pixels confined to [391, 133, 455, 145] PDF points (the name row); every pixel elsewhere was identical. The document remains a local-only test artifact. Android unit tests passed (60).
