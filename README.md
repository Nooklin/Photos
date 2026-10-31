# Nooklin Photos

A privacy-first, open-source photo gallery for Android. No Google Play Services, no internet permission, no telemetry.

Part of the Nooklin FOSS Android suite. License: GPL-3.0. Fonts: Work Sans and Inter (SIL Open Font License 1.1, see THIRD-PARTY-LICENSES).

## Version 1.2.0 change log

### Editing
- Video editor: the trim bar shows a strip of frames and a line where the player is, and Styles and Crop work on videos too.
- Styles for video: the same filters as for photos, with a live preview. A filtered video is saved as a new clip.
- Crop for video: the same aspect ratios, straighten, rotate and flip as for photos. A cropped video is saved as a new clip. Perspective is for photos only.
- A cleaner Crop screen for photos and videos like on iOS: rotate, flip and aspect ratio in a row above the picture, and a ruler below for Straighten (and Vertical and Horizontal for photos).
- Photo and video editor share one design: the same icon bar as the viewer, the same red Revert button.
- Video editor: the preview shows a paused still frame with the edits live. The Save menu has Save Video (over the original, Revert restores it) and Save Video as New Clip. Revert brings the video back to the original, whatever was changed (trim, filter or both).

### Library and Home
- A photo or video opened from the camera app opens in the normal viewer: swipe through the Library and use every viewer option. Back returns to the camera app.
- Folders inside albums, for example the album Holidays with the folders Location 1 and Location 2. Add one with New Folder in the album menu. Add photos with Add to Album, where a folder is named Album / Folder. Deleting a folder, or an album with folders, asks first and never deletes photos. Also in Hidden albums.
- Album screen with a big photo at the top (the newest, or a cover photo you choose in the album menu with Choose cover photo), the folders as a list, and a play button for a full screen slideshow of the photos (slow zoom in and out, a dissolve between photos, now and then two photos above each other, a tap shows Back, Pause and a strip with all photos in order). The menu items are Rename Album and Rename Folder. Also in Hidden albums. The count of an album includes the photos in its folders, and an album with folders shows the front of its first 4 folders.
- A long press on an album in the Albums and Hidden albums overviews opens a menu with Share, Rename, New Folder, Merge and Delete, and a long press on a folder opens Rename and Delete.
- Inter (SIL Open Font License) as extra font, explanations split into short paragraphs and lists, the same row height in all Settings screens.
- Exports and backups keep the original file names and dates (metadata.json in every zip). The Hidden Section remembers the original name and dates, and Unhide restores them.
- Favorites has the top of an album (big photo, title, count, play button), also in Hidden. Hidden locks the moment you leave the app. An album with 2 or more folders always shows 4 cells. Settings is more compact and the Hidden Section has its own block.
- The Hidden Section has a Calendar (under Utilities) and its Settings are grouped: Security, Display, Backup & Import.
- Library Backup (a zip of the library, flat or organized by album and folder) and Import folder structure for the normal library, in Settings, under one entry Backup & Import. Import ignores leftover files when checking duplicates. A deleted folder gives its photos to the album it was in. In select mode the X is now a red Cancel button.
- Swipe to select: a sideways swipe over photos in select mode selects whole rows (also moving down) and swiping over selected photos deselects them. The check mark on a selected photo has a white ring.
- Import a folder structure (or a zip) into the Hidden Section, from Settings > Hidden Section > Import folder structure. Folders become albums and folders, photos that are already hidden are skipped and listed, the original dates stay, and the originals or the zip can be deleted afterwards.
- Long press on a photo or video in the Library shows it small with a menu: Share, Favorite, Delete, Copy, Duplicate, Hide and Add to Album. Tap the photo to open it. (Long press used to toggle Favorite.)
- Design: bigger tap areas on small buttons with press feedback only on the circle, a darker red for better contrast, editor choices survive Android closing the app, empty screens have a Back button, clearer error messages.

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
- Photo editor: rotate, flip, straighten, perspective, crop, styles and adjustments
- Video editor: trim with a strip of frames, the same styles and crop (without perspective); a filter or crop is saved as a new clip
- Photo info sheet with EXIF data
- Search by file name
- Duplicates review: photos that look the same are grouped, you keep one and the rest go to Recently Deleted (needs Machine Learning, Duplicates, and a library scan)
- Share sheet for several photos: a row of apps that accept the share, save to a folder, export the unmodified originals, copy, and for one photo also Print and Use as Wallpaper; Cancel stops a share that is still being prepared
- Metadata removal when sharing (choose what is removed: location, camera and device, date and time, description and other)
- Calendar view and a Years, Months, All timeline in the Library
- Select and a menu in every group of photos and albums (Media Types, Utilities, albums, the Albums and Hidden albums overviews, Search, pets), all in the same style
- Open photos and videos from other apps (a library photo or video opens in the normal viewer), and pick a photo or video for other apps
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
- A video with a filter or crop is encoded again as H.264 and saved as a new clip. This can take a while for long videos, and some videos (for example 10-bit HDR) may not export on every phone.
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
