# جحفان | Jahfan

Android PDF reader and editor with the original PdfCraft Android interface, Arabic and English.

## Source provenance

- Android UI and UniFFI bridge: `artcraft-android/pdfcraft-android`, original 0.0.4-beta Android app.
- Engine: `storytold/pdfcraft`, release **v0.9.0**, commit `645016addcfb9078c557f0189559ab47cdcee057`.
- The user-supplied Arabic source archive is a translation reference only. Its older engine and application logic are not used as the foundation.
- Upstream MIT / Apache-2.0 notices remain in the repository. Jahfan is a modified, unofficial application.

Application ID: `com.jahfan.pdf` (debug builds add `.debug`). Interface language follows the device initially and can be changed under Settings → Preferences → Language. Android 13+ also supports the system's per-app language setting. Document text and read-aloud voices are independent of the interface language.

## Mobile compatibility extensions

The upstream 0.9.0 engine is retained with the Android-specific extensions ported forward:

- Original PDF annotation timestamps, including the timezone.
- Annotation opacity and line width in inspection and refreshed comment summaries.
- Sound-annotation WAV extraction for voice comments.
- Selective hiding of sticky-note appearances while the Android viewer draws its own icons.
- On-device Office import, Excel export and photo document-edge detection from the Android source.

The upstream digital-ID creation policy requires at least eight password characters. The Android validation and translated hints follow that policy.

These extensions must be checked during every future upstream upgrade. Never replace the core solely by changing version numbers.

## Build

Use JDK 21, Rust 1.99.0, cargo-ndk 4.1.2, Android SDK API 37 and NDK 29.0.14206865.

```powershell
cd apps/pdfcraft-android/android
./gradlew.bat assembleDebug testDebugUnitTest -Ppdfcraft.abis=arm64-v8a
```

The installed NDK may be selected explicitly with `-Pjahfan.ndkVersion=VERSION`. The default stays pinned for reproducible CI. Preview APKs are debug-signed; a private release signing key must be configured before distribution as a production release.

GitHub Actions builds a private downloadable preview artifact and runs the original mobile compatibility tests and Android unit tests. Manual device checks remain necessary for layout, language switching, reading, editing, forms, signatures and large-file performance. Passing compilation does not prove all features or performance.
