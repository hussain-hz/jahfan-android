# جحفان | Jahfan

تطبيق أندرويد لقراءة ملفات PDF وتعديلها، بواجهة PdfCraft Android الأصلية ودعم العربية والإنجليزية.

Android PDF reader and editor using the original PdfCraft Android interface, with Arabic and English.

- Application ID: `com.jahfan.pdf`.
- PdfCraft engine: **0.9.0**, pinned to upstream release commit `645016addcfb9078c557f0189559ab47cdcee057`.
- Language: device default initially; change under Settings → Preferences → Language.
- Android-specific features are ported forward alongside the newer engine, including voice comments, Office import, Excel export and photo edge detection.
- No account is required to use the app. PDFs are processed on the device.

This is an unofficial modified application. Original project licences and attribution are preserved. The Arabic archive supplied by the owner is used as a translation reference; the original GitHub sources are the code foundation.

## Preview builds

GitHub Actions → **Jahfan preview and compatibility** → successful run → **jahfan-preview-arm64** artifact. Preview APKs are debug-signed and intended for testing. Production signing is configured separately with private repository secrets.

## Development

See [JAHFAN.md](JAHFAN.md) for source provenance, build instructions and compatibility notes. The unmodified Android README is preserved in [README-UPSTREAM.md](README-UPSTREAM.md).

Before any future engine update, run the mobile compatibility suite, Android unit tests and affected engine tests. Also check the app on an Android device; automated checks do not guarantee every workflow, layout or performance characteristic.

## Upstream

- Android application: https://github.com/artcraft-android/pdfcraft-android
- PdfCraft engine: https://github.com/storytold/pdfcraft
- Licence: MIT OR Apache-2.0. See LICENSE-MIT, LICENSE-APACHE and NOTICE.
