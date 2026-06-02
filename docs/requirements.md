# DupBuster v1 — Requirements

**Product:** DupBuster (working name) — cross-platform duplicate media finder for Android and iOS  
**Version:** 1.0 (initial release scope)  
**Sources:** `.agent/results/dupbuster-debate-results.md`, `.agent/results/dupbuster-video-cross-resolution-debate-results.md`, `.agent/debate-floors/dupbuster-debate.md`, `.agent/debate-floors/dupbuster-video-cross-resolution-debate.md`  
**Status:** M0 design package locked; ready for implementation

---

## 1. Product overview

DupBuster scans user-selected storage or, when no directory is chosen, all directories the app can legally access on the device. It surfaces groups of files with **the same content** — byte-identical files and, for photos, visually matching images across re-encode, resize, and compression — and lets users review duplicate groups before deleting or moving files to reclaim space.

All duplicate detection runs **on-device**. No account login, backend sync, or cloud upload of user files is required for v1.

---

## 2. Duplicate definition (non-negotiable)

Two or more files are **duplicates** when they match under the v1 equivalence rules in §4, regardless of:

- File path or folder location
- Filename
- On-disk byte layout (for images matched via `IMAGE_CONTENT_V1`)
- Resolution (images/video)
- Thumbnail or poster frame (videos)
- Metadata (EXIF, ID3, container tags, etc.) unless metadata *is* the content for text-like files

**Near-duplicates** are **out of scope for v1** **except**:

- **Images:** re-encoded, recompressed, or resized variants of the same picture via `IMAGE_CONTENT_V1` (see §4, §7, §11).
- **Video:** cross-resolution / cross-encode variants via `VIDEO_CONTENT_V1` (see §4, §7, §11).

---

## 3. Media scope

**Media** means **all file types** the app can read on device storage, including but not limited to photos, videos, audio, documents, plain text, and other binary files present in scanned paths. The product must not treat "photos only" as the default.

---

## 4. Content equivalence matrix (v1)

Per-type rules for determining content sameness:

| Category | v1 rule | Normalization profile |
|----------|---------|----------------------|
| Plain text | UTF-8 NFC, strip UTF-8 BOM, CRLF→LF; SHA-256 of normalized bytes. **Trailing whitespace significant** (no trim). | `TEXT_NFC_LF` |
| Documents (docx, pdf, etc.) | Byte-identical file hash (SHA-256 streaming). Cross-format logical equivalence **deferred**. | `RAW_BYTES` |
| Images | Two-path rule: (A) byte-identical on-disk hash (`RAW_BYTES`), OR (B) same picture content via `IMAGE_CONTENT_V1` (single-frame dHash on decoded bitmap, downscale ≤320×180, Hamming ≤ 8). EXIF/orientation/metadata differences alone do not block (B) when visuals match. | `RAW_BYTES`, `IMAGE_CONTENT_V1` |
| Audio | Full container file hash (bytes on disk). No atom/tag stripping in v1. | `RAW_BYTES` |
| Video | Two-path rule: (A) byte-identical container hash (`RAW_BYTES`), OR (B) same substantive content across resolution/encode/container via `VIDEO_CONTENT_V1` under duration gate + Hamming threshold (see §7.1). | `RAW_BYTES`, `VIDEO_CONTENT_V1` |
| Other binary | Opaque byte stream SHA-256. | `RAW_BYTES` |
| Empty files | All zero-byte files share synthetic fingerprint `EMPTY:0` — one duplicate class, not mixed with non-empty. | `EMPTY:0` |
| Symlinks | Do not follow; index with `is_symlink` flag; hash symlink node only. Follow mode **deferred**. | N/A |

---

## 5. Scan scope

### 5.1 Scan modes

| Mode | Trigger | Behavior |
|------|---------|----------|
| **A — User-selected root** | User picks a folder via SAF / DocumentPicker | Scan only that subtree; isolated `scan_root` record |
| **B — Default / no selection** | User starts scan without choosing a folder | Union of platform discovery surfaces the app can access — **not** a blind full-device crawl |

### 5.2 Platform mapping

**Android (API 26+):**

- MediaStore query: images, video, audio, downloads
- Optional SAF tree if user previously granted
- **No `MANAGE_EXTERNAL_STORAGE`** in v1
- Deep folder access requires user "Choose folder" (SAF)

