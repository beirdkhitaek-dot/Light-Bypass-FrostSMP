# Last.fm HUD (Fabric 1.21.11)

A standalone now-playing HUD powered by Last.fm. No Spotify login, works on Android (Zalith) and desktop, and works
alongside any client.

Setup (in game chat):
1. Make a free key at https://www.last.fm/api/account/create
2. `/lastfmhud key <your api key>`
3. `/lastfmhud user <your last.fm username>`
4. Make sure your player scrobbles to Last.fm (Spotify: Settings -> Link Last.fm).

Commands: `/lastfmhud toggle`, `/lastfmhud pos <x> <y>`, `/lastfmhud scale <0.5-4>`, `/lastfmhud status`.
Config file: `config/lastfmhud.json`. Shows title, artist, cover art and an estimated progress bar. Display only (Last.fm
cannot control playback).
