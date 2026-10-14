# Nooklin Photos

A privacy-first, open-source photo gallery for Android. No Google Play Services, no internet permission, no telemetry.

Part of the Nooklin FOSS Android suite. License: GPL-3.0.

## Version 1.1.0 change log

### Library and Home
- Timeline bar in the Library: Years, Months and All.
- Calendar view, under Utilities on Home.

### Selecting and menus
- Select and a menu in every group of photos: Media Types, Utilities, albums, the Albums overview, Hidden albums, Search and pets.
- The same style for Select, menus and round buttons on every screen with a group of photos or albums, with icons in the menus.

### Sharing
- Remove metadata when sharing: all Exif, XMP and IPTC data from JPEG photos, videos are copied without their tags, and you choose what is removed (Location, Camera and device, Date and time, Description and other).
- Share sheet: a row with the apps that accept the share, Print and Use as Wallpaper for one photo, and Cancel while a share is being prepared.

### Machine Learning
- Duplicates review: visually similar photos in groups, keep one and clean up the rest. Switch on Duplicates under Machine Learning first.

### Hidden Section
- Hidden Recently Deleted stays private inside the app.

### General
- Open photos and videos from other apps, and choose a photo or video for other apps.
- English and Dutch. Pick a language under Settings, Appearance.
- A new How to use page, as a tree that is closed by default.
- Settings are grouped in blocks, and the version and this change log are in Settings, About.
- A new app icon.

## Privacy

- The app requests no internet permission, so it cannot send anything off the device.
- No analytics, crash reporting or telemetry of any kind.
- No Google Play Services dependency.
- Face and pet analysis runs fully on the device, using bundled models.
- Photos leave the device only when you share, print or send them to another app yourself.

## Features

- Photo and video grid with a full-screen viewer (swipe, pinch-zoom, video playback)
- Albums, favorites, screenshots, videos, panoramas
- Recently Deleted with 30-day expiry
- Hidden folder, locked with your device biometric or PIN, with its own private Recently Deleted
- Photo editor: rotate, flip, crop, styles and adjustments; video trimming
- Photo info sheet with EXIF data
- Search by file name
- Duplicates review: photos that look the same are grouped, you keep one and the rest go to Recently Deleted (needs Machine Learning, Duplicates, and a library scan)
- Share sheet for several photos: a row of apps that accept the share, save to a folder, export the unmodified originals, copy, and for one photo also Print and Use as Wallpaper; Cancel stops a share that is still being prepared
- Metadata removal when sharing (choose what is removed: location, camera and device, date and time, description and other)
- Calendar view and a Years, Months, All timeline in the Library
- Select and a menu in every group of photos and albums (Media Types, Utilities, albums, the Albums and Hidden albums overviews, Search, pets), all in the same style
- Open photos and videos from other apps, and pick a photo or video for other apps
- Optional on-device People grouping (off by default)
- Light, dark or system theme; square or aspect-ratio grid
- Available in English and Dutch; follows the system language or can be set per app

## What you need to do yourself

Some features do nothing until you set them up:

- **People:** open Settings, Machine Learning, switch on "Use machine learning", make sure People is on under "Tags to scan", then run the library scan from the Scan section. After the scan you can give each person a name.
- **Duplicates:** switch on Duplicates under Tags to scan in Settings, Machine Learning, and run the library scan. Duplicates then appears under Utilities on Home.
- **Cars and Motorcycles:** the same scan finds these, no further steps.
- **Scan finished notification (optional):** switch it on in Settings, Notifications, and allow notifications when Android asks.
- **Language:** the app follows the system language. To pick one yourself, open Settings, Appearance, Language.
- **Hidden section:** it is locked with your phone's own biometric or screen lock, so set one up in Android first.

## Known limits

- RAW photos can be viewed where Android can decode them, but not edited.
- Search matches file names only.
- Duplicates are matched by a visual fingerprint (a perceptual hash), so edited or very similar photos can be grouped. You always choose what to keep.
- Panoramas are detected by aspect ratio, which is a heuristic.
- Print and Use as Wallpaper work on one photo at a time, not on videos or several photos. Use as Wallpaper hands the photo to an app you choose.
- The apps row in the share sheet lists the apps that accept a share. The app asks for no permission to see all apps, it only asks Android which apps take an image or video share.
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
