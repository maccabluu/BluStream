# BluStream 2.3 Alpha

BluStream is an Android, iOS and TV streaming app project.

## Current status

BluStream 2.3 Alpha is the current Android development version. Android builds are produced through GitHub Actions. The iOS project remains available for Xcode testing and further feature parity work.

## New in 2.3 Alpha

- TV show detail pages now load seasons and episodes from built-in metadata
- Season selector added to series pages
- Episodes show title, episode number, thumbnail and summary where available
- Selecting an episode clears old sources and searches specifically for that episode
- A large Play button appears after compatible sources are found
- Every source card still has its own Play button
- Torrent and magnet sources start the built-in P2P engine directly
- Home button remains on title detail pages
- Proper face-style profile avatars remain enabled
- Built-in update checker remains inside Settings > App
- Kids profiles continue filtering browsing toward family and animation content
- Real movie and show metadata and artwork
- Stable BluStream APK signing
- BlueStacks artwork compatibility loading

## Supported device targets

- Android phones, Android 8.0+
- Android TV 11+ on ARM devices
- NVIDIA Shield
- onn 4K Streaming Box
- iPhone and iPad through the native iOS project

## Privacy

BluStream does not require a BluStream account, registration or a sign-up wall. Local profiles and preferences remain on the device by default.

Third-party add-ons and metadata providers have their own privacy policies and network behaviour.

## Metadata

BluStream includes built-in metadata browsing through its current metadata provider. The project also supports Stremio-compatible add-ons for compatible metadata and stream sources.

## Add-ons

- Stremio-compatible manifests
- HTTP and HTTPS manifest URLs
- Per-profile installed add-ons
- Directory browsing
- Duplicate protection
- Refresh checks
- Direct HTTP and HTTPS playback
- External URI and YouTube source handling
- Torrent and magnet source handling
- Native P2P engine through jlibtorrent

## Profiles

BluStream supports multiple local profiles with editable names, face avatars, Kids mode and separate installed add-ons.

When only one profile exists, BluStream skips the Who's Watching screen automatically. Profile management remains available from the menu.

## Kids profiles

Kids profiles restrict BluStream browsing toward Family and Animation metadata. Search follows the same profile filter.

## Built-in updates

BluStream checks the official GitHub Releases feed after launch. Automatic checks use a 15-minute cooldown.

Settings > App > Check for updates runs a manual check immediately.

When a newer release is available, BluStream offers Update now, What's new and Later.

Update now downloads the APK inside BluStream and opens Android's package installer.

## Android

- Current Android alpha: 2.3
- Minimum Android version: Android 8.0
- AndroidX Media3
- Coil artwork loading with BlueStacks compatibility
- ARM, ARM64, x86 and x86_64 P2P libraries
- GitHub Actions builds
- GitHub Releases distribution
- Stable alpha signing pipeline

## iOS

- Native SwiftUI project
- AVPlayer playback foundation
- iPhone and iPad project support
- Xcode build workflow

## Release numbering

BluStream releases follow simple version numbers:

2.0 → 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 3.0

## Planned

- Per-profile watch history and playback progress
- Watchlists with save-from-search support
- Similar titles on detail pages
- Preferred audio and subtitle language per profile
- Autoplay and next episode
- Catalog show, hide and reorder controls
- Browser-based configurator
- Optional multi-device sync for Android TV, Android phone and iOS

## Changelog

See CHANGELOG.md for the BluStream release history and current feature changes.
