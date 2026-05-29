# Docs

This folder contains the **source of truth** documentation for DupBuster v1.

## Read order (authority)

1. `docs/requirements.md` — product scope, acceptance criteria, and non-negotiables
2. `docs/architecture.md` — system boundaries, bridge contracts, schema, and security rules
3. `docs/implementation-plan.md` — milestone plan (M1–M5), tasks, and exit gates

If there is any conflict, follow the authority order above.

## Project invariants (v1 quick recap)

- **On-device only**: no accounts, backend sync, or cloud upload of user files
- **Duplicates = same content only** (by hash / defined equivalence rules), not name/path/metadata
- **Review before delete**: destructive actions require explicit keeper selection + two-step confirm
- **Partial coverage honesty**: never claim full-device scan without grants

## App scaffold (M1-01)

Bare React Native **0.85.3** with `android/` and `ios/` at repo root. Application id / bundle: `com.dupbuster`. Min platforms: **Android API 26**, **iOS 15.1**.

```bash
npm install && npm start
npm run android   # or npm run ios on macOS after pod install
```

Native ScanEngine code lives under `android/.../scanengine/` and `ios/ScanEngine/`.

### ScanEngine Turbo Module (M1-02)

| Piece | Location |
|-------|----------|
| Codegen spec + types | `src/native/NativeScanEngine.ts` |
| Jest-safe types (no native load) | `src/native/scanEngineBridge.types.ts`, re-exported from `src/types/scanEngine.ts` |
| Android stub | `android/.../scanengine/ScanEngineModule.kt` + `ScanEnginePackage.kt` |
| iOS stub | `ios/ScanEngine/RCTNativeScanEngine.{h,mm}` |

- **Module name:** `NativeScanEngine` (New Architecture / Turbo Module).
- **Commands (JS → native):** `startScan`, `pauseScan`, `resumeScan`, `cancelScan`, `deleteDuplicates`, `getCatalogMeta`.
- **Events (native → JS):** `onScanProgress`, `onScanError` — bridge law: no paths, hashes, or file bytes on events.
- **Codegen:** `package.json` → `codegenConfig` (`ScanEngineSpec`, `jsSrcsDir`: `src/native`). Regenerated on Android build via `generateCodegenArtifactsFromSchema`.
- **iOS:** After pulling, run `cd ios && bundle exec pod install` on macOS so codegen + `modulesProvider` link `RCTNativeScanEngine`.
- Stubs reject scan/delete commands until scan orchestrator wiring (M1-11+) / M3 delete; `getCatalogMeta` reads live catalog metadata from IndexWriter (M1-09).

### UriValidator (M1-03)

| Piece | Location |
|-------|----------|
| Android | `android/.../scanengine/security/UriValidator.kt` (+ `SafUriRules.kt`) |
| iOS | `ios/ScanEngine/Security/DBUriValidator.{h,mm}` |
| Fixture | `tests/fixtures/dupbuster/v1/security-uri-01.json` (AC-security-uri-01) |

- Fail-closed → `PERMISSION_DENIED` (FR-SE-01 / FR-UN-03). Rejects user-pasted URIs, `file://`, SAF docIds containing `..`, and out-of-grant documents.
- **Android unit tests:** `cd android && ./gradlew :app:testDebugUnitTest`
- **iOS unit tests (macOS):** `xcodebuild test -project ios/Dupbuster.xcodeproj -scheme Dupbuster -destination 'platform=iOS Simulator,name=iPhone 16' -only-testing:DupbusterScanEngineTests` (after `pod install`)

### DiscoveryEmitter mode A (M1-04)

| Piece | Location |
|-------|----------|
| Android | `android/.../scanengine/discovery/DiscoveryEmitter.kt` (SAF BFS via `DocumentsContract`) |
| iOS | `ios/ScanEngine/Discovery/DBDiscoveryEmitter.{h,mm}` (security-scoped `file://` folder) |
| Fixture | `tests/fixtures/dupbuster/v1/discovery-user-selected-01.json` |

- **Mode A only** (`user_selected`): SAF tree walk (Android) or DocumentPicker folder (iOS). Each file passes `UriValidator` with `DISCOVERY` provenance before emit.
- Emits `DiscoveredEntry` / `DBDiscoveredEntry`: `contentUri`/`contentURL`, `displayName`, `sizeBytes`, `mtimeNs`, `mediaTypeHint`, `scanRootId`, `generation` — native-only until IndexWriter (M1-09).
- Yields every **32** emitted files; honours cooperative cancel callback.

### DiscoveryEmitter mode B (M1-05)

| Piece | Location |
|-------|----------|
| Android | `DiscoveryEmitter.emitModeB` + `ContentResolverMediaStoreDiscoveryQuery` (images, video, audio, downloads) |
| iOS | `DBDiscoveryEmitter.emitModeBWithScanRootGrant:` + `DBPhotosPhAssetDiscoverySource` |
| Fixture | `tests/fixtures/dupbuster/v1/discovery-platform-discovery-01.json` |

- **Mode B** (`platform_discovery`): MediaStore union on Android; PHAsset fetch on iOS (optional limited-library identifier list). Optional SAF / security-scoped folder union via `additionalSafGrants` / `additionalScopedFolderURLs`.
- Android grant marker: `PlatformDiscoveryGrant.MARKER_URI` (`content://dupbuster/scan-root/platform-discovery`). iOS platform grant typically `uriGrant` `*`.
- PHAsset rows set `phAssetLocalIdentifier` on `DBDiscoveredEntry` (no `contentURL`); MediaStore rows use `contentUri` as in Mode A.
- No `/sdcard` crawl without grants (requirements §5.1 mode B).

### StatStage (M1-06)

