# Syncthing sync implementation

The sync layer now treats the Syncthing folder as a shared collection of per-device snapshots.

## What syncs

- Movie/TV watchlist membership and status
- Started/finished/interrupted/dropped state and dates
- Favorites and pinned state
- User ratings
- Notes
- TV season status, ratings, notes and last-episode progress
- Individual TV episode watched/unwatched state and watched dates
- Custom lists, their contents, descriptions, icons and pinned state

TMDB/trending caches and UI preferences are intentionally not synced.

## Merge behavior

Each device writes its own file:

`watchmaster_sync_<device-id>.json`

On Sync Now/resume, the app reads all snapshot files currently visible in the selected Syncthing folder and merges records using `lastModifiedAt`. Equal timestamps use the device ID as a deterministic tie-breaker.

The old `watchmaster_sync.json` format is still read for migration/compatibility.

The merge is additive: absence from a device snapshot is not treated as a deletion. This avoids accidental deletion when one device has stale or incomplete data.

## First-time setup

1. Point Syncthing at the same folder on both devices.
2. Let Syncthing finish exchanging the folder.
3. On each WatchMaster device, choose that folder under Settings > Sync.
4. Press **Sync now** on each device once.
5. After that, Sync now, app resume, and app stop update the device snapshot automatically.
