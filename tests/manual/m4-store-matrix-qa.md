# M4 — Store-matrix QA (API / OS versions)

**Task:** M4-10  
**Owner:** QA executes; Release signs off before prod promote  
**Authority:** `docs/implementation-plan.md` §9.3 (device matrix), §6.2 (Play pre-launch API 26/33/34)

Manual verification on **six OS slots** before production store submit. Complements automated gates (`npm run test:security-gates`, `npm run test:a11y`, `npm run test:store-privacy`) and M2-11 VoiceOver/TalkBack smoke.

---

## 1. Prerequisites

| Item | Detail |
|------|--------|
| Build | **Release candidate** — same artifact as Play internal / TestFlight external (tag from `release-matrix.yml` or Fastlane internal lane). Not Metro debug unless row says otherwise. |
| Android slots | Physical device or AVD at **API 26**, **API 33**, and **API 34** (see §3). `minSdkVersion` 26 per `android/build.gradle`. |
| iOS slots | Device or simulator at **iOS 15.x** (≥ deployment 15.1), **17.x**, and **18.x** |
| Fixtures | Seed each slot with at least: 2 duplicate photos (or re-encoded pair), 1 short video pair if available, 1 SAF-accessible folder (Android Mode A / iOS DocumentPicker) |
| Automated preflight | `npm run test:store-matrix` + `npm run test:store-privacy` + `npm run test:a11y` green on release branch |
| Privacy forms | M4-09 Play Data safety + App Privacy filed per `docs/store/` |

---

## 2. How to record results

Copy the sign-off table (§6) into the release ticket. For each row:

| Column | Fill |
|--------|------|
| **Result** | `Pass` / `Fail` / `Blocked` / `N/A` |
| **Slot** | `A26` / `A33` / `A34` / `iOS15` / `iOS17` / `iOS18` |
| **Notes** | Build version, device model, failure steps, `adb dumpsys` / screenshot link |
| **Tester / Date** | Initials + ISO date |

**Pass rule for M4-10:** Every row marked **Required** must **Pass** on each listed slot column (or documented `N/A` with Release approval). Optional rows may be `N/A` if the slot lacks hardware (e.g. no physical device) — note in sign-off.

---

## 3. Emulator / device setup (appendix)

### Android AVDs (Android Studio SDK Manager)

| API | Suggested system image | Notes |
|-----|------------------------|-------|
| 26 | `google_apis` x86_64, Android 8.0 | Validates `READ_EXTERNAL_STORAGE` path (`maxSdkVersion=32` in manifest) |
| 33 | `google_apis` x86_64, Android 13 | Granular `READ_MEDIA_*` only |
| 34 | `google_apis` x86_64, Android 14 | FGS `dataSync` type + current scoped-storage behavior |

```bash
# Example (adjust package path from sdkmanager --list)
sdkmanager "system-images;android-26;google_apis;x86_64"
sdkmanager "system-images;android-33;google_apis;x86_64"
sdkmanager "system-images;android-34;google_apis;x86_64"

avdmanager create avd -n dupbuster-api26 -k "system-images;android-26;google_apis;x86_64" -d pixel
avdmanager create avd -n dupbuster-api33 -k "system-images;android-33;google_apis;x86_64" -d pixel
avdmanager create avd -n dupbuster-api34 -k "system-images;android-34;google_apis;x86_64" -d pixel
```

Install release/internal APK: `adb install -r android/app/build/outputs/apk/release/app-release.apk` (or internal track build).

**WSL:** Use `npm run android` / `scripts/run-android-wsl.sh` per `project-rules.mdc` §8.1.

### iOS simulators (Xcode)

```bash
xcrun simctl list devices available
# Create if missing: iOS 15.x, 17.x, 18.x runtimes
xcodebuild -workspace ios/Dupbuster.xcworkspace -scheme Dupbuster -configuration Release -destination 'platform=iOS Simulator,name=iPhone 15,OS=17.5' build
```

Install TestFlight or archive build on each runtime. Limited-library tests require **Select Photos…** on first Photos prompt (not **Allow Access to All Photos**).

---

## 4. Scenario setup (appendix)

### S0 — Fresh install

