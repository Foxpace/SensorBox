# SensorBox Google Play submission pack

This directory is the reviewable source for the SensorBox Google Play listing. It covers the phone and Wear OS apps published under `com.tomasrepcik.sensorbox`.

The pack is not ready for submission yet. The copy and graphics match the current code, but publisher placeholders, the live Play Console questionnaires, and the final signed bundles still need review.

## Prepare the assets

Requirements:

- macOS `sips`
- ImageMagick `magick`
- current screenshot-test references in `app/src/screenshotTestDebug/reference` and `wear/src/screenshotTestDebug/reference`

From any directory, run:

```sh
/absolute/path/to/SensorBox/store/google_play/prepare.sh
```

The script creates `assets/`, validates every generated image, and updates the matching Fastlane image directories.

Before regenerating graphics after a UI change, run:

```sh
./gradlew :app:validateDebugScreenshotTest :wear:validateDebugScreenshotTest
```

## Release evidence

- Phone application ID: `com.tomasrepcik.sensorbox`
- Phone version in source: `87`, `5.0.0-dev`
- Wear version in source: `1000049`, `4.0.0-dev`
- Phone minimum and target SDK: 24 and 37
- Wear minimum and target SDK: 26 and 37
- Release entry point: `./release_android.sh`
- Native bundle tasks: `./gradlew :app:bundleRelease` and `./gradlew :wear:bundleRelease`

The release script follows the Bookish environment-secret workflow. It builds the phone and Wear OS bundles with one generated release name and separate generated version codes, signs both with the upload key, and verifies both signatures. Fastlane then creates separate Google Play edits for the phone and Wear form-factor tracks. The defaults are draft releases on `internal` and `wear:qa`. Metadata, graphics, screenshots, and changelogs are skipped.

The uploads are sequential. If Google Play accepts the phone edit and rejects the Wear edit, the phone draft may already exist and should be reviewed in Play Console before retrying.

Required environment variables:

- `ANDROID_GRADLE_ALIAS`
- `ANDROID_GRADLE_BASE64_JKS`
- `ANDROID_GRADLE_KEY_PASSWORD`
- `ANDROID_GRADLE_KEYSTORE_PASSWORD`
- `ANDROID_SUPPLY_BASE64_SECRET`

Validate the complete release with Google Play without committing it:

```sh
./release_android.sh --validate-only
```

## Pack files

- `listing-en-GB.md` contains the exact English listing copy and character counts.
- `play-console-answers.md` contains draft declarations and reviewer instructions.
- `asset-notes.md` records upload order, alt text, provenance, and rights.