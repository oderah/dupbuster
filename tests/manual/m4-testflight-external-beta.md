# M4 — TestFlight external beta sign-off

**Task:** M4-12  
**Owner:** Release executes; QA reviews crashes and tester feedback  
**Authority:** `docs/implementation-plan.md` §6.2 (TestFlight external beta complete)

Apple **TestFlight external testing** exposes the **same release IPA** signed off in M4-10 (iOS 15/17/18 matrix) to external testers after **Beta App Review**. This document is the operator runbook and sign-off record before Fastlane **prod** / App Store submit.

**Independent of Android:** Play pre-launch (M4-11) does not satisfy this gate.

---

## 1. Prerequisites

| Item | Detail |
|------|--------|
| M4-10 | `tests/manual/m4-store-matrix-qa.md` §6 — iOS rows **Pass** on **this** build (`CFBundleVersion` / build number) |
| M4-09 | App Store Connect **App Privacy** filed per `docs/store/ios-app-privacy-answers.md` |
| Artifact | **Release IPA** — Fastlane `ios internal` (`upload_to_testflight`) or `release-matrix.yml` iOS archive signed for App Store |
| Upload | Build **Processed** in App Store Connect → **TestFlight** (typically 10–30 minutes after upload) |
| Automated preflight | `npm run test:testflight-external` + `npm run test:store-privacy` + `npm run test:store-matrix` green on release branch |

**Do not** enable external testing on a different build than the M4-10 iOS matrix. If the IPA changes, re-run M4-10 iOS slots and this sign-off.

---

## 2. Upload build (if not already on TestFlight)

1. From repo root (macOS + Xcode): `bundle exec fastlane ios internal` — uploads to TestFlight with `distribute_external: false` (internal testers only until §3).
2. Or upload the tagged **ios-release** artifact from `release-matrix.yml` via Transporter / `xcrun altool`.
3. App Store Connect → **TestFlight** → **iOS** → select build → wait until status is **Ready to Test** (not **Processing** / **Failed**).

Record in sign-off (§6): **Version** (marketing), **Build** number, upload date.

---

## 3. Enable external testing

