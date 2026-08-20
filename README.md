# BluStream 0.5.0 Alpha

BluStream is an Android streaming app starter for legal, licensed, public-domain, or user-owned media.

## Added in 0.2

- Profiles: Macca, Guest, and Kids
- Movie and TV series detail pages
- Season 1 episode list demo
- Saved favourites using Android SharedPreferences
- Saved playback position for movies and episodes
- Continue Watching foundation
- Focusable cards and buttons for Android TV / controller navigation
- Updated streaming-style home screen
- Search across movie and show titles
- AndroidX Media3 ExoPlayer playback

## Test content

The project uses public Google sample videos only. Replace them with media you own or have permission to distribute.

## Build

Open the BluStream folder in Android Studio, allow Gradle sync, then run the app on an Android device or emulator. Minimum Android version is Android 8.0.

## 0.3 Alpha branding update
- Added the new BluStream B app launcher icon.
- Added the BluStream logo artwork to app resources as `drawable/blustream_logo.png`.
- Added standard Android launcher icon density sizes.


## Added in 0.4

- Add-ons tab for Stremio-compatible HTTPS manifests
- Browse approved add-on metadata from stremio-addons.net public API
- Install and remove compatible add-ons
- Resolve direct HTTPS stream resources for supported media IDs
- Play supported direct streams with Media3
- Parse direct URLs, external URLs, YouTube IDs, torrent info hashes, file indexes, tracker hints, NZB and archive-style stream descriptors
- Direct HTTP/HTTPS video URLs play through Media3
- External, YouTube and other URI-based sources stay visible
- Torrent and magnet sources now use BluStream’s built-in P2P engine
- No keyword-based add-on or source blocking
- Directory attribution shown inside the app

BluStream is intended for legal, licensed, public-domain, and user-authorized media sources.


## Added in 0.5 P2P

- Native BitTorrent engine using FrostWire jlibtorrent and libtorrent
- Android ARM, ARM64, x86 and x86_64 native support
- Magnet and info-hash source support
- Torrent metadata fetching inside BluStream
- Selected-file priority support using Stremio `fileIdx`
- Automatic largest-video selection when no file index is supplied
- Localhost HTTP byte-range bridge for AndroidX Media3
- Progressive playback while pieces arrive
- Seeking waits for the required verified torrent pieces
- P2P connection status shown in the BluStream interface

Use P2P streaming only for media you are authorised to access or distribute.
