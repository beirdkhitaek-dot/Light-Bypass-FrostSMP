# Last.fm HUD (Fabric 1.21.11)

A standalone now-playing HUD powered by Last.fm. No Spotify login, works on Android (Zalith) and desktop, and works
alongside any client.

Setup (in game chat):
1. Make a free key at https://www.last.fm/api/account/create
2. `/lastfmhud key <your api key>`
3. `/lastfmhud user <your last.fm username>`
4. Make sure your player scrobbles to Last.fm (Spotify: Settings -> Link Last.fm).

Commands: `/lastfmhud toggle`, `/lastfmhud style <glass|krypton|slate>`, `/lastfmhud lyrics`, `/lastfmhud syncoffset <ms>`,
`/lastfmhud pos <x> <y>`, `/lastfmhud scale <0.5-4>`, `/lastfmhud status`.

Open chat and drag the HUD with the mouse/finger to move it (position is saved).
Lyrics: synced lyrics from lrclib.net show under the card when found. Last.fm has no playback position, so use
`/lastfmhud syncoffset` to nudge timing.
Config file: `config/lastfmhud.json`. Shows title, artist, cover art and an estimated progress bar. Display only (Last.fm
cannot control playback).

## Settings GUI (1.4.0)
Press **Right Shift** (or `/lastfmhud gui`, or tap the HUD while chat is open) to open a flat dark settings screen with an
HSV colour wheel, style buttons, size / lyrics-sync sliders and Last.fm user + API key fields. No chat commands needed.
