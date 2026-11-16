# Changelog

All notable changes to Nooklin Photos, newest first. Each GitHub release has its own notes with the APK hashes: https://github.com/Nooklin/Photos/releases

## Version 1.3.2

### Albums
- Albums and folders can be sorted by Alphabet or Size, and albums also by Manual order. Manual order is set by long-pressing an album on the Albums page and dragging it. New albums go first in Manual order. The same works in the Hidden Section.
- Home shows all albums, sorted the way you choose in Settings, in 1 or 2 rows. Hidden Home has its own setting.

### General
- Vibration feedback when dragging albums, swiping to select, favoriting in the viewer and entering a PIN. It can be turned off in Appearance.

### Hidden Section
- Imported items are placed by their original date instead of the moment they were hidden, in the library, the day grouping and the calendar. Recently Saved still uses the moment they were hidden.
- New setting to hide album and folder previews. Albums and folders then show a placeholder with their name and item count.

## Version 1.3.1

### Home
- Media Types, Utilities, Albums and Discovered sit directly on the page, in the Hidden Section too. The tiles are filled without an outline, and the albums have rounder corners.
- Reorder Home Screen is now called Customize Home, on Home and in the Hidden Section.

### Folders
- Folders can go up to 6 levels deep. Importing a folder or zip keeps its structure, in the library and in the Hidden Section.

### Swipe to select
- Swiping to select keeps scrolling while you hold your finger at the top or bottom of the screen.

### Hidden Section
- Selecting in the Hidden Section works the same as in the library, including Share when it is turned on.

### Recently Deleted
- Recently Deleted has a menu with Recover All and Delete All. Selecting there works like the rest of the app.

## Version 1.3.0

### Home
- The Library card on Home scrolls sideways with the number of rows you set in Settings, Gallery. Media Types and Utilities scroll sideways in two rows.

### Panoramas
- Panoramas have their own category under Media Types.
- Panoramas are only wide landscape pictures now. Screenshots and tall strips no longer show up there.

### Automatic albums
- Albums for Camera, Screenshots, Downloads and WhatsApp that fill themselves with new photos. Hide them under Settings, Gallery, Automatic albums. An album you delete stays deleted.

### Duplicate
- Duplicate puts the copy next to the original and no longer shows the same photo twice. In the viewer you jump straight to the copy.

### Long press menu
- A long press opens a menu with a preview in the gallery, albums, search, People, Discovered, the Hidden Section and the Recycle Bin. For photos in the library it offers Select, Add to Album, Copy, Duplicate and Hide. Select starts selecting, a swipe still selects several photos, and Back first cancels the selection.
- The long press preview is sharp and no longer zooms in when it loads.

### Filter menu
- The filter menu has headings: Show and Sort.

### Sharing
- A tap on the greyed Share button explains that sharing is turned off, with a box to not show it again.

### Hidden Section
- Hide asks for access to media once. After that, hiding photos no longer shows the system confirmation.
- The Hidden Section PIN has at least 6 digits. A shorter PIN has to be replaced the next time you unlock.
- Your Hidden Section PIN is now stored with a slower, stronger hash (PBKDF2). An existing PIN keeps working and is upgraded the next time you set a PIN.
- Hidden photos are no longer cached outside their private folder while you browse them.

### Appearance
- Colorful display is now right under Theme in Settings, Appearance.

### Privacy
- App data is excluded from Android backup and device transfer.
- The video player log no longer writes the location of the file.

### Speed and smoothness
- Swiping back from a screen no longer flashes a white area.
- Scrolling and filtering large libraries puts less work on the main thread.

## Version 1.2.0

### Video editor
- Video editor: the trim bar shows a strip of frames and a line where the player is, and Styles and Crop work on videos too.
- Styles for video: the same filters as for photos, with a live preview. A filtered video is saved as a new clip.
- Crop for video: the same aspect ratios, straighten, rotate and flip as for photos. A cropped video is saved as a new clip. Perspective is for photos only.
- Video editor: the editor opens faster, the video no longer waits for the strip of frames.
- Video editor: when the video is paused, the preview shows the current frame with your style, flip, rotation and straighten applied, live while you drag. The video no longer moves. While playing, the video shows the edits itself.
- Video editor: when the video is paused, a change of style, flip or straighten plays the video muted for a moment so the edit shows in the preview.
- Video editor: the player is now prepared with the video effects already set, and rebuilt on a rotate tap, so the live preview of rotate, flip, straighten and filters can show the edits.
- Delete confirmations now say photo, video or items, matching what you delete. Inter is the default font and the first choice. In the video editor you can save over the original (the original is kept, Revert gets it back). The video editor writes diagnostic log lines while its live preview of rotate, flip, straighten and filters is being fixed.

### Photo editor
- A smaller Crop screen for photos: three round buttons (Straighten, Vertical, Horizontal) and one slider.
- Editors: the photo and video editor now share one design. The mode bar looks like the toolbar of the viewer. Crop has flip, rotate and the aspect ratio menu above the picture and a ruler below it, like the iOS Photos app. Video editor: the save menu has Save Video (the original is kept, Revert brings it back) and Save Video as New Clip, and there is one red Revert button.
- Photo editor, Crop: the photo always fills the full height or width of the screen, with a margin so the corner handles at the edges can be reached, also at the bottom. The handles react to touch in a bigger area. Switches and chips no longer show purple.

