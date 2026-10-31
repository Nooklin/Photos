# Nooklin Photos: SCOPE (backlog and status)

Updated 2026-10-25. Items 1 to 7 below were built between v135 and v148, item 12 in v172 to v174 (see Photos/TODO.md for versions and what is not tested). Nothing is compiled or tested on a device by Claude, the user builds in Android Studio.

Conditions for every item: no INTERNET permission, no Google Play Services, no telemetry, no new component style (reuse GlassPill / GlassBlock). Only add features that are fully built, no "coming soon" items in the app.

## Order
1. Multiple languages: built (v135, EN + NL)
2. Open from other apps / default gallery: built (v136)
3. Improve metadata stripping: built (v148), per-field choice not built
4. Visual duplicates: built (v147)
5. Timeline zoom (Years / Months / All bar): built (v139 to v143). A side year scrubber was not built.
6. Calendar view: built (v146)
7. Select and menu in every group of photos, How to use as a tree: built (v144, v145)
8. Widget
9. Motion photos, Ultra HDR, RAW
10. OCR, then search by description (both need research into offline models first)
12. Import a folder structure into the Hidden Section: built (v172 to v174), for the normal library built in v177. Library Backup built in v177.

## 1. Multiple languages: built
- All UI text is in `values/strings.xml` and `values-nl/strings.xml`. Language picker in Settings, Appearance, follows the system by default.
- Some data-layer strings are still English (AlbumRepository, zip paths, ML logs).