**iOS (15+):**

- PHAsset fetch for photo library
- Security-scoped bookmarks from DocumentPicker
- Limited photo library: scan only authorized identifiers; show coverage gap in UX

### 5.3 Partial access UX

When coverage is less than 100% of user expectation:

- Show `CoverageBanner` with variant: `partial`, `denied`, or `limited-library`
- Scan summary footer: "Results cover authorized items only"
- Re-show banner on duplicate-group list if any member lies outside current grant
- **Marketing constraint:** Do not claim full-device scan without user granting access

---

## 6. User stories

### Scanning

| ID | Story | Priority |
|----|-------|----------|
| US-01 | As a user, I can start a scan of all storage I have granted access to, so I can find duplicates without picking a folder first. | Must |
| US-02 | As a user, I can choose a specific folder to scan, so I can limit scope to one subtree. | Must |
| US-03 | As a user, I see scan progress (files processed, groups found, reclaimable space) while scanning. | Must |
| US-04 | As a user, I can pause, cancel, or resume a scan. | Must |
| US-05 | As a user, I see which files could not be scanned and why, so I understand incomplete results. | Must |
| US-06 | As a user, I am told when my scan covers only part of my library (partial permissions), so I am not misled. | Must |
| US-07 | As a user, I can manually refresh to re-scan changed files without filesystem watchers. | Must |
| US-08 | As a user, I can opt in to hashing files larger than 2 GB (with battery warning). | Must |

### Review and delete

| ID | Story | Priority |
|----|-------|----------|
| US-09 | As a user, I can browse duplicate groups and see thumbnails, file types, and reclaimable space. | Must |
| US-10 | As a user, I must explicitly choose which file to keep before deleting duplicates. | Must |
| US-11 | As a user, I can use preset keeper rules (largest, newest, smallest file) but must confirm my choice. | Must |
| US-12 | As a user, I receive a two-step confirmation before any destructive delete. | Must |
| US-13 | As a user, I see how much space I can free by deleting non-keeper members. | Must |
| US-14 | As a user, I see when the same file appears at multiple paths (hard links / aliases). | Must |

### Settings and maintenance

| ID | Story | Priority |
|----|-------|----------|
| US-15 | As a user, I am prompted to rescan when an app update changes comparison rules. | Must |
| US-16 | As a user, I can use the app offline after permissions are granted. | Must |
| US-17 | As a user, crash analytics are opt-in and default off. | Must |

---

## 7. Functional requirements

### 7.1 Fingerprinting

| ID | Requirement |
|----|-------------|
| FR-FP-01 | Multi-stage pipeline: discovery → size bucket → quick sample (files > 50 MB: first + last 64 KiB SHA-256) → full content SHA-256 (1 MiB read buffer). |
| FR-FP-02 | Files with unique size skip further read except zero-byte `EMPTY:0`, **images** (must run `IMAGE_CONTENT_V1`), and **video** (must run `VIDEO_CONTENT_V1`; see FR-FP-07 / FR-FP-09). |
| FR-FP-03 | Files over 2 GB default cap marked `LARGE_SKIPPED` unless user opts in via settings. |
| FR-FP-04 | Per-file hash timeout 120 s → `HASH_TIMEOUT`. |
| FR-FP-05 | Group by `full_hash` (and normalized text hash where applicable). |
| FR-FP-06 | SHA-256 collision risk documented as negligible; no special mitigation beyond schema version reporting. |
| FR-FP-07 | **Video cross-resolution duplicates (v1):** For `media_type=video`, compute `VIDEO_CONTENT_V1` via native `VideoFingerprinter` (5 sampled frames, downscale ≤320×180, 64-bit dHash) in parallel with `RAW_BYTES`. Group as `SAME_CONTENT_VIDEO` when duration gate passes and ≥ 3 of 5 frame pairs match within Hamming threshold (ship default ≤ 8; QA tuning gate may raise to ≤ 10 under false-positive budget). |
| FR-FP-08 | `VIDEO_CONTENT_V1` resource caps: platform decoders only (MediaCodec/AVAssetReader), max 1 active decode session per process, 90 s wall-clock per file, 500 MB default fingerprint budget (2 GB when `settings.largeFiles` opt-in), and 32 MB header/index parse cap before decode. |
| FR-FP-09 | **Image re-encode / resize duplicates (v1):** For `media_type=image`, compute `IMAGE_CONTENT_V1` via native `ImageFingerprinter` (decode one representative frame, downscale ≤320×180, 64-bit dHash) in parallel with `RAW_BYTES`. Group as `SAME_CONTENT_IMAGE` when Hamming distance ≤ 8 (ship default; QA may tune ≤ 10 under false-positive budget). `EXACT_BYTES` groups take precedence (AC-equiv-image-content-04). |
| FR-FP-10 | `IMAGE_CONTENT_V1` resource caps: platform decoders only (`BitmapFactory` / `UIImage`), 30 s wall-clock per file, 50 MB default fingerprint budget (2 GB when `settings.largeFiles` opt-in). Failure maps to `IMAGE_DECODE_FAILED`. |

