# App Store Connect — App Privacy answers (M4-09)

**Console path:** App Store Connect → *Your app* → **App Privacy** (Privacy Nutrition labels).

Complete for the **production candidate** IPA. Answers assume v1 **on-device-only** processing and **Privacy Manifest** with no collected data types.

---

## Privacy Manifest (bundled)

File: `ios/Dupbuster/PrivacyInfo.xcprivacy`

| Key | Value | Meaning for Connect |
|-----|-------|---------------------|
| `NSPrivacyCollectedDataTypes` | `[]` (empty) | Manifest declares no collected data types |
| `NSPrivacyTracking` | `false` | No tracking |
| `NSPrivacyAccessedAPITypes` | File timestamp, UserDefaults, system boot time | Required-reason API declarations only |

Run `npm run test:store-privacy` on the release branch before submit.

---

## App Privacy questionnaire

### Tracking

| Question | Answer |
|----------|--------|
| Do you or your third-party partners use data for tracking? | **No** |

### Collect data

| Question | Answer |
|----------|--------|
| Do you collect data from this app? | **No** (v1 default: no analytics SDK egress; manifest empty) |

If a future build enables **opt-in** crash analytics (M4-13), re-open the questionnaire and declare only the minimal diagnostic fields allowed in `docs/architecture.md` §8.2 (no paths, hashes, unscannable codes, or `frame_hashes_blob`).

---

## Data access vs collection (reviewer narrative)

DupBuster **accesses** Photo Library and document assets the user authorizes, but **does not collect** file content for transmission:

- Scanning and hashing run in native `ScanEngine` on-device.
- Limited Photos library is supported; in-app copy uses `partial.ios.limited` (not a false “full library” claim in the permission string).
- Delete uses `PHAssetChangeRequest` / platform APIs after in-app two-step confirm.

**Permission string (`Info.plist`):** Must match `tokens.denied.blocking` verbatim:

> DupBuster needs storage access to find duplicates.

---

## Photo Library (if Connect asks by category)

When linking questionnaire to user-facing behavior:

| Field | Value |
|-------|-------|
| Purpose | **App Functionality** (duplicate scan and optional delete) |
| Linked to user identity | **No** |
| Used for tracking | **No** |
| Collected | **No** (processed on device; not transmitted) |

Limited-library UX is explained in-app via `CoverageBanner` variant `limited-library` and `partial.ios.limited`.

---

## Other data categories

Declare **no collection** for: Contact Info, Health, Financial, Location, Sensitive, Contacts, Browsing History, Search History, Identifiers (no account system), Purchases, Usage Data (default off), Diagnostics (default off), Surroundings, Body, etc.

---

## External TestFlight (M4-12)

**Operator runbook:** `tests/manual/m4-testflight-external-beta.md` — execute after M4-10 iOS matrix on the same build; Beta App Review must approve before prod promote.

---

## Store listing consistency

- **Privacy policy URL:** M4-14 (link when published; must state on-device processing).
- **No claims** of cloud duplicate sync, forensic erasure, or automatic deletion (requirements FR-SE-05, FR-AC-01).

---

## Sign-off

| Role | Date | Build version |
|------|------|---------------|
| Release owner | | |
| Privacy review | | |

After sign-off, check **implementation-plan.md** §12 iOS Privacy Nutrition bullet.