1. Uninstall DupBuster (or clear app data).  
2. Install release candidate.  
3. Cold launch.

### S1 — Android partial media (API 33+)

1. S0.  
2. Grant only **Photos** or **Videos** (deny audio).  
3. Expect `CoverageBanner` variant `partial` + `tokens.partial.android`.

### S2 — Android denied

1. S0.  
2. Deny all `READ_MEDIA_*` / storage; tap **Don't ask again** if shown.  
3. Expect `CoverageBanner` `denied`.

### S3 — iOS limited library

1. S0 on iOS.  
2. Photos prompt → **Select Photos…** → pick subset.  
3. Expect `CoverageBanner` `limited-library` + `tokens.partial.ios.limited`.

### S4 — iOS full library

1. S0 on iOS.  
2. Photos prompt → **Allow Access to All Photos**.  
3. No limited-library banner; Mode B scan can discover granted assets.

### S5 — Mode A SAF / DocumentPicker

1. S0.  
2. Grant storage/photos as needed.  
3. Use **Choose folder** (or app flow that opens SAF / DocumentPicker).  
4. Pick a tree with known duplicate files.  
5. Start scan — discovery Mode A only under granted tree.

### S6 — Mode B platform discovery

1. S0 with media permissions granted (not denied).  
2. Do **not** pick a custom folder (or clear Mode A grant).  
3. Start scan — MediaStore / PHAsset union (Mode B).

### S7 — Scan to complete

1. S5 or S6 with duplicates present.  
2. Run scan to `complete`.  
3. Open duplicate group → select keeper → two-step delete (M3).

### S8 — Cancel during active scan (Android FGS)

1. S6 on Android.  
2. Start scan; when FGS notification appears, tap **Cancel scan** in app.  
3. Verify notification clears (AC-integrity-cancel-01).

---

## 5. Smoke matrix

**Legend**

| Shorthand | Meaning |
|-----------|---------|
| **Req** | Required for M4-10 sign-off on listed slots |
| **AC** | Acceptance criterion ID in `docs/requirements.md` §11 |

### 5.1 Android — Permissions & scoped storage

| ID | Req | Ref | Setup | Steps | Expected | A26 | A33 | A34 |
|----|-----|-----|-------|-------|----------|-----|-----|-----|
| M4-SMOKE-01 | ✓ | FR-UN-04 | S2 | Launch app | `CoverageBanner` denied; no crash; scan start blocked or honest empty state | ✓ | ✓ | ✓ |
| M4-SMOKE-02 | ✓ | FR-UN-04 | S1 | Launch | Partial banner + expand CTA; scan only granted media types | | ✓ | ✓ |
| M4-SMOKE-03 | ✓ | NFR-01 | S6 | Mode B scan | Discovers images/video in MediaStore without `/sdcard` crawl | ✓ | ✓ | ✓ |
| M4-SMOKE-04 | ✓ | FR-UN-03 | S5 | Mode A invalid tree | No silent skip of denied URIs; unscannable or deny surfaced | ✓ | ✓ | ✓ |
| M4-SMOKE-05 | ✓ | AC-unscan-05 | S5 | Revoke SAF access mid-scan | Run pauses or errors with `PERMISSION_DENIED`; partial groups if applicable (M3-08) | ✓ | ✓ | ✓ |

*API 26 note:* Row 02 may use legacy storage permission dialog (`READ_EXTERNAL_STORAGE`) instead of `READ_MEDIA_*` — still must show partial/denied honesty.

### 5.2 Android — Foreground service & cancel

| ID | Req | Ref | Setup | Steps | Expected | A26 | A33 | A34 |
|----|-----|-----|-------|-------|----------|-----|-----|-----|
| M4-SMOKE-06 | ✓ | FR-SI-05, NFR-05 | S6 | Start scan | Ongoing notification visible; channel `com.dupbuster.scan.foreground.v1`; title/body from `tokens.notification.*`; **no** "delete"/"remove" in notification | ✓ | ✓ | ✓ |
| M4-SMOKE-07 | ✓ | AC-integrity-cancel-01 | S8 | Cancel scan | Phase `cancelling` → `cancelled`; notification cleared ≤120 s; `adb shell dumpsys activity services` shows no orphan DupBuster FGS | ✓ | ✓ | ✓ |
| M4-SMOKE-08 | ✓ | project-rules §6 | S6 | Inspect notification channel (Settings → Apps → DupBuster → Notifications) | Channel name/description match frozen tokens; not misleading "complete" while scan running | ✓ | ✓ | ✓ |

