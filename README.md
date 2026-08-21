# BluStream 0.7 Alpha

BluStream is an Android / iOS / TV streaming app.

## Current status

BluStream 0.7 Alpha is the current Android development version. Android builds are produced through GitHub Actions. The iOS project remains available for Xcode testing.

## New in 0.7

- Bottom navigation removed
- Main navigation moved into the top-left burger menu
- Movies screen
- Shows screen
- Native movie and show search
- Search history saved separately for each profile
- Genres screen with movie and show filters
- My Stuff saved separately for each profile
- Real title metadata and artwork through Cinemeta
- Series detail pages with season and episode lists
- Runtime, cast and genre metadata where available
- Expanded Who's Watching profile manager
- Edit profile names
- Edit avatar letters
- Change profile theme colours
- Kids profile toggle
- Add up to 5 profiles
- Remove profiles

## Home and browsing

BluStream uses real movie and show metadata for browsing. The home screen includes cinematic artwork, popular movie rows, popular show rows and title detail pages.

## Add-ons and playback

- Stremio-compatible add-on manager
- HTTP and HTTPS manifest support
- Direct HTTP and HTTPS playback
- External URI handling
- YouTube source handling
- Torrent and magnet source handling
- Native P2P engine through jlibtorrent
- Media3 ExoPlayer playback

## Built-in updates

BluStream checks the official GitHub Releases feed after launch. Automatic checks use a 15-minute cooldown. Settings includes a manual Check for updates action.

When a newer canonical alpha release is available, BluStream offers Update now, What's new and Later.

## Android

- Minimum Android version: Android 8.0
- AndroidX Media3
- Coil artwork loading
- ARM, ARM64, x86 and x86_64 P2P libraries
- GitHub Actions builds
- GitHub Releases distribution

## iOS

- Native SwiftUI project
- AVPlayer playback foundation
- iPhone and iPad project support
- Xcode build workflow

## Changelog

See CHANGELOG.md for the BluStream release history and current feature changes.
