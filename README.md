<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96" height="96" alt="Sonora" />
  <h1>Sonora</h1>
  <p>Offline music player for Android.</p>
  <p><a href="README.md">English</a> / <a href="README.ru.md">Русский</a></p>
</div>

A simple local audio player built with Jetpack Compose and Media3. No ads, no analytics, no internet access.

## Features

- Plays local audio (FLAC, WAV, MP3, AAC, M4A, OGG, OPUS) in the background with media notification controls.
- Dynamic Material 3 theming that picks colors from the current album cover, system wallpaper, or a custom color, plus an AMOLED black mode.
- Wavy seek bar, swipe down to close the player, and swipe left/right on the cover to switch tracks.
- Shows codec, bitrate, and sample rate on the player screen.
- Scans your device and selected folders, with search, A–Z sorting, favorites, custom playlists, and a sleep timer.
- Adapts to landscape mode with a two-column player and a side navigation bar (can be placed on the left or right in settings).

## Build

Requires Android SDK (API 35) and JDK 17:

```bash
git clone https://github.com/chaliemori/Sonora.git
cd Sonora
gradle assembleDebug
```

For a release build (uses `release.properties` and `release.keystore` if present):

```bash
gradle assembleRelease
```
