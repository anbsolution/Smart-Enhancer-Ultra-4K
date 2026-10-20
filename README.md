# Smart Enhancer Ultra 4K

Native Android image enhancement app with a dark UI, bounded working-image decoding, offline contrast/sharpening, capped scaling, and user-selected export destination.

## Implemented features

- Android Storage Access Framework image picker and save dialog; no broad storage permission is requested.
- EXIF orientation correction for rotated/flipped camera photos when metadata is available.
- Original working preview and enhanced preview with toggle.
- Fast offline contrast/sharpening mode.
- Stronger offline heuristic mode. **This is not a trained neural or generative AI model.**
- Adjustable strength; changing enhancement settings invalidates stale output so it cannot accidentally be saved.
- Standard working output (source is decoded with a maximum 2048 px long edge), 2×, 4× (output edge capped at 4096 px), and 4K long-edge output (3840 px).
- JPG, PNG, and WebP export, with stream and encoder failure handling.
- Background image decoding, processing, and saving; controls are locked while an operation is active.
- Pure Kotlin image-sizing unit tests, Android lint, and CI debug APK build.

## Build and validation

GitHub Actions runs:

1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`

The APK artifact is uploaded only if all three tasks succeed. Validation reports are uploaded even when the run fails (when reports exist).

For a local Android build, use Android Studio with JDK 17, Android SDK platform 35, and Gradle 8.9. Run `gradle testDebugUnitTest lintDebug assembleDebug` from the project root.

See [TESTING.md](TESTING.md) for the release-gate test matrix and manual device checklist.

## Important limitations

- The app uses deterministic offline filters; no neural super-resolution model is bundled.
- Upscaling enlarges the working image and does not guarantee recovery of missing details.
- To reduce memory pressure, source images are decoded to a working preview with a long edge of at most 2048 px. Output scaling is capped at 4096 px on the long edge.
- A successful CI build is not equivalent to live-device validation. Test image selection, each export format, dimensions, cancellation, and large-image behavior on a real Android device before publishing a release.
