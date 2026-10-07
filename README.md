# Nooklin Photos

A privacy-first, open-source photo gallery for Android. No Google Play Services, no internet permission, no telemetry.

Part of the Nooklin FOSS Android suite. License: GPL-3.0.

## Privacy

- The app requests no internet permission, so it cannot send anything off the device.
- No analytics, crash reporting or telemetry of any kind.
- No Google Play Services dependency.
- Face and pet analysis runs fully on the device, using bundled models.
- Photos leave the device only through the Android share sheet.

## Features

- Photo and video grid with a full-screen viewer (swipe, pinch-zoom, video playback)
- Albums, favorites, screenshots, videos, panoramas
- Recently Deleted with 30-day expiry
- Hidden folder, locked with your device biometric or PIN
- Basic editor: rotate and flip
- Photo info sheet with EXIF data
- Search by file name
- Duplicates finder (exact file size match)
- Optional on-device People grouping (off by default)
- Light, dark or system theme; square or aspect-ratio grid

## What you need to do yourself

Some features do nothing until you set them up:

- **People:** open Settings, Machine Learning, switch on "Use machine learning", make sure People is on under "Tags to scan", then run the library scan from the Scan section. After the scan you can give each person a name.
- **Cars and Motorcycles:** the same scan finds these, no further steps.
- **Scan finished notification (optional):** switch it on in Settings, Notifications, and allow notifications when Android asks.
- **Hidden section:** it is locked with your phone's own biometric or screen lock, so set one up in Android first.

## Known limits

- RAW photos can be viewed where Android can decode them, but not edited.
- Search matches file names only.
- Duplicates are matched by file size, so different photos with the same size can appear together.
- Panoramas are detected by aspect ratio, which is a heuristic.
- Scrolling in very large libraries (10k+ photos) has not been tested yet.
- Release builds are not minified (R8 is off).

## Build

Requires Android Studio with JDK 17 or newer, Android 13 (API 33) or newer to run.

    .\gradlew assembleDebug

For a signed release, copy `keystore.properties.example` to `keystore.properties`, fill it in, then run `.\gradlew assembleRelease`. Never commit the keystore or `keystore.properties`.

## Verify a release APK

    apksigner verify --print-certs photos-v1.0.0.apk

Compare the certificate SHA-256 with the value in the release notes, and check the file hash with `Get-FileHash photos-v1.0.0.apk` (PowerShell) or `sha256sum photos-v1.0.0.apk`.

## Issues and feature requests

Use the GitHub issue tracker: https://github.com/Nooklin/Photos/issues