### 5.3 Android — SAF picker (Mode A)

| ID | Req | Ref | Setup | Steps | Expected | A26 | A33 | A34 |
|----|-----|-----|-------|-------|----------|-----|-----|-----|
| M4-SMOKE-09 | ✓ | FR-SI-01 | S5 | Pick folder via SAF | Scan completes; duplicate groups only from granted tree | ✓ | ✓ | ✓ |
| M4-SMOKE-10 | ✓ | AC-security-uri-01 | — | *(Automated)* | `SecurityUriCiGateTest` green in CI — crafted `..` docId denied | CI | CI | CI |
| M4-SMOKE-11 | ✓ | FR-AC-07 | S7 | Delete duplicates (Mode A member) | System SAF delete confirm; catalog updates; keeper retained | ✓ | ✓ | ✓ |

### 5.4 Android — MediaStore delete (Mode B)

| ID | Req | Ref | Setup | Steps | Expected | A26 | A33 | A34 |
|----|-----|-----|-------|-------|----------|-----|-----|-----|
| M4-SMOKE-12 | ✓ | FR-AC-07 | S7 | Delete Mode B media duplicate | API 30+: `createDeleteRequest` batch confirm; API 26–29: platform delete path still succeeds or shows recoverable confirm | ✓ | ✓ | ✓ |
| M4-SMOKE-13 | ✓ | AC-action-delete-01 | S7 | Delete flow | Two-step in-app confirm **before** system delete sheet | ✓ | ✓ | ✓ |

### 5.5 iOS — Photo library (limited vs full)

| ID | Req | Ref | Setup | Steps | Expected | iOS15 | iOS17 | iOS18 |
|----|-----|-----|-------|-------|----------|-------|-------|-------|
| M4-SMOKE-14 | ✓ | A11Y-02 | S3 | Launch | `limited-library` banner; polite (not alert); copy `partial.ios.limited` | ✓ | ✓ | ✓ |
| M4-SMOKE-15 | ✓ | FR-UN-04 | S4 | Mode B scan | Discovers only authorized PHAssets; no false "full device" claim | ✓ | ✓ | ✓ |
| M4-SMOKE-16 | ✓ | FR-UN-04 | S2 equiv (denied) | Deny Photos | `CoverageBanner` denied + Settings CTA | ✓ | ✓ | ✓ |
| M4-SMOKE-17 | ✓ | — | S3 | Expand coverage CTA | Opens photo picker / limited library expansion per platform | ✓ | ✓ | ✓ |

### 5.6 iOS — Scoped bookmarks (Mode A)

| ID | Req | Ref | Setup | Steps | Expected | iOS15 | iOS17 | iOS18 |
|----|-----|-----|-------|-------|----------|-------|-------|-------|
| M4-SMOKE-18 | ✓ | FR-SI-01 | S5 | DocumentPicker folder | Scan discovers files under scoped URL; security-scoped access held for hash | ✓ | ✓ | ✓ |
| M4-SMOKE-19 | ✓ | M4-03 | S6 | Background during scan | Send app to background → scan pauses or BG continuation; no silent data loss; resume prompt if killed (M3-09) | ✓ | ✓ | ✓ |

### 5.7 iOS — PHAsset delete

| ID | Req | Ref | Setup | Steps | Expected | iOS15 | iOS17 | iOS18 |
|----|-----|-----|-------|-------|----------|-------|-------|-------|
| M4-SMOKE-20 | ✓ | FR-AC-07 | S7 | Delete PHAsset duplicate | System Photos delete confirm; keeper remains; group updates | ✓ | ✓ | ✓ |
| M4-SMOKE-21 | ✓ | AC-action-delete-01 | S7 | Delete flow | DupBuster two-step confirm before `PHAssetChangeRequest` | ✓ | ✓ | ✓ |
| M4-SMOKE-22 | ✓ | AC-a11y-match-02 | S7 | `SAME_CONTENT_VIDEO` group | `ContentMatchNotice` visible before delete control | ✓ | ✓ | ✓ |

