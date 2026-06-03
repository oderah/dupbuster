# DupBuster v1 — Store privacy disclosure (M4-09)

**Authority:** `docs/requirements.md` (NFR-06, FR-SE-02/04, US-17), `docs/architecture.md` §8.2, `docs/implementation-plan.md` §12.

**Product claim (both stores):** Duplicate detection runs entirely on the user’s device. User files, paths, hashes, and perceptual fingerprints are **not uploaded** to DupBuster or third-party backends in v1. Optional crash analytics (M4-13) are **opt-in and default off**; when disabled, no telemetry egress occurs via `ScanTelemetryEgress` / `DBScanTelemetryEgress`.

---

## 1. What the app does with user data

| Data | On-device use | Transmitted off-device (v1 default) |
|------|---------------|-------------------------------------|
| Photos / videos (PHAsset, MediaStore) | Read for hash + fingerprint; optional delete after two-step confirm | **No** |
| Audio / downloads / SAF documents | Read for hash; optional delete | **No** |
| File paths / display names | Shown in UI; redacted in crash/analytics egress | **No** (egress redacted; v1 analytics off by default) |
| SQLite catalog | Local index only | **No** |
| Unscannable reason codes | Stored locally; shown in app | **No** (not in allowed telemetry fields) |
| `frame_hashes_blob` | Debug builds only | **No** in prod (AC-security-blob-01) |

**Bridge law:** Progress and error events never include paths, hashes, or file bytes (`docs/architecture.md` §6.5).

---

## 2. Permission and in-app copy (must match store claims)

Frozen EN strings live in `src/tokens/tokens.ts`. Store-facing permission rationale must stay aligned:

| Surface | Source token | Purpose |
|---------|--------------|---------|
| iOS `NSPhotoLibraryUsageDescription` | `denied.blocking` | System photo-library prompt |
| In-app partial coverage (Android) | `partial.android` | Honest partial MediaStore/SAF scope |
| In-app limited library (iOS) | `partial.ios.limited` | Honest limited Photos selection |
| Denied state | `denied.blocking` + Settings CTA | No full-library claim |

**iOS Privacy Nutrition ↔ limited library:** The system permission string explains why access is needed; **limited-library** behavior is disclosed in-app via `CoverageBanner` (`partial.ios.limited`), not by overstating scope in the plist string.

---

## 3. Google Play — Data safety

Use the step-by-step answers in [`play-data-safety-answers.md`](./play-data-safety-answers.md).

**Summary for reviewers:**

- **Collect / share user data:** No (v1 prod candidate with analytics default off and no file upload).
- **Encryption in transit:** N/A when nothing is collected.
- **Photos, videos, files:** Processed on-device for duplicate detection; not collected or shared by the app.
- **Account / location / contacts / financial:** Not used.
- **Foreground service:** User-initiated local duplicate scan (`dataSync`); notification copy from `notification.*` tokens — no delete/remove wording.

**Play pre-launch (M4-11):** Declarations here must match the built APK/AAB (permissions in `AndroidManifest.xml`: media read + FGS only; `INTERNET` for RN dev tooling — not used to upload user files). Sign-off runbook: `tests/manual/m4-play-prelaunch-report.md`.

---

## 4. Apple App Store — App Privacy + Privacy Manifest

Use the step-by-step answers in [`ios-app-privacy-answers.md`](./ios-app-privacy-answers.md).

**Privacy Manifest (`ios/Dupbuster/PrivacyInfo.xcprivacy`):**

- `NSPrivacyCollectedDataTypes`: **empty** (no Apple-declared collected types in the manifest).
- `NSPrivacyTracking`: **false**.
- Required-reason APIs: file timestamps, UserDefaults, system boot time (RN / scan dependencies) — reasons documented in plist.

**App Privacy questionnaire (Connect):** Align with manifest + on-device-only processing. Photo Library access is for app functionality (scan/delete user-authorized assets), not tracking or advertising.

---

## 5. Prod promote checklist cross-reference

Before `fastlane … prod` (`docs/implementation-plan.md` §6.2), confirm:

- [ ] Play Data safety form submitted per §3 (this doc + `play-data-safety-answers.md`)
- [ ] App Store App Privacy submitted per §4 (`ios-app-privacy-answers.md`)
- [ ] `npm run test:store-privacy` green on the release tag
- [ ] Permission strings unchanged vs `tokens.ts` (or tokens updated with requirements doc amend)
- [ ] Store listing does not claim cloud sync, forensic erasure, or auto-delete (FR-SE-05, FR-AC-01)
- [ ] Privacy policy published at `https://dupbuster.app/privacy` per [`privacy-policy.md`](./privacy-policy.md) (M4-14 runbook: `tests/manual/m4-privacy-policy-url.md`)

M4-14 can ship in the same release window as M4-09; Play and App Store **Privacy policy URL** fields must match `src/config/privacyPolicyUrl.ts`.

---

## 6. When disclosures must be updated

Re-file store forms if any of the following change:

- Enabling crash/analytics SDK with default-on or new data types leaving the device
- Uploading hashes, paths, or file bytes to a server
- New permissions (e.g. network sync, accounts, location)
- `PrivacyInfo.xcprivacy` collected-data types no longer empty

Update this doc and `project-rules.mdc` §M4-09 in the same change as the product behavior.
