# YouTubeWhitelist

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/X8X71TWXEN)

A free, open-source Android app that lets parents whitelist specific YouTube channels, videos, and playlists. Kids only see what's approved — nothing else.

## Features

- **Parent Mode** — Browse YouTube freely, add channels/videos/playlists to your kid's whitelist
- **Kid Mode** — Video feed home with channel chips, 2-column grid, and Shorts filtered out (>60s only)
- **Channel Blocklist** — Block specific channels so they never appear in Kid Mode
- **PIN Protection** — Parent mode is locked behind a secure PIN
- **Multiple Profiles** — Create separate whitelists for each child
- **Daily Time Limits** — Set per-profile daily watch time limits
- **Sleep Mode** — Timer-based playback with gradual volume fade-out for bedtime
- **Watch Statistics** — Track daily, weekly, and monthly watch time per profile
- **Kiosk Mode** — Screen pinning keeps kids inside the app
- **Export / Import** — Backup and restore profiles and whitelists as JSON
- **Voice Search** — Speak to search on TV remotes with a mic button
- **Search** — Kids can search within their whitelisted content
- **Suggested Videos** — Pool of related whitelisted videos suggested after each video
- **Playlist Support** — Whitelist entire playlists with automatic video listing
- **Android TV Support** — Full D-pad navigation, fullscreen player with overlay suggestions, adaptive layout
- **Runtime Credential Entry** — Enter your Google API credentials inside the app — no recompilation needed
- **100% Client-Side** — No backend server, your data stays on your device
- **No Ads, No Tracking** — Completely free and open source

## Building

### Prerequisites

- Android Studio (or JDK 17 + Android SDK)
- A Google Cloud project with YouTube Data API v3 enabled (OAuth 2.0 Client ID + optionally an API key)

### Option A — Enter credentials inside the app (recommended)

1. Clone and build:
   ```bash
   git clone https://github.com/degipe/YouTubeWhitelist.git
   cd YouTubeWhitelist
   ./gradlew assembleDebug
   ```
2. Install the APK and launch it — you'll be prompted to enter your **OAuth Client ID** and **Client Secret** on first run.
3. See [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md) for step-by-step Google Cloud Console instructions.

### Option B — Bake credentials into the build

1. Create `local.properties` and add:
   ```properties
   YOUTUBE_API_KEY=your_api_key_here
   GOOGLE_CLIENT_ID=your_client_id_here
   GOOGLE_CLIENT_SECRET=your_client_secret_here
   ```
2. Build:
   ```bash
   ./gradlew assembleDebug
   ```

For detailed Google Cloud Console setup instructions (OAuth client, API key restrictions), see [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md).

## Tech Stack

- Kotlin + Jetpack Compose
- Material Design 3
- MVVM + Clean Architecture (multi-module)
- Hilt (DI), Room (database), Retrofit (network)
- YouTube Data API v3 + IFrame Player API

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

## Support Development

If you find this app useful, consider supporting its development:

<a href='https://ko-fi.com/X8X71TWXEN' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>