### 7.2 Index and catalog

| ID | Requirement |
|----|-------------|
| FR-IX-01 | Single merged SQLite catalog across all scan roots. |
| FR-IX-02 | Incremental scan: generation counter per `scan_run`; re-hash when mtime or size differs. |
| FR-IX-03 | Deleted paths tombstoned then purged after successful run. |
| FR-IX-04 | Hard links / multi-uri: same inode+device → one `file_entry_id`, multiple path alias rows. |
| FR-IX-05 | `schema_version` change triggers `full_rescan_required` flag and `RescanPromptBanner`. |

### 7.3 Scan integrity

| ID | Requirement |
|----|-------------|
| FR-SI-01 | Size/mtime change between stat and hash → discard hash, re-queue. |
| FR-SI-02 | Permission revoked mid-scan → pause run, retain partial results, show banner. |
| FR-SI-03 | Process kill → resume from checkpoint or offer restart. |
| FR-SI-04 | Native progress to JS throttled max 4 events/s (250 ms coalesce). |
| FR-SI-05 | Android FGS cancel: notification cleared ≤ 5 s typical, 120 s hard bound. |

### 7.4 Unscannable files

| ID | Requirement |
|----|-------------|
| FR-UN-01 | Never skip silently. Persist reason code on `file_entry`. |
| FR-UN-02 | Supported reason codes: `CLOUD_PLACEHOLDER`, `ENCRYPTED`, `PERMISSION_DENIED`, `OFFLINE_ONLY`, `LOCKED`, `LARGE_SKIPPED`, `HASH_TIMEOUT`, `VIDEO_DECODE_FAILED`, `IMAGE_DECODE_FAILED`. |
| FR-UN-03 | Invalid SAF/content URI → `PERMISSION_DENIED` (no separate enum in v1). |
| FR-UN-04 | Aggregate unscannable counts in `UnscannableSummaryCard` at scan end. |
| FR-UN-05 | Cloud/offline placeholders never hashed. |

### 7.5 Duplicate actions

| ID | Requirement |
|----|-------------|
| FR-AC-01 | Review-before-delete mandatory for all destructive actions. |
| FR-AC-02 | KeeperSelector presets: **largest** (defaultHighlighted), newest mtime, smallest file size. |
| FR-AC-03 | Delete button disabled until user explicitly selects a member or preset. |
| FR-AC-04 | `KeeperEducationSheet` shown once per session on first destructive action. |
| FR-AC-05 | Optional session toggle: "Use largest for the rest of this session" — not persisted across cold start. |
| FR-AC-06 | Reclaimable space = sum(size) of non-keeper members. |
| FR-AC-07 | Delete via platform APIs: Android MediaStore `deleteRequest` (API 11+), iOS `PHAssetChangeRequest`. |

### 7.6 Security and privacy

| ID | Requirement |
|----|-------------|
| FR-SE-01 | UriValidator before any stat/open/hash; fail-closed on validation failure. |
| FR-SE-02 | RedactionFilter on all crash SDK, opt-in analytics, and JS bridge error strings. |
| FR-SE-03 | Bridge errors emit `file_entry_id` + `unscannable_reason` + optional `scan_run_id` only — never `uri_or_path`. |
| FR-SE-04 | On-screen paths remain user-readable; redaction applies to telemetry egress only. |
| FR-SE-05 | No forensic wipe / secure erasure claims in store or marketing copy. |

---

