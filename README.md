# Smart Enhancer Ultra 4K — v1.2

## v1.2 upgrades
- Image processing runs on a background worker thread, keeping the UI responsive.
- Processing indicator and disabled action buttons prevent duplicate jobs.
- More defensive output sizing to reduce integer overflow risk.
- Maximum output long edge capped at 4096 px to reduce memory pressure.
- Save remains disabled until an enhanced result exists.

## Features
- Android image picker and side-by-side preview
- Toggle original/enhanced preview
- Fast contrast + sharpening filter
- Stronger offline heuristic "AI Enhance" mode
- Adjustable enhancement strength
- Original, 2×, 4× and 4K long-edge output
- JPG, PNG, WebP export
- Reset controls
- GitHub Actions APK workflow

## Build APK from your phone
1. Extract this ZIP.
2. Upload the folder contents to the root of a GitHub repository.
3. Open **Actions** → **Build Android APK** → **Run workflow**.
4. Download `SmartEnhancerUltra4K-debug-apk` from the successful workflow run.
5. Extract and install `app-debug.apk`.

## Important limitation
This project does not yet include a trained AI super-resolution model. The "AI Enhance" mode is a stronger offline filter, not generative or neural AI. True restoration requires a properly licensed model and Android inference runtime. Build has not been run/verified in this environment.
