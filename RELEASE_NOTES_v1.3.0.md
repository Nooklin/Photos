# Nooklin Photos 1.3.0

## What is new

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

## Verify

- File: photos-v1.3.0.apk
- APK SHA-256: `<paste Get-FileHash output>`
- Signing certificate SHA-256: `<paste from apksigner verify --print-certs>`

Requires Android 13 or newer.
