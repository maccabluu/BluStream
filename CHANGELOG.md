# BluStream Changelog

## v2.5

BluStream 2.5 Alpha changes startup and stream priority for faster everyday use.

### Added

- Dedicated Who's Watching launch screen shown before the main app
- New BLU STREAM branding with the STREAM ANYTHING tagline on the startup profile screen
- Fast source ranking for compatible add-on results
- FAST DIRECT label for direct stream results

### Changed

- HTTPS direct streams are preferred first
- HTTP direct streams are preferred next
- External hosted and YouTube sources follow
- P2P torrent sources now sit lower in the list as fallback sources
- The main Play action selects the fastest ranked available source first
- Who's Watching no longer gets skipped when one profile exists
- Current Android release is now BluStream 2.5 Alpha
- GitHub Actions publishes BluStream-2.5-alpha.apk as release v2.5

### Kept

- P2P fallback playback
- TV seasons and episode selection
- Title-page Play buttons
- 10-second status-message dismissal
- Built-in update checker
- Kids profile filtering

## v2.4

BluStream 2.4 Alpha improves torrent playback reliability and cleans up status messages.

### Added

- Stremio torrent file-index parsing
- Stremio torrent tracker-hint parsing
- Fallback tracker list for torrent sources with limited tracker data
- Automatic 10-second dismissal for on-screen status and error messages
- Multiple metadata attempts before a P2P source is marked unavailable

### Fixed

- Torrent sources losing fileIdx and sources fields from compatible Stremio add-ons
- Some magnets being created without useful tracker information
- Torrent metadata errors staying pinned across the top of the title page
- P2P metadata lookup failing after a single attempt

### Changed

- P2P failures now show a shorter message asking users to try another source or retry later
- Current Android release is now BluStream 2.4 Alpha
- GitHub Actions publishes BluStream-2.4-alpha.apk as release v2.4

## v2.3

BluStream 2.3 Alpha adds proper TV episode browsing and a clearer playback flow.

### Added

- Season selector on TV show detail pages
- Episode list with episode numbers, titles, thumbnails and summaries where metadata provides them
- Episode-specific source lookup
- Large Play button after compatible sources are found
- Selected episode indicator

### Fixed

- TV shows no longer search the whole series ID when an episode is selected
- Source results are cleared when switching episodes or seasons
- Users no longer need to guess which episode a stream belongs to

### Changed

- Current Android release is now BluStream 2.3 Alpha
- GitHub Actions publishes BluStream-2.3-alpha.apk as release v2.3

## v2.2

BluStream 2.2 Alpha fixes title-page playback and navigation.

### Added

- Visible Play button on every source card
- Direct P2P launch from torrent and magnet source cards
- Connecting state while the P2P engine prepares a source
- Visible Home button on movie and show detail pages

### Fixed

- Removed the Select this source from Add-ons for P2P playback dead-end
- Torrent sources now pass directly into BluStream's P2P engine from the title page
- Home from a title page now returns to the main BluStream screen

### Changed

- Current Android release is now BluStream 2.2 Alpha
- GitHub Actions publishes BluStream-2.2-alpha.apk as release v2.2

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