## 8. Non-functional requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | Platform | React Native bare workflow; min Android API 26, iOS 15 |
| NFR-02 | Performance | Design for tens of thousands of files on mid-range phones (Pixel 6a / iPhone 12 class) |
| NFR-03 | Memory | Streaming hash with 1 MiB buffer; batch size 32 files; throttle on low battery/thermal |
| NFR-04 | Bridge | Zero content bytes cross JS bridge except throttled progress metadata |
| NFR-05 | Background | Android FGS `dataSync` with ongoing notification; iOS foreground-primary scan |
| NFR-06 | Privacy | On-device-only default; opt-in crash analytics default off |
| NFR-07 | Store | Compatible with App Store / Play policy; two-step delete confirm |
| NFR-08 | Accessibility | WCAG 2.2 AA target where applicable; 44×44 dp touch targets |
| NFR-09 | Localization | v1 EN strings frozen; localization in dot-release |

---

## 9. Permission UX and copy (EN v1 freeze)

Implement these strings verbatim from `tokens.ts`:

| Token | Value |
|-------|-------|
| `partial.android` | "Scanning media and downloads you've granted access to. Some folders aren't visible without choosing a folder." |
| `partial.ios.limited` | "Scanning {count} photos and videos you selected. Your full library isn't included." |
| `partial.cta.expand` | "Choose folder" (Android) / "Manage photo access" (iOS) |
| `denied.blocking` | "DupBuster needs storage access to find duplicates." + platform Settings deep link |
| `notification.scan.title` | "Scanning for duplicates" |
| `notification.scan.body` | "{percent}% · {filesProcessed} files" |
| `notification.channel.scan` | "Duplicate scan" |
| `keeper.rememberSession` | "Use largest for the rest of this session" |
| `settings.largeFiles` | "Hash files larger than 2 GB (uses more battery)" |
| `reclaimable.label` | "You can free up {size}" |
| `rescan.required` | "This update changed how files are compared. Rescan to refresh results." |
| `resume.interrupted` | "A scan was interrupted. Continue from where it stopped or start over." |
| `resume.cta.resume` | "Resume scan" |
| `resume.cta.restart` | "Start over" |
| `unscan.retry` | "Retry" |

**Android notification channel ID:** `com.dupbuster.scan.foreground.v1` (stable across upgrades)

**Store strings:** Derive `NSPhotoLibraryUsageDescription` and Android permission rationale from `denied.blocking` + `partial.*`.

---

## 10. Accessibility requirements

| ID | Requirement |
|----|-------------|
| A11Y-01 | `CoverageBanner` partial/denied: `accessibilityRole="alert"` on first display per session; polite on repeat same `coverageSessionKey`. |
| A11Y-02 | `CoverageBanner` limited-library: polite always. |
| A11Y-03 | Variant escalation (e.g. partial → denied mid-scan) resets first-display alert once per escalation via `coverageSessionKey`. |
| A11Y-04 | `ScanProgress`: `accessibilityLiveRegion="polite"`; announce at 10% `filesTotalKnown` OR 500 files delta. |
| A11Y-05 | Phase terminal announcements required: discovering, complete, paused, error, cancelling, cancelled. |
| A11Y-06 | `KeeperSelector`: radiogroup; largest defaultHighlighted without `selected=true` until explicit user activation. |
| A11Y-07 | `DeleteConfirmModal`: focus trap, heading focus on open, return focus to delete trigger on dismiss. |
| A11Y-08 | `PathChipList`: single label "Same file, {n} locations". |
| A11Y-09 | Reduced motion: pulsing dot instead of animated progress fill. |
| A11Y-10 | RTL: mirror progress bar; path strings stay LTR. |
| A11Y-11 | Dynamic type: no clipping at iOS XL / Android fontScale 1.3 on CoverageBanner and ScanProgress. |

See architecture doc for full `a11y.*` token list.

---

## 11. Acceptance criteria

### Equivalence (equiv-*)

