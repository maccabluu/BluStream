# BluStream 0.6 Alpha

BluStream is an Android / iOS / TV streaming app.

## Current status

BluStream 0.6 Alpha is the current development branch. Android builds are published through GitHub Actions and GitHub Releases. The iOS project is included for Xcode testing.

## Latest Android UI update

- Bottom navigation removed
- Home, Discover, Search, My List, Add-ons and Settings moved into the top-left burger menu
- Switch Profile and Manage Profiles added to the burger menu
- Who's Watching now supports editable profile names
- Add up to 5 profiles
- Remove profiles
- Profile choices persist on the device
- Home now loads real movie and series metadata and artwork from the Cinemeta Stremio metadata catalog
- Real title detail pages with artwork, release information and descriptions
- Add-on source lookup remains separate from metadata browsing

## Added in 0.6

- Cinematic BluStream home screen
- BLUSTREAM branded top bar
- Full-width featured hero artwork
- Real movie and series poster rows
- Dark navy streaming interface with bright blue accents
- Who's Watching profile screen
- Discover screen
- Search screen
- My List screen
- Add-ons screen
- Settings screen
- Movie and series detail screen
- Direct playback through AndroidX Media3
- Existing P2P source handling
- Existing Stremio-compatible add-on handling
- BluStream blue B play icon for the Android launcher
- BluStream branding prepared for Android, iOS and TV

## Built-in updates

BluStream checks the official GitHub Releases feed after launch.

Automatic update checks use a 15-minute cooldown.

Settings > Check for updates runs a manual check immediately.

When a newer canonical alpha is available, BluStream offers:

- Update now, download the APK through BluStream and open Android's install confirmation
- What's new, read the release notes
- Later, continue using the current version

BluStream compares installed alpha build numbers so the same release is not offered repeatedly.

## Android

- Minimum Android version: Android 8.0
- AndroidX Media3 ExoPlayer playback
- Coil image loading for cinematic artwork and poster cards
- Real movie and series metadata through Cinemeta
- ARM, ARM64, x86 and x86_64 native P2P support
- GitHub Actions APK builds
- GitHub Releases distribution

## iOS

- Native SwiftUI project
- AVPlayer playback foundation
- iPhone and iPad project support
- Xcode build workflow included

## Add-ons and sources

- Stremio-compatible HTTP and HTTPS manifests
- Install and remove compatible add-ons
- Direct HTTP and HTTPS stream playback
- External URI handling
- YouTube source handling
- Torrent and magnet source handling through BluStream's P2P engine
- File-index and tracker-hint parsing

## P2P

- FrostWire jlibtorrent and libtorrent engine
- Torrent metadata fetching
- Selected-file priority support
- Largest-video selection when no file index is supplied
- Localhost HTTP byte-range bridge for Media3
- Progressive playback while pieces arrive
- Seek support based on verified torrent pieces
- P2P connection status inside BluStream

## Development history

0.2 added profiles, detail pages, favourites, playback position, Continue Watching foundations, Android TV focus support and Media3 playback.

0.3 added BluStream launcher branding and logo resources.

0.4 added Stremio-compatible add-ons, source parsing and direct playback.

0.5 added the native P2P engine and progressive torrent playback.

0.6 adds the cinematic BluStream interface, drawer navigation, editable profiles, real catalog metadata, Android / iOS / TV branding and built-in GitHub update checks.
