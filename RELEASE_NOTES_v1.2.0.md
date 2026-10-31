# Nooklin Photos 1.2.0

## What is new

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
- Favorites has the top of an album (big photo, title, count, play button), also in Hidden. Hidden locks the moment you leave the app. An album with 2 or more folders always shows 4 cells. Settings is more compact and the Hidden Section has its own block.
- The Hidden Section has a Calendar (under Utilities) and its Settings are grouped: Security, Display, Backup & Import.
- Library Backup (a zip of the library, flat or organized by album and folder) and Import folder structure for the normal library, in Settings, under one entry Backup & Import. Import ignores leftover files when checking duplicates. A deleted folder gives its photos to the album it was in. In select mode the X is now a red Cancel button.
- Swipe to select: a sideways swipe over photos in select mode selects whole rows (also moving down) and swiping over selected photos deselects them. The check mark on a selected photo has a white ring.
- Import a folder structure (or a zip) into the Hidden Section, from Settings > Hidden Section > Import folder structure. Folders become albums and folders, photos that are already hidden are skipped and listed, the original dates stay, and the originals or the zip can be deleted afterwards.
- Exports and backups keep the original file names and dates: every zip gets a metadata.json (original name, modified date, capture date) and importing uses it, never the import date. The Hidden Section stores the original name and dates when you hide or import, exports with them, and Unhide restores them. Only characters a zip cannot hold are replaced by `_`, two files with the same name in one directory get " (2)". Items hidden before this version keep their internal name.
- Hidden no longer locks when the app itself opens a file picker or system prompt (import of a zip works again); it still locks at once when you leave the app.
- Hidden Home matches Library Home: same block margins, round expand button in section headers, a Hidden Library card that scrolls sideways through all hidden photos, and "Recently Deleted" is no longer cut off.
- Delete confirmations say photo, video or items to match what is deleted, also in album and folder texts. Inter is the default font for everyone and the first choice.
- Long press on a photo or video in the Library shows it small with a menu: Share, Favorite, Delete, Copy, Duplicate, Hide and Add to Album. Tap the photo to open it. (Long press used to toggle Favorite.)
- Design: bigger tap areas on small buttons with press feedback only on the circle, a darker red for better contrast, editor choices survive Android closing the app, empty screens have a Back button, clearer error messages.

## Verify

- File: photos-v1.2.0.apk
- APK SHA-256: `<paste Get-FileHash output>`
- Signing certificate SHA-256: `<paste from apksigner verify --print-certs>`

Requires Android 13 or newer.