| ID | Criterion |
|----|-----------|
| AC-equiv-text-01 | CRLF and LF-only text with identical NFC content → same duplicate group |
| AC-equiv-text-02 | UTF-8 BOM stripped; BOM vs no-BOM identical content → same group |
| AC-equiv-text-03 | NFC vs NFD composed identical content → same group |
| AC-equiv-text-04 | Trailing whitespace differs → **different** groups |
| AC-equiv-doc-01 | Byte-identical PDF copies → same group |
| AC-equiv-doc-02 | PDF vs exported DOCX of same logical content → **different** groups |
| AC-equiv-img-01 | Identical bytes, different paths → same group |
| AC-equiv-img-02 | Different on-disk bytes (e.g. different EXIF) → different groups |
| AC-equiv-av-01 | Identical container bytes → same group regardless of metadata interpretation |
| AC-equiv-video-xres-01 | Same source exported 1080p + 720p + 480p → one `SAME_CONTENT_VIDEO` group (≥ 3 members) |
| AC-equiv-video-xres-02 | Same resolution, different scene (including duration within gate negative pair) → different groups |
| AC-equiv-video-xres-03 | Same content H.264 vs H.265 → same `SAME_CONTENT_VIDEO` group |
| AC-equiv-video-xres-04 | Byte-identical video copies → `EXACT_BYTES` group and **no** separate `SAME_CONTENT_VIDEO` group |
| AC-equiv-video-xres-05 | Duration differs > 2% of min duration → different groups |
| AC-equiv-video-xres-05b | 59 s vs 60 s same content (1.0 s floor) → different groups |
| AC-equiv-video-xres-06 | 5% crop or >5% trim → different groups (documented v1 false-negative) |
| AC-equiv-video-xres-07 | Same content MP4 vs MOV container → same `SAME_CONTENT_VIDEO` group |
| AC-equiv-video-xres-08 | Clip < 3 s duration → single-frame fingerprint path still groups cross-resolution exports |
| AC-equiv-video-xres-09 | Malformed/truncated container → `VIDEO_DECODE_FAILED`; scan continues; aggregate row appears |
| AC-equiv-video-xres-10 | Video > 500 MB without opt-in: video content fingerprint skipped/marked unscannable; with opt-in: fingerprint completes or times out at 90 s wall-clock |
| AC-equiv-empty-01 | All zero-byte files → single `EMPTY:0` duplicate class |
| AC-equiv-symlink-01 | Symlink indexed separately; not grouped with target content |

### Unscannable (unscan-*)

| ID | Criterion |
|----|-----------|
| AC-unscan-01 | Each reason code appears in `UnscannableSummaryCard` with non-zero count when applicable |
| AC-unscan-02 | Cloud placeholder → `CLOUD_PLACEHOLDER`; never hashed |
| AC-unscan-03 | File > 2 GB without opt-in → `LARGE_SKIPPED` |
| AC-unscan-04 | Hash exceeds 120 s → `HASH_TIMEOUT` with Retry CTA |
| AC-unscan-05 | Invalid SAF URI → `PERMISSION_DENIED`; no silent skip |
| AC-unscan-06 | Video metadata parse/decode fails or caps exceeded → `VIDEO_DECODE_FAILED` (no crash) |

### Integrity (integrity-*)

| ID | Criterion |
|----|-----------|
| AC-integrity-progress-01 | ≤ 4 ScanEngine→JS events/s under 10k synthetic discovery load |
| AC-integrity-cancel-01 | User cancel → notification cleared ≤ 120 s; no orphan FGS in adb dumpsys |
| AC-integrity-toctou-01 | File grows/shrinks between stat and hash → hash discarded, re-queued |
| AC-integrity-toctou-02 | File deleted mid-hash → tombstoned; scan continues without crash |
| AC-integrity-perm-01 | Permission revoke mid-scan → pause; partial results retained |
| AC-integrity-resume-01 | Process kill during hash → resume from checkpoint or restart prompt |
| AC-integrity-hardlink-01 | Same inode+device, two paths → one `file_entry_id`; PathChipList shows aliases |

### Security (security-*)

| ID | Criterion |
|----|-----------|
| AC-security-redact-01 | Crash payload with `/storage/emulated/0/DCIM/test.jpg` → redacted or CI fails |
| AC-security-uri-01 | Crafted SAF docId with `..` segment → `PERMISSION_DENIED`; no open() syscall |
| AC-security-decode-01 | Malformed/truncated video container fixture → `VIDEO_DECODE_FAILED`; no native crash |
| AC-security-decode-02 | Pathological decode exceeds cap → terminates ≤ 90 s; no FGS hang |
| AC-security-blob-01 | Prod candidate: no frame-hash blob bytes in crash/analytics payloads (DEBUG blob off) |

### Actions (action-*)