## 2. Default gallery app and opening from other apps: built, not tested on a device
- VIEW (content:// image and video, one file), PICK and GET_CONTENT (single or multiple). OPEN_DOCUMENT and file:// are not supported.
- Uncertain: whether Android offers the app as default gallery depends on the device maker.

## 3. Metadata stripping when sharing: built in v148, not compiled or tested
- JPEG: all metadata segments are cut out without recompressing (Exif, XMP, IPTC, comments, maker notes, the embedded thumbnail and anything after the image end, such as the video of a motion photo). Colour profile and orientation are kept. The algorithm was tested in Python on synthetic JPEGs (normal and progressive), the Kotlin version is not compiled.
- Video: copied into a new MP4 without recompressing (MediaExtractor and MediaMuxer), so location and other tags are not carried over. Other tracks (for example motion data) are dropped. Fails closed: if a video cannot be copied it is not shared. Not tested on a device.
- HEIC: shared as JPEG when stripping is on (re-encoded, no metadata). PNG and WebP: the old tag removal (XMP not covered).
- Not built: choosing per field what is stripped. It needs a design decision on the screens (Settings and the share sheet).

## 4. Visually similar duplicates: built in v147
- Detection already existed: dHash on a thumbnail (`ml/ImageHashing.kt`), Hamming distance 5 or less, stored in Room, opt-in under Settings, Machine Learning, Duplicates, filled by the library scan.
- Now grouped (photos linked through a chain of matches share one group) in a review screen: per group a photo to keep (default most pixels, then biggest file), the rest go to Recently Deleted. Also "Clean up all" with a confirmation.
- Uncertain: the threshold of 5 is a judgment, not measured.

## 5. Timeline with zoom: built
- Years (2 columns), Months (3 columns) and All, bar appears while scrolling. Not built: side year scrubber, zoom animation between the views, pinch to switch views.

## 6. Calendar view: built in v146
- Month per page, swipe sideways, first photo of the day as the cover, tap opens that day in the viewer. Entry on Home under Utilities (reorderable).

## 7. Select in every group of photos: built in v144
- Media Types and Utilities (Favorites, Recently Saved, Duplicates), album detail, Search and pet detail now have Select and a menu (Share, Favorite, Add to Album, Move to Recently Deleted). The Albums overview has no select by design.

## 8. Home screen widget
- Local: photos from a chosen album or favorites.
- Open: Glance or RemoteViews, and whether the widget needs no background rights or extra permissions (otherwise it does not fit the principles). Research first.

## 9. Motion photos, Ultra HDR, RAW/DNG preview
- Motion photos: recognise the video part and play it in the viewer.
- Ultra HDR: HDR display on supported screens (Android 14+ supports it, minSdk 33 so a version check is needed).
- RAW/DNG: now "view where Android can decode". Goal: reliable preview, editing stays out of scope.
- Uncertain: how much Coil 3 and the Android decoder already handle. Research first, then plan.

## 10. Read text in a photo (OCR)
- Goal: select and copy text from a photo, fully offline.
- Open: which model (must be Apache 2.0 or similar, no Google Play parts). ML Kit is out because of Google. Not researched yet.
- Heavy: weigh model size and APK size.

## 11. Search by description ("dog on the beach")
- Goal: semantic search on the content, fully offline.
- Approach (still to research): an image-text embedding model (CLIP-like) via LiteRT, store embeddings per photo in Room, search with vector comparison.
- Open: model licence and size, scan time for large libraries, opt-in like People.
- Link with item 10: they share the scan pipeline and the settings under Machine Learning.

## 12. Import a folder structure into the Hidden Section: built in v172 to v174, not compiled or tested
- Decided by the user on 2026-10-25. Own row in Settings, Hidden Section ("Import folder structure"), behind the same three step gate as the backup (the Hidden Section is locked first, so you always log in again).
- Source: a folder (system folder picker) or a zip (for example from the organized export). A scan shows the albums and folders with counts and a checkbox each before anything is copied.
- Mapping: first folder level = album, second level = folder in that album, deeper levels join that folder, loose files without album. In a zip "Hidden Images" means loose and "Hidden Albums" is the album level.
- Only photos and videos, the original file date is kept (hiddenAt and dateAdded).
- Album with the same name already exists: one question for all of them, Merge or New name ("Name 2").
- Duplicates: SHA-256 of the content against everything hidden (also the recycle bin) and against earlier files of the same import. Skipped and listed afterwards.
- Delete afterwards: a folder source has a switch (originals deleted permanently, no recycle bin). A zip asks after the import "Delete the zip file?" with a permanent warning, only when nothing failed. The zip picker asks write access for this.
- Library version (v177, merged in v178): Settings has one entry "Backup & Import" with "Library Backup" and "Import folder structure" for the normal library, same screens and rules. Import creates new MediaStore items (Pictures/ or Movies/, original date as DATE_TAKEN and sort date) and the album database rows; duplicates are found by hashing every library file first (slow for big libraries). Backup writes a zip: flat (`Library/`) or organized (`Albums/Album/Folder/`, `Library/` for photos in no album); a zip with those folder names imports back into the same albums. Both share one import engine (`ImportSink` in `data/HiddenImport.kt`, `data/LibraryTransfer.kt`).
- v178: duplicate check only uses files with a database row (leftovers are deleted), 0 imported shows "Nothing imported", files missing from a zip count as failed.
- v179: Hidden Section has a Calendar under Utilities (same screen as the library, day = date hidden or original date after import). Settings, Hidden Section: toggle on top, groups Security (Extra PIN), Display (Hidden albums, Image sharing), one Backup & Import entry.
- v180: Favorites (library and Hidden) has the album top: big photo (newest favorite or chosen, stored in `LocalMetadataStore`), title, count, play button. Hidden locks at once on `onUserLeaveHint`, system screens keep the 15 s grace. Album cover collage: 2 dp gaps, an album with 2 or more folders always shows 4 cells (plain accent area where there is no photo). Settings spacing smaller, Hidden Section own block. v181: the same row height in all Settings screens (`SettingsToggleRow`, `SettingsRadioRow`, About).
- v182: Inter variable font (`res/font/inter.ttf`, latin subset, OFL in THIRD-PARTY-LICENSES/inter, extra choice in Appearance, not the default). Rows with a Switch or RadioButton no longer get the 48 dp touch target, so all Settings rows are 44 dp. 36 long explanation texts (EN and NL) split into paragraphs, lists and steps; legal texts only in paragraphs. The How to screen keeps bullet lines whole.
- v183: original names and dates. `hidden_media` (HiddenDatabase v6, migration 5 to 6) has `originalName`, `originalModified`, `originalTaken`, filled in `copyToHidden` and `HiddenImportSink.store`; older hidden items have none and export with their internal name. Every export zip (library and Hidden, flat, organized, selected) starts with `metadata.json` (`files`: entry path to name, modified ms, taken ms, added seconds); `scanImportZip` reads it and `ImportEntry` carries name, modified, taken and added, so import sets DATE_TAKEN, DATE_MODIFIED and the sort date from the original values. Entry names only replace characters a zip cannot hold with `_`, same name in one directory gets " (2)". Unhide uses the original name (when an edit changed the format only the extension follows), sets DATE_TAKEN and DATE_MODIFIED, and Android adds " (1)" for a name that exists. `MediaItem.dateTaken` is read from MediaStore (DATE_TAKEN). DATE_ADDED cannot be set by an app, the sort date covers that. Not verified on a device.
- v184: `onUserLeaveHint` also fires when the app opens a picker or prompt itself, which locked Hidden before an import could start. `MainActivity` overrides `startActivityForResult` and `startIntentSenderForResult` to set `startedByApp`; `onStop` locks only when `leftByUser && !startedByApp`. Not verified on a device (also check export destination, delete prompt, biometrics).
- v185: `HiddenHomeScreen` keeps local copies of `CategoryRow`, `GlassBlock` and `SectionHeader` (the Home ones are private) and they had drifted. Now equal: category rows `widthIn(min = 180.dp)` with `softWrap = false` (fixes "Recently Deleted" cut to "Recently"), blocks 2 dp from the edge, round 32 dp expand button. The Hidden Library card is a `LazyHorizontalGrid` over all hidden items with a `LazyGridState` (like Library Home), the old preview window (`previewWindowStartRow`) is no longer used there. Hidden Home still uses 3 preview rows, not the Library Home setting. Not verified on a device.
- v186: the Dutch changelog string of v185 had an unescaped apostrophe, which failed `mergeDevResources`; fixed. Rule for strings: every apostrophe in a string resource is written as backslash plus apostrophe.
- v187: `deleteWording(videoFlags)` in `ui/common/DeleteWording.kt` gives the text and the red button of a delete confirmation for photo, video, videos, photos or items (8 call sites, EN and NL); album and folder texts say items. Inter is the default font for everyone: the setting moved to the key `app_font_v2` so the earlier choice is ignored once, and Inter is listed first. Video editor: Save over Original (`VideoEditExporter.exportToFile`, `MediaStore.createWriteRequest`, original kept in `OriginalsStore` with key `media_<id>`, red Revert pill when an original exists). The export with edits works, the live preview effects (`player.setVideoEffects`) do not: v187 only adds a seek after setting the effects, a fallback for the picture size from `onVideoSizeChanged`, and log lines (tags VideoTrimScreen and VideoEditing). Not verified on a device. Alternative if it stays broken: preview the edits in Compose (`graphicsLayer` and a colour filter) instead of Media3 effects.
- v188: video editor preview, second attempt. v187 log (Pixel 10 Pro XL, Android 37, HEVC 2160x3840): the effect lists were built on every rotate tap but the player never reported a new video size, so `setVideoEffects` on a prepared player did not change the picture (not even with Media3's own `ScaleAndRotateTransformation`). Now `ExoPlayer` is created without `prepare()`, and `VideoTrimScreen` prepares it after the picture size is known with the effects set first; a rotate tap does pause, stop, `setVideoEffects`, seek back, prepare. If the picture still does not change: preview the edits in Compose (`graphicsLayer` and a colour filter) instead. Not verified on a device.
- v189: v188 log and test: rotation works after the prepare-with-effects change; flip, straighten and style only show while the video plays (a paused player draws no new frame, a seek to the same place does not either). `VideoTrimScreen` now plays muted for 150 ms after such an edit (`peeking`, `peekReturnMs`) and seeks back, a newer edit takes over. Not verified on a device.
- v190: removed the muted peek of v189 (it moved the video). When paused and an edit is active (style, flip, rotation, straighten; crop alone excluded) `VideoTrimScreen` grabs the frame at the current position with `MediaMetadataRetriever` (about 1080 px high, polled every 100 ms for seeks) and shows it as an `Image` over the player with `graphicsLayer` (rotate, flip, straighten with the export's fill scale) and `ColorFilter`. The previous frame stays while a new one loads; on a grab error the old frame or the player stays. While playing the Media3 effects are shown. Export unchanged. Not verified on a device.
- v191: opening the video editor took about 4.5 s on a 4K HEVC clip (log: silence between the player init and `prepare`) because `metadataDone` was set after the 10 frame grabs. It is now set right after the picture size is read, so `prepare` starts at once and the strip fills in meanwhile. The video still plays only after the play button. Not verified on a device.
- v192: design audit against the app design guide (30 Oct 2026), see AUDIT.md. New: `ui/theme/Tokens.kt` (Spacing, AppText, Motion, AppColors, MinTouchTarget); `ui/common/EditorChrome.kt` (`EditorModeBar` like the viewer toolbar, `EditorDestructivePill`), `TouchTarget.kt` (`Modifier.touchTarget()`), `EmptyState.kt`. Editors: both use `EditorModeBar` (icons only, like the viewer) and the iOS-like Crop layout (`CropTopRow` above the picture: rotate, flip, aspect ratio menu; `GeometryTools` with a ruler below, straighten only in the video editor). The Crop picture area is 56 dp shorter in Crop. Video editor menu: Save Video (the former Save over Original, disabled while nothing changed) and Save Video as New Clip; one red Revert button (without a stored original it only clears a trim saved by an older version, `VideoTrims` stays for that). Four unused strings removed. Applied: 48 dp tap areas (Home and Hidden Home chevrons, People plus, Discovered quick add, color swatches, video control bar 44 to 48), `Role.Button` on 58 clickables, editor state in `rememberSaveable` (modes, filter, rotation, straighten, perspective, flips, aspect preset; not the crop rectangle or the Adjust values), `EmptyState` with a Back button on 10 empty screens, clearer error toasts, red `#D70015` (5.4 to 1 with white) for buttons with white text and for destructive text on light grounds, hint text alpha 0.6 to 0.7, tween durations to `Motion` (slideshow keeps 600 and 700 ms on purpose), off-grid padding (5, 6, 10, 14, 20, 28 dp) mapped to the grid, text sizes (11 and 12 to 12, 13 and 14 to 14, 15 and 17 to 16, 28 and 30 to 28), color literals to `AppColors`. Not done on purpose: haptics (long press already gives it), the 1 dp hairlines, `Color.White` and `Color.Black` over photos, the Black mode (user choice), the user color palette in `AppPreferences`. Not verified: it was not compiled or run on a device.
- v193: Library long press opens a preview (`ui/gallery/PhotoPreviewMenu.kt`): the photo small on a dimmed background (a video shows its first frame), tap the card to open the viewer, tap outside to close. Menu: Share, Favorite, Delete (through the existing Recycle Bin confirm), Copy, Duplicate, Hide, Add to Album. Copy and Share follow the sharing setting, Hide follows the Hidden section setting. This replaces long press = toggle favorite in the Library; other grids keep long press selection. Round buttons (Home and Hidden Home chevrons, People plus, Discovered quick add, color swatches): the 48 dp tap area stays but press feedback shows only on the circle (`TouchTargetBox` in `TouchTarget.kt`). Not verified: it was not compiled or run on a device; card and menu fit on small phones and with large system text.
- Not built: a list of files that failed, a background service for long imports.

## Album screen details (v166 to v171)
- Album and folder screens: big photo at the top, folders as a list, play button. The count of an album includes its folders (distinct photos), an album with folders shows the front of its first 4 folders. Long press on an album in the Albums and Hidden albums overviews opens a menu (Share, Rename, New Folder, Merge, Delete; Hidden has no Share and no Merge).
