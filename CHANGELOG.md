# BluStream Changelog

## v2.1

BluStream 2.1 Alpha restores the in-app update controls and fixes the profile and artwork problems seen in the 2.0 test build.

### Added

- Settings > App > Check for updates
- Automatic GitHub release checks after launch
- 15-minute cooldown for automatic update checks
- Update now action to download the APK and open Android's installer
- What's new release-notes view
- Proper face-style profile avatars
- Automatic conversion of older emoji avatar data to face avatars
- BlueStacks artwork compatibility loading

### Fixed

- Emoji avatars rendering incorrectly across the screen
- Profile avatar display in the top-right header
- Missing visible manual update option in Settings
- Poster and hero artwork corruption on BlueStacks
- See All navigation from home rails

### Changed

- Current Android release is now BluStream 2.1 Alpha
- Release numbering now uses simple versions such as 2.1, 2.2, 2.3, 2.4, 2.5 and 3.0
- GitHub Actions publishes BluStream-2.1-alpha.apk as release v2.1

## v2.0

BluStream 2.0 Alpha focuses on profiles, safer add-on handling, kids browsing and cleaner navigation.

### Added

- Face-style profile avatar foundation
- Automatic profile selection when only one profile exists
- Per-profile add-on storage
- Add-on refresh check
- Duplicate add-on protection
- Safer manifest validation and install error handling
- Kids-only home, movie, show and search filtering for Kids profiles
- Working See All navigation from home rails

### Changed

- Add-on installs no longer replace the current screen or silently fail
- Installed add-ons are separated by profile
- Directory results are de-duplicated before rendering
- Kids profiles focus on Family and Animation metadata

## v0.7

- Native movie and show search
- Search history per profile
- My Stuff per profile foundation
- Series detail pages with seasons and episode lists
- Expanded Profile Manager
- Inline source finder on movie and show detail pages
- Source picker with compatible streams
- Media3 player opens directly after a source is selected
- Bottom navigation removed in favour of the burger menu

## v0.6

- Cinematic BluStream home screen
- Real movie and show poster artwork
- Who's Watching screen
- Drawer navigation foundation
- Editable profile names
- BluStream launcher branding
- Built-in GitHub release update checks

## v0.5

- Native P2P engine
- Progressive torrent playback
- Local HTTP bridge for Media3
- Torrent file selection and seek support

## v0.4

- Stremio-compatible add-ons
- Source parsing
- Direct stream playback

## v0.3

- BluStream app icon and branding

## v0.2

- Profiles
- Detail pages
- Favourites foundation
- Playback position foundation
- Continue Watching foundation
- Android TV focus support