### 5.8 Cross-slot integrity (any one slot per row)

| ID | Req | Ref | Setup | Steps | Expected | Slots |
|----|-----|-----|-------|-------|----------|-------|
| M4-SMOKE-23 | ✓ | AC-integrity-resume-01 | Kill app mid-scan | Force-quit during hashing; relaunch | `ResumePromptBanner`; resume skips checkpoint; restart abandons | Any Android + any iOS |
| M4-SMOKE-24 | ✓ | FR-IX-05 | Schema bump build | Install over migrated DB | `RescanPromptBanner` when `fullRescanRequired` | Any |
| M4-SMOKE-25 | ✓ | FR-FP-03 | S7 | Large file skipped | Enable **Scan large files** setting; rescan | Previously `LARGE_SKIPPED` files hash | Any Android |
| M4-SMOKE-26 | ✓ | M2-11 | — | Manual a11y | M2 matrix signed off (VoiceOver + TalkBack) | See `tests/manual/m2-voiceover-talkback-smoke-matrix.md` |
| M4-SMOKE-27 | ✓ | M4-09 | — | Store forms | Play Data safety + App Privacy submitted | Console |

---

## 6. Sign-off

| Row IDs | A26 | A33 | A34 | iOS15 | iOS17 | iOS18 |
|---------|-----|-----|-----|-------|-------|-------|
| M4-SMOKE-01 | | | | — | — | — |
| M4-SMOKE-02 | | | | — | — | — |
| M4-SMOKE-03 | | | | — | — | — |
| M4-SMOKE-04 | | | | — | — | — |
| M4-SMOKE-05 | | | | — | — | — |
| M4-SMOKE-06 | | | | — | — | — |
| M4-SMOKE-07 | | | | — | — | — |
| M4-SMOKE-08 | | | | — | — | — |
| M4-SMOKE-09 | | | | — | — | — |
| M4-SMOKE-11 | | | | — | — | — |
| M4-SMOKE-12 | | | | — | — | — |
| M4-SMOKE-13 | | | | — | — | — |
| M4-SMOKE-14 | — | — | — | | | |
| M4-SMOKE-15 | — | — | — | | | |
| M4-SMOKE-16 | — | — | — | | | |
| M4-SMOKE-17 | — | — | — | | | |
| M4-SMOKE-18 | — | — | — | | | |
| M4-SMOKE-19 | — | — | — | | | |
| M4-SMOKE-20 | — | — | — | | | |
| M4-SMOKE-21 | — | — | — | | | |
| M4-SMOKE-22 | — | — | — | | | |
| M4-SMOKE-23 | | | | | | |
| M4-SMOKE-24 | | | | | | |
| M4-SMOKE-25 | | | | | | |
| M4-SMOKE-26 | | | | | | |
| M4-SMOKE-27 | | | | | | |

### Release sign-off

| Role | Name | Date | Build (tag / version) |
|------|------|------|------------------------|
| QA executed (Android matrix) | | | |
| QA executed (iOS matrix) | | | |
| Release approved for prod promote | | | |

**M4-10 complete when:** all **Req** rows **Pass** on every applicable slot; M4-SMOKE-10 confirmed via CI; M4-SMOKE-26–27 documented; Release row signed. Feeds **M4-11** Play pre-launch (same API 26/33/34 builds).

---

## 7. Relationship to M4 exit gates

| Gate | Row IDs |
|------|---------|
| implementation-plan §9.3 device matrix | 01–22 per platform slots |
| §6.2 Play pre-launch API 26/33/34 | 01–13 on A26/A33/A34 |
| §6.2 FGS cancel ≤120 s | 07–08 |
| §6.2 manual a11y smoke | 26 |
| §6.2 store privacy forms | 27 |
| M4-11 pre-launch report | `tests/manual/m4-play-prelaunch-report.md` on **same** AAB after matrix green |
| M4-12 TestFlight external | `tests/manual/m4-testflight-external-beta.md` on **same** IPA after iOS matrix green |

Re-run matrix after changes to ScanEngine discovery, FGS, delete coordinator, or permission flows.