| Piece | Location |
|-------|----------|
| Android | `StatStage.kt` + `ContentResolverFileStatReader` (`openFileDescriptor` + `Os.fstat`) |
| iOS | `DBStatStage` + `DBFileStatReader` (`lstat` for `file://`; PHAsset mtime via Photos.framework) |
| Fixture | `tests/fixtures/dupbuster/v1/stat-content-uri-01.json` |

- Runs **after** UriValidator on each discovered file; re-reads authoritative size/mtime/inode/device_id (FR-SI-01 TOCTOU baseline).
- Android: `st_ino` / `st_dev` from `Os.fstat`; best-effort symlink flag via `/proc/self/fd` readlink.
- iOS `file://`: `lstat` only (no symlink follow); PHAsset rows have null inode/device_id (hard links N/A in photo library).
- Video duration/width/height deferred to M1-13+.
- Output: `StagedFile` / `DBStagedFile` — native-only until IndexWriter (M1-09).

### HashPipeline (M1-07)

| Piece | Location |
|-------|----------|
| Android | `HashPipeline.kt` + `Sha256Hasher` + `ContentResolverFileContentReader` |
| iOS | `DBHashPipeline` + `DBSha256Hasher` + `DBFileContentReader` |
| Fixture | `tests/fixtures/dupbuster/v1/hash-raw-bytes-01.json` |

- Stages: size-bucket skip (unique size) → quick sample (> 50 MB: first+last 64 KiB SHA-256) → full `RAW_BYTES` stream (1 MiB buffer, 120 s timeout).
- `EMPTY:0` for zero-byte files; video exempt from size-bucket skip (full `VIDEO_CONTENT_V1` in M1-13+).
- `LARGE_SKIPPED` when size > 2 GB without `largeFilesOptIn`; symlink nodes indexed without following.
- Output: `HashedFile` / `DBHashedFile` — native-only until IndexWriter (M1-09).

### Text normalization (M1-08)

| Piece | Location |
|-------|----------|
| Android | `TextNormalizer.kt`; `HashPipeline` uses `TEXT_NFC_LF` when `mediaTypeHint` is `text` |
| iOS | `DBTextNormalizer`; `DBHashPipeline` routes `DBMediaTypeHintText` |
| Fixture | `tests/fixtures/dupbuster/v1/hash-text-nfc-lf-01.json` |

- Pipeline: UTF-8 decode (strict) → strip BOM → NFC → CRLF→LF → SHA-256; **no** trailing-whitespace trim (AC-equiv-text-04).
- Quick sample (> 50 MB) remains on raw bytes; invalid UTF-8 → unscannable (same as read failure).
- Covers AC-equiv-text-01–04 in native unit tests.

### IndexWriter (M1-09)

| Piece | Location |
|-------|----------|
| Android | `CatalogSchema.kt`, `CatalogDatabase.kt`, `IndexWriter.kt`, `SqliteSizeBucketIndex.kt` |
| iOS | `DBCatalogSchema`, `DBCatalogDatabase`, `DBIndexWriter`, `DBSqliteSizeBucketIndex` |
| Fixture | `tests/fixtures/dupbuster/v1/index-hashed-file-01.json` |

- SQLite schema v2 per architecture §5.2 (`schema_version` in `meta`); `getCatalogMeta` reads live values from the catalog DB.
- Persists hash-pipeline outcomes (`file_entry`, `fingerprint`, hard-link `file_path` aliases).

### Grouper (M1-10)

| Piece | Location |
|-------|----------|
| Android | `MatchKind.kt`, `Grouper.kt` under `scanengine/index/` |
| iOS | `DBMatchKind`, `DBGrouper` under `ScanEngine/Index/` |
| Fixture | `tests/fixtures/dupbuster/v1/index-duplicate-group-01.json` |

- `rebuildDuplicateGroups()` clears and rebuilds `duplicate_group` / `duplicate_member` from hashed `file_entry` rows (≥2 per fingerprint).
- `match_kind`: `EXACT_BYTES` for `RAW_BYTES` / `TEXT_NFC_LF` / `EMPTY:0`; `SAME_CONTENT_VIDEO` for `VIDEO_CONTENT_V1` (hashing in M1-13).
- Scan-time `reclaimable_bytes_est` = sum(sizes) − max(size); `is_keeper` stays 0 until delete (M3).
- `EXACT_BYTES` takes precedence: members already in an exact-bytes group are excluded from `SAME_CONTENT_VIDEO` groups (AC-equiv-video-xres-04).
- `SqliteSizeBucketIndex` replaces in-memory counts for production size-bucket elimination.

**WSL:** copy `android/local.properties.example` → `android/local.properties`. Builds in WSL need a **Linux** SDK (`~/Android/Sdk`), not the Windows SDK under `/mnt/c/...` (NDK host toolchain mismatch). Emulator can stay on Windows via `adb.exe`.

## Milestones

Implementation proceeds in milestones as defined in `docs/implementation-plan.md`.

- **M1** (in progress): Native ScanEngine spike (discovery, hashing, SQLite index, progress throttle, checkpoint, video fingerprinting)
- **M2**: React Native shell + UX catalog (can stub progress until bridge is live)
- **M3**: Actions + integrity (two-step delete, TOCTOU, permission-revoke pause/resume)
- **M4**: Background + release hardening (Android FGS, redaction gate, store checklist)
- **M5**: Deliverable docs (this folder)

## Working on a milestone

When implementing a milestone, treat the milestone’s **Exit gate** section in `docs/implementation-plan.md` as the definition of done:

- Track exit-gate bullets as a checklist.
- Mark an item complete only when it is **actually** satisfied (ideally with tests/fixtures where required).

