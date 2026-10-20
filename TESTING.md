# Smart Enhancer Ultra 4K — QA and release gate

This checklist is a test plan, not a claim that device tests have already passed. Mark a test PASS only after recording the actual result.

## Automated CI gate

- [ ] `testDebugUnitTest` passes.
- [ ] `lintDebug` passes without unresolved errors.
- [ ] `assembleDebug` passes on JDK 17 / Gradle 8.9 / Android SDK 35.
- [ ] CI uploads validation reports.
- [ ] CI uploads `app-debug.apk` only after all required tasks pass.
- [ ] Verify the APK package name is `com.anb.smartenhancer` and version is `1.3.0` / versionCode `2`.

## Image-sizing unit coverage

`ImageSizingTest` checks:

- Standard working-size dimensions.
- Power-of-two sampling for ordinary and very large dimensions.
- Boundary case at 4097 px where sample size must increase to keep the decoded long edge within 2048 px.
- Invalid dimensions are rejected.
- 2× dimensions when under the cap.
- 4× output capped at a 4096 px long edge.
- Landscape and portrait 4K output with a 3840 px long edge.

A separate local JVM smoke/property run should also cover random dimensions and `Int.MAX_VALUE` metadata inputs.

## Manual Android-device matrix

### Import and preview

- [ ] Open JPEG, PNG, and WebP images from the system document picker.
- [ ] Open a camera photo with EXIF 90°/270° orientation and verify it appears upright.
- [ ] Open a mirrored/flipped EXIF image if available.
- [ ] Try a corrupt/unsupported image and confirm an error is shown without a crash.
- [ ] Verify the working preview long edge does not exceed 2048 px.

### Enhancement and state

- [ ] Run Fast Enhance at 10%, 65%, and 100% strength.
- [ ] Run the stronger heuristic mode and verify the UI clearly says it is not neural AI.
- [ ] Toggle original/enhanced repeatedly.
- [ ] Change mode, strength, or scale after enhancement; confirm the old result is cleared and Save is disabled.
- [ ] Reset; confirm both previews and result are cleared and settings return to defaults.
- [ ] Rapid taps do not start overlapping operations.

### Dimensions

- [ ] Standard output retains working dimensions.
- [ ] 2× output reaches 2× where below the cap.
- [ ] 4× output is capped at 4096 px on the long edge; UI reports actual dimensions.
- [ ] 4K output has a 3840 px long edge, with aspect ratio preserved within rounding.
- [ ] Verify portrait and landscape images.

### Export

- [ ] Save JPG and open the saved image in Gallery/Photos.
- [ ] Save PNG and open the saved image.
- [ ] Save WebP and open the saved image on the target Android version.
- [ ] Confirm each file is non-empty and matches the selected format.
- [ ] Cancel the save dialog; app returns to usable state and does not claim success.
- [ ] Simulate unavailable/invalid destination if possible; failure is shown and no stale success message remains.

### Reliability and compatibility

- [ ] Test a large camera image and a large panorama.
- [ ] Repeat enhance/save at least 10 times; watch for crashes and memory pressure.
- [ ] Background and foreground the app during idle and after a completed operation.
- [ ] Test on Android 8/API 26 or another minimum-supported device if available.
- [ ] Test on Android 15/API 35 for status/navigation bar inset layout.
- [ ] Confirm no stale output can be saved after a processing error.

## Release decision

Do not call the release verified until the CI gate is green, the APK artifact is checked, and manual device tests are recorded. A green build alone does not prove export correctness or memory safety on every device.
