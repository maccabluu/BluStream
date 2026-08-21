# BluStream 3.2 Alpha

BluStream is an Android, iOS and TV streaming app project.

## Current status

BluStream 3.2 Alpha is the current Android development version. Android builds are produced through GitHub Actions. The iOS project remains available for Xcode testing and further feature parity work.

## New in 3.2 Alpha

- HTTPS direct streams are preferred first for faster playback startup
- HTTP direct streams are second
- Hosted and external streams follow
- YouTube-compatible sources follow
- P2P torrent sources remain available as a fallback only
- Source cards identify FAST HTTPS DIRECT, DIRECT HTTP and P2P FALLBACK
- The main Play action uses the first ranked source, so direct HTTPS wins when available
- Genres is back in the burger menu
- Movie and TV genre browsing is available
- Kids profiles receive a reduced family-friendly genre list
- Who's Watching remains the first screen on launch
- BLU STREAM branding with the STREAM ANYTHING tagline remains enabled
- TV seasons and episodes remain available from show detail pages
- Built-in update checker remains inside Settings > App

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

## Add-ons and playback

- Stremio-compatible manifests
- HTTP and HTTPS manifest URLs
- Per-profile installed add-ons
- Directory browsing
- Duplicate protection
- Refresh checks
- Direct HTTP and HTTPS playback
- HTTPS direct source prioritisation
- External URI and YouTube source handling
- Torrent and magnet fallback handling
- Native P2P fallback engine through jlibtorrent

BluStream does not remove P2P sources. It ranks faster direct HTTPS sources above them when an add-on supplies both.

## Profiles

BluStream supports multiple local profiles with editable names, face avatars, Kids mode and separate installed add-ons.

Who's Watching appears before the main BluStream interface on app launch. The chosen profile is saved as the active profile for the main app session.

## Kids profiles

Kids profiles restrict BluStream browsing toward Family and Animation metadata. Search and Genres follow the same profile filter.

## Genres

Genres is available from the burger menu. Standard profiles receive movie and TV genre browsing. Kids profiles receive a smaller family-focused genre list.

## Built-in updates

BluStream checks the official GitHub Releases feed after launch. Automatic checks use a 15-minute cooldown.

Settings > App > Check for updates runs a manual check immediately.

When a newer release is available, BluStream offers Update now, What's new and Later.

Update now downloads the APK inside BluStream and opens Android's package installer.

## Android

- Current Android alpha: 3.2
- Minimum Android version: Android 8.0
- AndroidX Media3
- Coil artwork loading with BlueStacks compatibility
- ARM, ARM64, x86 and x86_64 P2P fallback libraries
- GitHub Actions builds
- GitHub Releases distribution
- Stable alpha signing pipeline

## iOS

- Native SwiftUI project
- AVPlayer playback foundation
- iPhone and iPad project support
- Xcode build workflow

## Release numbering

BluStream releases use simple version numbers such as 3.0, 3.1, 3.2, 3.3 and 4.0.

## Planned

- Faster local metadata and artwork caching
- Improved player buffering and playback error feedback
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