### Opening from other apps
- A photo or video opened from the camera app opens in the normal viewer: swipe through the Library and use every viewer option. Back returns to the camera app.

### Albums and folders
- Folders inside albums, for example the album Holidays with the folders Location 1 and Location 2. Deleting a folder never deletes its photos. This also works in Hidden albums.
- Album screen with a big photo at the top (the newest, or a cover photo you choose), the folders as a list, and a slideshow button.
- The slideshow shows two photos above each other now and then (60/40, 40/60 or 50/50), and the number of items of an album counts the photos in its folders too. An album with folders shows the front of its first 4 folders.
- A long press on an album in the Albums and Hidden albums overviews opens a menu: Share, Rename, New Folder, Merge and Delete. A long press on a folder in an album opens Rename and Delete. Deleting a Hidden album returns to the albums overview.
- Favorites now has the top of an album: a big photo (the newest favorite or one you choose) with the title, the count and a play button, also in the Hidden Section. The Hidden Section locks the moment you leave the app. An album with 2 or more folders always shows 4 cells, with a coloured area where there is no photo, and the photos in an album cover have 2 dp between them. Settings is more compact and the Hidden Section has its own block.

### Selecting and menus
- Swipe to select: a sideways swipe over photos in select mode selects whole rows (also moving down), swiping over selected photos deselects them again. The check mark on a selected photo has a white ring, so it shows on any photo.
- Library, albums and all photo groups: images in a grid get sharp faster, also after scrolling away and back (a started decode is no longer thrown away). While selecting you can now drag across photos in every grid, like in the Library.

### Backup and import
- Library Backup (zip, flat or organized by album) and Import folder structure now also exist for the normal library, in Settings. The Hidden backup screen got the same explanation with an example. Deleting a folder moves its photos to the album it was in. Select mode shows a red Cancel button instead of an X. The import screen scrolls to the chosen folder or zip.
- Library Backup and Import are now one Settings entry, Backup & Import. Import no longer counts leftover files as duplicates, shows Nothing imported when nothing was added, and files missing from a zip count as failed.
- Exports and backups keep the original file names and dates. Each zip has a metadata.json with the original name, modified date and capture date, and importing uses them instead of the import date. The Hidden Section remembers the original name and dates of what you hide or import, and Unhide puts them back.
- Import a folder structure (or a zip) into the Hidden Section: Settings > Hidden Section > Import folder structure. Folders become albums and folders, photos that are already hidden are skipped and listed, the original dates stay, and you can delete the originals or the zip afterwards.

### Hidden Section
- The Hidden Section has a Calendar under Utilities, and its Settings are split into Security, Display and one Backup & Import entry.
- Hidden no longer locks when you pick a file or answer a system prompt from inside the app, so importing a zip works again. It still locks the moment you leave the app.
- Hidden Home looks like Library Home: the same block margins, the round expand button in section headers, and the Hidden Library card scrolls sideways through all hidden photos. Recently Deleted is no longer cut off to Recently.

### Long press menu
- Library: long press on a photo or video shows it small with a menu (Share, Favorite, Delete, Copy, Duplicate, Hide, Add to Album); tap the card to open it. Round buttons now show press feedback only on the circle.

### Library and Home
- Settings, Gallery: a switch to show screenshots in the Library (on by default, it replaces the Screenshots item in the Library filter menu). The columns in Media Types and Utilities on Home have the same space behind the widest item.

### Viewer
- Viewer: a photo you open is sharp at once. Before, it could keep showing the small grid thumbnail until you swiped away and back.

### Settings and design
- Rows in all Settings screens are as compact as the main Settings menu.
- Explanations in the app are split into short paragraphs and lists, for example in How to, About and the Hidden Section settings. Inter is available as a font. Rows in all Settings screens have the same height as the main menu, and Backup & Import in the Hidden Section settings has spacing above it.
- Design: bigger tap areas on small buttons, a darker red for better contrast, editor choices survive Android closing the app, empty screens have a Back button, clearer error messages, and spacing, text sizes and animation times follow one scale.
- Settings: the icons sit on colored tiles, with a switch in Appearance (Colorful display) to turn it off. Videos that failed to start in the viewer now try again by themselves. A hidden PNG or WebP stays that format after editing. The share cache no longer removes a file another app may still be reading.

## Version 1.1.0

### Library and Home
- Timeline bar in the Library: Years, Months and All.
- Calendar view, under Utilities on Home.

### Machine Learning
- Duplicates review: visually similar photos in groups, keep one and clean up the rest. Switch on Duplicates under Machine Learning first.

### Selecting and menus
- Select and a menu in every group of photos: Media Types, Utilities, albums, the Albums overview, Hidden albums, Search and pets.
- The Library style for Select, menus and round buttons on every screen with a group of photos or albums, with icons in the menus.

### Sharing
- Remove metadata when sharing: all Exif, XMP and IPTC data from JPEG photos, videos are copied without their tags, and you choose what is removed (Location, Camera and device, Date and time, Description and other).
- Share sheet: a row with the apps that accept the share, Print and Use as Wallpaper for one photo, and Cancel while a share is being prepared.

### Hidden Section
- Hidden Recently Deleted stays private inside the app.

### Opening from other apps
- Open photos and videos from other apps, and choose a photo or video for other apps.

### General
- English and Dutch. Pick a language under Settings, Appearance.
- A new How to use page, as a tree that is closed by default.
- Settings are grouped in blocks, and the version and this change log are in Settings, About.
- A new app icon.
