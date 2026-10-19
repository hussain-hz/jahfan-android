# Jahfan preview validation — 2026-10-19

Engine: PdfCraft **0.9.0**, upstream release commit `645016addcfb9078c557f0189559ab47cdcee057`, plus the preserved Android compatibility extensions documented in JAHFAN.md.

## Automated checks

| Suite | Passed | Failed |
| --- | ---: | ---: |
| Android mobile bridge / engine workflows | 18 | 0 |
| Android JVM unit tests | 46 | 0 |
| Annotation library | 42 | 0 |
| PDF creation and Office import | 49 | 0 |
| PDF export | 26 | 0 |
| PDF rendering and inspection | 301 | 0 |
| Total | 482 | 0 |

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
