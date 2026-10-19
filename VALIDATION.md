# Jahfan preview validation — 2026-10-19

Engine: PdfCraft **0.9.0**, upstream release commit `645016addcfb9078c557f0189559ab47cdcee057`, plus the preserved Android compatibility extensions documented in JAHFAN.md.

## Automated checks

| Suite | Passed | Failed |
| --- | ---: | ---: |
| Android mobile bridge / engine workflows | 19 | 0 |
| Android JVM unit tests | 46 | 0 |
| Annotation library | 42 | 0 |
| PDF creation and Office import | 49 | 0 |
| PDF export | 26 | 0 |
| PDF rendering and inspection | 301 | 0 |
| Total | 483 | 0 |

The original mobile suite also passed all 16 tests before the engine update. The upgraded suite includes explicit checks for Office import → Excel export → PDF save/reopen and invalid photo detection. Voice comment creation, extraction and save/reopen, digital signatures and signer trust, forms, search, selection, annotations, rendering, passwords, attachments and undo are covered by the mobile tests.

The digital-ID test uses an eight-character password and confirms that the older seven-character sample is rejected, matching the upstream 0.9.0 password policy. The annotation-display and Office-detection test expectations retain the capabilities already present in the Android source.

## Build and emulator checks

- Local Android builds and JVM tests succeeded with JDK 17, Rust 1.99.0, SDK 37 and the installed NDK 30.0.16248370 override. CI defaults to JDK 21 and pinned NDK 29.0.14206865; a cloud CI result is reported separately.
- Both ARM64 and x86_64 native libraries were built; the downloadable phone preview contains ARM64 only.
- The app was installed and launched on the existing Android API 37 x86_64 emulator.
- A synthetic PDF was opened through Android's document URI and its text was visually verified.
- Arabic and English were selected through the app's own language preferences. Arabic layout direction and English layout direction were observed, including the navigation labels.
- Arabic toolbar labels were adjusted to avoid splitting connected words across lines and visually checked.

## Remaining validation

This preview is debug-signed. Production signing, full manual testing of every tool on physical devices, long-session stability, large-file speed and memory benchmarks, and comprehensive Arabic layout review remain before a production release. Automated tests do not prove every feature is unchanged or that performance is better. OCR language limitations remain those of the engine; this work does not add Arabic OCR.

## Arabic font correction — 0.1.0-beta.2

- Embedded the pinned Noto Sans Arabic font from craft-fonts revision `d1cf47d05e04426615fbda1d637b9ca6b1e7af35`, SHA-256 `63111b5b2e074dd48cc67692e0a2726d86ee94c1c37fe8598257b7b4e87e869e`.
- All 19 mobile tests passed with the required font, including Arabic addition on a blank page, save/reopen, search and rendering. The 13 Arabic-specific edit-library tests passed as well (additional to the table's suites).
- The previous build lacked the font. The new build requires a font source and includes the Noto OFL licence in its licences screen.
- Replacing an existing Latin-font paragraph with Arabic still reports the engine's missing-glyph/fallback error; the correction covers Arabic added text and its supported editing paths.
- Fixed an intermittent startup race observed on the emulator: the engine startup coroutine could access a lazy delegate before its property initializer ran.

- The saved Arabic sample was displayed on the API 37 emulator: connected letters and correct reading order were visually verified. The corrected app started successfully without a crash.
- The ARM64 APK built successfully; its signature and embedded Arabic OFL licence were verified. Android JVM tests passed.
