# BluStream Changelog

## v2.0.0-alpha

BluStream 2.0 Alpha focuses on profiles, safer add-on handling, kids browsing and cleaner navigation.

### Added

- Face-style profile avatars
- Automatic profile selection when only one profile exists
- Per-profile add-on storage
- Add-on refresh check
- Duplicate add-on protection
- Safer manifest validation and install error handling
- Kids-only home, movie, show, search and genre filtering for Kids profiles
- Working See All navigation from home rails
- BluStream 2.0 Alpha release naming and APK versioning

### Changed

- Add-on installs no longer replace the current screen or silently fail
- Installed add-ons are separated by profile
- Directory results are de-duplicated before rendering
- Artwork keeps the emulator compatibility path used for BlueStacks
- Kids profiles focus on Family, Animation, Kids and Children metadata categories
- GitHub releases now publish as BluStream 2.0 Alpha

### Device targets

- Android phones, Android 8.0+
- Android TV 11+ on ARM devices
- NVIDIA Shield
- onn 4K Streaming Box
- iPhone and iPad through the separate iOS project

### Privacy direction

- No BluStream account registration requirement
- No BluStream sign-up wall
- Local profile storage remains the default

### Planned during the 2.0 alpha cycle

- Per-profile watch history and playback progress
- Full watchlists, including saving search results directly
- Similar titles on detail pages
- Preferred audio and subtitle language per profile
- Autoplay and next-episode controls
- Catalog show, hide and reorder controls
- Browser-based configurator
- Optional multi-device sync
- Broader compatible extension adapters for lawful HTTP media providers

## v0.7.0-alpha

This release expands BluStream into a fuller streaming-style app with native browsing, profiles and direct title-page playback.

### New Features

- Native movie and show search
- Search history per profile
- Genres screen for movies and shows
- My Stuff saved separately for each profile
- Series detail pages with seasons and episode lists
- Expanded Profile Manager
- Inline source finder on movie and show detail pages
- Large Play button after compatible sources are found
- Source picker with available compatible streams
- Episode source lookup from the series detail page

### Added

- Movies screen
- Shows screen
- Profile avatar letters
- Profile theme colours
- Kids profile toggle
- Up to 5 local profiles
- Cast information on supported title detail pages
- Runtime and genre metadata where available
- Cinemeta metadata search and genre catalog support
- Installed add-ons are checked directly from title pages
- Media3 player opens directly after a source is selected

### Changed

- Bottom navigation is removed
- Main navigation now lives in the top-left burger menu
- My List is renamed My Stuff
- Search now queries the metadata catalog instead of only filtering loaded home titles
- Movie and show browsing use real catalog metadata and artwork
- Find sources no longer sends the user away from the title page
- Android version moved to 0.7 alpha

### Existing Features

- Stremio-compatible add-on manager
- Direct HTTP and HTTPS playback
- External and YouTube source handling
- Torrent and magnet P2P playback
- Media3 player
- Built-in GitHub release updater

## v0.6.0-alpha

### Added

- Cinematic BluStream home screen
- Real movie and show poster artwork
- Who's Watching screen
- Drawer navigation foundation
- Editable profile names
- BluStream launcher branding
- Built-in GitHub release update checks

## v0.5.0-alpha

### Added

- Native P2P engine
- Progressive torrent playback
- Local HTTP bridge for Media3
- Torrent file selection and seek support

## v0.4.0-alpha

### Added

- Stremio-compatible add-ons
- Source parsing
- Direct stream playback

## v0.3.0-alpha

### Added

- BluStream app icon and branding

## v0.2.0-alpha

### Added

- Profiles
- Detail pages
- Favourites foundation
- Playback position foundation
- Continue Watching foundation
- Android TV focus support