| ID | Criterion |
|----|-----------|
| AC-action-keeper-01 | Delete disabled until explicit keeper selection despite largest defaultHighlighted |
| AC-action-keeper-02 | KeeperEducationSheet shown once per session on first delete attempt |
| AC-action-delete-01 | Two-step confirm required; no auto-delete |
| AC-action-reclaim-01 | Reclaimable label shows sum of non-keeper member sizes |

### Accessibility (a11y-*)

| ID | Criterion |
|----|-----------|
| AC-a11y-coverage-01 | Partial/denied banner alert on first display per session; polite on repeat |
| AC-a11y-coverage-02 | Limited-library banner polite always |
| AC-a11y-coverage-03 | Dismiss control 44×44 dp; Settings CTA link role without raw URI in announcement |
| AC-a11y-scan-01 | Live region polite; 10%/500-file cadence; phase terminal announcements |
| AC-a11y-keeper-01 | Radiogroup; no `selected=true` until explicit activation |
| AC-a11y-delete-01 | Modal focus trap and focus return |
| AC-a11y-path-01 | PathChipList single "Same file, {n} locations" label |
| AC-a11y-match-01 | Duplicate group list item announces match kind before count (`EXACT_BYTES`, `SAME_CONTENT_IMAGE`, `SAME_CONTENT_VIDEO`) |
| AC-a11y-match-02 | `SAME_CONTENT_IMAGE` / `SAME_CONTENT_VIDEO` group detail announces ContentMatchNotice once (polite); absent for `EXACT_BYTES` |
| AC-a11y-match-03 | MatchKindBadge includes readable text (not color-only) for all variants |
| AC-a11y-match-04 | ContentMatchNotice appears before delete button in accessibility traversal order |
| AC-a11y-match-05 | Hashing phase with video content work announces `a11y.scan.videoContent` once per phase entry |
| AC-equiv-image-content-01 | Three JPEGs of the same picture at different compression/sizes → one `SAME_CONTENT_IMAGE` group (≥3 members) |
| AC-equiv-image-content-02 | Hamming distance > threshold → not grouped |
| AC-equiv-image-content-03 | Different pictures → not grouped |
| AC-equiv-image-content-04 | Byte-identical image copies → `EXACT_BYTES` only; no competing `SAME_CONTENT_IMAGE` group |

---

## 12. Out of scope (v1 defer list)

- Audio re-encode near-duplicate matching
- Cropped images where less than ~50% of the frame overlaps (heavy crop / different aspect) — may not match `IMAGE_CONTENT_V1`
- Cross-format document deduplication (e.g. docx vs exported pdf)
- Cloud upload hashing
- `MANAGE_EXTERNAL_STORAGE` broad crawl
- Symlink follow mode
- Filesystem watchers (manual refresh + incremental mtime only)
- Payload-only audio/video hashing (container atom stripping)
- Account login or backend sync
- Auto-delete without user review
- Forensic secure wipe / unrecoverable delete claims
- Runtime theme switching

---

## 13. UI component requirements (M2 minimum)

| Component | Purpose |
|-----------|---------|
| `CoverageBanner` | Partial / denied / limited-library permission states |
| `ScanProgress` + `ScanStatusChip` | Scan progress and phase display |
| `UnscannableSummaryCard` | Aggregate unscannable counts by reason |
| `DuplicateGroupListItem` | Group summary in list view |
| `MatchKindBadge` | Shows `EXACT_BYTES`, `SAME_CONTENT_IMAGE`, `SAME_CONTENT_VIDEO` match kind |
| `KeeperSelector` | Keeper preset and member selection |
| `KeeperEducationSheet` | One-time delete safety education |
| `DeleteConfirmModal` | Two-step destructive confirm |
| `PathChipList` | Multi-path / hard-link display |
| `TextFileBadge` | Text duplicate indicator (no diff view) |
| `RescanPromptBanner` | Shown when `full_rescan_required` |
| `ContentMatchNotice` | Trust copy on `SAME_CONTENT_IMAGE` / `SAME_CONTENT_VIDEO` group detail |

---

## 14. References

- Results: `.agent/results/dupbuster-debate-results.md`
- Architecture: `.agent/deliverables/dupbuster/architecture.md`
- Implementation plan: `.agent/deliverables/dupbuster/implementation-plan.md`
- Debate floor: `.agent/debate-floors/dupbuster-debate.md`