1. [App Store Connect](https://appstoreconnect.apple.com) → DupBuster → **TestFlight** → **External Testing**.
2. Create or select an **External Group** (e.g. `DupBuster v1 external`).
3. **Add Build** → select the M4-10 candidate build.
4. Complete **Test Information** (what to test, contact email, sign-in if none).
5. **Submit for Beta App Review** (first external build per version, or when Apple requires re-review).
6. Wait for **Beta App Review Status: Approved** (typically hours to 2 days).
7. Add **External Testers** (email invites or public link per policy). Minimum: at least one tester per target OS band in §4, or document matrix-only coverage in §6.

External distribution uses Apple’s TestFlight app; testers must accept invite and install the listed build.

---

## 4. Required OS coverage

External pools are not guaranteed to hit every OS; **M4-10 matrix on the same build is the floor**. External beta confirms real-world installs and crash telemetry.

| Project gate | Expectation |
|--------------|-------------|
| iOS 15.x (≥ deployment 15.1) | M4-10 rows 14–22 **Pass** on `iOS15`, **or** external tester on 15.x with no launch crash |
| iOS 17.x | Same on `iOS17` slot or external 17.x |
| iOS 18.x | Same on `iOS18` slot or external 18.x |

If external testers cannot cover a band, **Pass** only when the corresponding M4-10 slot is **Pass** and noted in §6.

---

## 5. Checklist

**Legend:** **Req** = required for M4-12 sign-off.

| ID | Req | Area | Steps | Pass criteria |
|----|-----|------|-------|---------------|
| M4-TFEXT-01 | ✓ | Beta review | External group → build → **Beta App Review** | Status **Approved** for prod candidate build |
| M4-TFEXT-02 | ✓ | Stability | TestFlight → build → **Crashes** | **Zero** unmitigated crash clusters on cold launch for prod candidate |
| M4-TFEXT-03 | ✓ | Traceability | Compare build numbers | TestFlight **Build** = M4-10 iOS matrix build |
| M4-TFEXT-04 | ✓ | iOS 15 | M4-10 `iOS15` or external 15.x | Launch + scan screen without crash; limited-library honesty (M4-SMOKE-14) |
| M4-TFEXT-05 | ✓ | iOS 17 | M4-10 `iOS17` or external 17.x | Same as 04 |
| M4-TFEXT-06 | ✓ | iOS 18 | M4-10 `iOS18` or external 18.x | Same as 04 |
| M4-TFEXT-07 | ✓ | Privacy | App Privacy vs IPA | Questionnaire matches `ios-app-privacy-answers.md`; `PrivacyInfo.xcprivacy` empty collection |
| M4-TFEXT-08 | ✓ | Permission copy | Install build; trigger Photos prompt | `NSPhotoLibraryUsageDescription` matches `tokens.denied.blocking` (no “full library” overclaim) |
| M4-TFEXT-09 | ✓ | Limited library | S3 limited Photos grant | `CoverageBanner` `limited-library`; `partial.ios.limited` (M4-SMOKE-14) |
| M4-TFEXT-10 | ✓ | Export compliance | TestFlight build metadata | Encryption / export questions answered; no blocking compliance flag |
| M4-TFEXT-11 | ✓ | Tester feedback | External group feedback | No **unresolved blocking** issues (crash-on-launch, data loss, misleading delete) |
| M4-TFEXT-12 | ✓ | Delete path | M4-SMOKE-20/21 on matrix or external QA | PHAsset delete only after two-step in-app confirm (AC-action-delete-01) |
| M4-TFEXT-13 | ✓ | M4-10 overlap | Any TestFlight crash tied to Photos/BG | Cross-check M4-SMOKE-14–19; fix before prod promote |
| M4-TFEXT-14 | | Performance | Metrics → Disk/Energy (if shown) | Triage; **Pass** if no regression vs prior internal TestFlight or waived in §6 |
| M4-TFEXT-15 | | What’s New | Test Information for testers | Accurate: on-device duplicate scan; no cloud upload / auto-delete claims |

**Pass rule:** Every **Req** row = **Pass** (or **N/A** with Release approval and written rationale in §6).

---

## 6. Failure triage (quick reference)

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Beta review rejected — privacy | App Privacy mismatch | Update Connect + `docs/store/ios-app-privacy-answers.md`; `npm run test:store-privacy` |
| Beta review rejected — permission string | `Info.plist` drift | Align with `tokens.denied.blocking`; re-upload build |
| Launch crash on iOS 15 | RN / Photos API on older runtime | Reproduce on iOS 15 simulator; fix; re-run M4-SMOKE-14–16 |
| Crash after backgrounding | BG task / pause (M4-03) | M4-SMOKE-19; verify `BGTaskSchedulerPermittedIdentifiers` |
| Limited library shows full-device claim | Copy regression | `CoverageBanner` + `partial.ios.limited`; tokens test |
| Delete without in-app confirm | JS flow regression | AC-action-delete-01; M4-SMOKE-21 |
| Crash with no Photos grant | Expected post-permission only if matrix passes launch | Launch crashes still **Fail** |

Do not waive launch crashes on iOS 15/17/18 for v1 prod promote.

---

## 7. Sign-off

| ID | Result | Tester / Date | Notes |
|----|--------|---------------|-------|
| M4-TFEXT-01 | | | |
| M4-TFEXT-02 | | | |
| M4-TFEXT-03 | | | |
| M4-TFEXT-04 | | | |
| M4-TFEXT-05 | | | |
| M4-TFEXT-06 | | | |
| M4-TFEXT-07 | | | |
| M4-TFEXT-08 | | | |
| M4-TFEXT-09 | | | |
| M4-TFEXT-10 | | | |
| M4-TFEXT-11 | | | |
| M4-TFEXT-12 | | | |
| M4-TFEXT-13 | | | |

### Release sign-off

| Field | Value |
|-------|-------|
| Marketing version | |
| Build number | |
| Beta App Review approved (UTC) | |
| External group name | |
| External tester count | |
| Release approved for prod promote | Name / Date |

**M4-12 complete when:** all **Req** checklist rows **Pass**; Beta App Review **Approved**; iOS 15/17/18 coverage per §4; traceability row 03 matches M4-10 build; Release sign-off filled.

---

## 8. Relationship to M4 exit gates

| Gate | This document |
|------|----------------|
| implementation-plan §6.2 TestFlight external beta | Rows 01–02 + §4 OS bands |
| §6.2 after M4-10 matrix | Prerequisite §1 (iOS slots) |
| M4-11 Play pre-launch | Android-only; run in parallel after respective platform matrix |
| Fastlane prod (`DUPBUSTER_PROD_PROMOTE_APPROVED`) | Manual sign-off before setting flag |
| M2-11 VoiceOver smoke | M4-SMOKE-26; complements external feedback, not a substitute |

Re-run external beta after changes to `Info.plist`, `PrivacyInfo.xcprivacy`, Photos permission flow, `DBDeleteCoordinator`, or iOS launch/crash paths.
