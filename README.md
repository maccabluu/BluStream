# BluStream 2.0 Alpha

BluStream is an Android, iOS and TV streaming app project.

## Current status

BluStream 2.0 Alpha is the current Android development version. Android builds are produced through GitHub Actions. The iOS project remains available for Xcode testing and further feature parity work.

## New in 2.0 Alpha

- Working See All navigation from home rows
- Face-style avatars for profiles
- Automatic profile selection when only one profile exists
- Kids profiles filter home, movies, shows, search and genres toward family and animation content
- Add-ons stored separately for each profile
- Safer add-on installation with duplicate protection and manifest validation
- Add-on refresh checks
- Stremio-compatible add-on support
- Real movie and show metadata and artwork
- Movie and series detail pages
- Seasons and episodes
- Source finder and built-in Media3 playback
- Stable BluStream alpha APK signing
- Built-in GitHub update checks

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

BluStream 2.0 Alpha is also being prepared for broader lawful HTTP provider adapters. Provider-specific compatibility depends on the provider format and permissions.

## Profiles

BluStream supports multiple local profiles with editable names, face avatars, Kids mode, separate My Stuff data and separate installed add-ons.

When only one profile exists, BluStream skips the Who's Watching screen automatically. Profile management remains available from the menu.

## Kids profiles

Kids profiles restrict BluStream browsing toward titles tagged with Family, Animation, Kids or Children metadata. Search and genre browsing follow the same profile filter.

## Planned during the 2.0 alpha cycle

- Per-profile watch history and playback progress
- Watchlists with save-from-search support
- Similar titles on detail pages
- Preferred audio and subtitle language per profile
- Autoplay and next episode
- Catalog show, hide and reorder controls
- Browser-based configurator
- Optional multi-device sync for Android TV, Android phone and iOS

## Built-in updates

BluStream checks the official GitHub Releases feed after launch. Automatic checks use a 15-minute cooldown. Settings includes a manual Check for updates action.

When a newer alpha release is available, BluStream offers Update now, What's new and Later.

## Android

- Minimum Android version: Android 8.0
- AndroidX Media3
- Coil artwork loading
- ARM, ARM64, x86 and x86_64 P2P libraries
- GitHub Actions builds
- GitHub Releases distribution
- Stable alpha signing pipeline

## iOS

- Native SwiftUI project
- AVPlayer playback foundation
- iPhone and iPad project support
- Xcode build workflow

## Changelog

See CHANGELOG.md for the BluStream release history and current feature changes.
