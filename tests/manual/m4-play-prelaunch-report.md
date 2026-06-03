# M4 — Play pre-launch report sign-off

**Task:** M4-11  
**Owner:** Release executes; QA reviews failures  
**Authority:** `docs/implementation-plan.md` §6.2 (Play pre-launch API 26/33/34)

Google Play **Pre-launch report** runs automated tests on Firebase Test Lab devices against the **same release AAB** signed off in M4-10. This document is the operator runbook and sign-off record before Fastlane **prod** / production track submit.

---

## 1. Prerequisites

| Item | Detail |
|------|--------|
| M4-10 | `tests/manual/m4-store-matrix-qa.md` §6 signed off on **this** build (version code + version name) |
| M4-09 | Play Data safety filed per `docs/store/play-data-safety-answers.md` |
| Artifact | **Release AAB** — `android/app/build/outputs/bundle/release/app-release.aab` from tag build (`release-matrix.yml`) or Fastlane **internal** lane (not debug APK) |
| Track | Upload to **Internal testing** (or Closed testing) first — pre-launch runs on bundles in Play Console |
| Automated preflight | `npm run test:play-prelaunch` + `npm run test:store-privacy` + `npm run test:store-matrix` green on release branch |

**Do not** run pre-launch on a different build than the M4-10 matrix. If the bundle changes, re-run M4-10 and this report.

---

## 2. Upload and open pre-launch report

1. [Google Play Console](https://play.google.com/console) → DupBuster → **Test and release** → **Internal testing** (or your internal track).
2. **Create new release** → upload the release AAB → save (review not required for pre-launch on internal).
3. Open **Test and release** → **Pre-launch report** (or **Release** → **App bundle explorer** → select bundle → **Pre-launch report**).
4. Wait for status **Complete** (typically 15–60 minutes). Refresh if **In progress**.

Record in sign-off (§6): Play Console **version code**, **version name**, upload date, and report completion timestamp.

---

## 3. Required device / API coverage

Pre-launch device pools vary by Google; verify the report summary includes exercise on **low**, **mid**, and **high** API bands that map to project gates:

| Project gate | Pre-launch expectation |
|--------------|------------------------|
| API 26 (Android 8) | At least one device profile **API 26–27** with **no launch crash** |
| API 33 (Android 13) | At least one profile **API 33** with **no launch crash** |
| API 34 (Android 14) | At least one profile **API 34+** with **no launch crash** |

If a required band is **missing** from the report device list, note in §6 and run supplemental matrix rows on physical/AVD for that API (M4-10 §3) before prod promote.

---

## 4. Checklist

**Legend:** **Req** = required for M4-11 sign-off.

| ID | Req | Area | Steps | Pass criteria |
|----|-----|------|-------|---------------|
| M4-PRELAUNCH-01 | ✓ | Stability | Open **Issues** → Crashes | **Zero** unmitigated crash clusters on cold launch for prod candidate |
| M4-PRELAUNCH-02 | ✓ | Stability | **ANRs** tab | **Zero** unmitigated ANRs on launch or idle home |
| M4-PRELAUNCH-03 | ✓ | API 26 | Filter devices API 26–27 | Launch + navigate to scan screen without crash |
| M4-PRELAUNCH-04 | ✓ | API 33 | Filter devices API 33 | Same as 03 |
| M4-PRELAUNCH-05 | ✓ | API 34 | Filter devices API 34+ | Same as 03 |
| M4-PRELAUNCH-06 | ✓ | Permissions | **Policy** / permission warnings | No **undeclared** sensitive permissions; manifest matches `docs/store/play-data-safety-answers.md` § Permissions alignment |
| M4-PRELAUNCH-07 | ✓ | FGS | Review FGS / background warnings | Only `dataSync` FGS for scan (`ScanForegroundService`); notification copy has no "delete"/"remove" (AC-integrity-cancel-01 / project-rules §6) |
| M4-PRELAUNCH-08 | ✓ | Privacy | Data safety vs APK | Declared data types = **not collected** for user files; no location/contacts in manifest |
| M4-PRELAUNCH-09 | ✓ | Security | **Security** section (if shown) | No blocking issues; any **medium+** findings triaged with Release + documented waiver in §6 Notes |
| M4-PRELAUNCH-10 | ✓ | Accessibility | Pre-launch a11y findings (if any) | Critical issues **fixed** or mapped to open M2-11 / M2-10 items with Release approval |
| M4-PRELAUNCH-11 | ✓ | Screenshots | Visual review of captured screens | No crash dialogs; coverage banners use frozen tokens (no debug paths in **telemetry** — on-screen user paths OK) |
| M4-PRELAUNCH-12 | ✓ | Traceability | Compare build IDs | Pre-launch AAB **version code** = M4-10 matrix build |
| M4-PRELAUNCH-13 | ✓ | M4-10 overlap | If pre-launch flags permission/FGS issue | Cross-check M4-SMOKE-06–08 on A33/A34; do not prod promote until resolved |
| M4-PRELAUNCH-14 | | Performance | Startup / battery warnings | Triage; **Pass** if no regression vs prior internal baseline or waived in §6 |
| M4-PRELAUNCH-15 | | Size | Large APK/AAB warnings | Informational unless Play blocks — note in §6 |

**Pass rule:** Every **Req** row = **Pass** (or **N/A** with Release approval and written rationale in §6).

---

## 5. Failure triage (quick reference)

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Launch crash on API 26 | Legacy storage permission path | Fix permission flow; re-run M4-SMOKE-01–03 on A26 |
| Launch crash on API 33/34 | `READ_MEDIA_*` or RN init | Check `AndroidManifest.xml`; verify release not debug-only ABI split issue |
| FGS policy warning | Wrong `foregroundServiceType` or misleading notification | Confirm `dataSync` + `tokens.notification.*`; re-run M4-SMOKE-06–08 |
| Permission / data safety mismatch | Form vs manifest drift | Update Play form or manifest; `npm run test:store-privacy` |
| Crash during scan in pre-launch | Test Lab has no media grants | Expected if crash is **post-permission** only — reproduce on M4-10 matrix with grants; launch-time crashes still **Fail** |
| Security: `..` URI | UriValidator regression | `npm run test:security-gates` (AC-security-uri-01) |

Do not waive launch crashes on API 26/33/34 for v1 prod promote.

---

## 6. Sign-off

| ID | Result | Tester / Date | Notes |
|----|--------|---------------|-------|
| M4-PRELAUNCH-01 | | | |
| M4-PRELAUNCH-02 | | | |
| M4-PRELAUNCH-03 | | | |
| M4-PRELAUNCH-04 | | | |
| M4-PRELAUNCH-05 | | | |
| M4-PRELAUNCH-06 | | | |
| M4-PRELAUNCH-07 | | | |
| M4-PRELAUNCH-08 | | | |
| M4-PRELAUNCH-09 | | | |
| M4-PRELAUNCH-10 | | | |
| M4-PRELAUNCH-11 | | | |
| M4-PRELAUNCH-12 | | | |
| M4-PRELAUNCH-13 | | | |

### Release sign-off

| Field | Value |
|-------|-------|
| Play version code | |
| Play version name | |
| AAB SHA-256 (optional) | |
| Pre-launch completed (UTC) | |
| Release approved for prod promote | Name / Date |

**M4-11 complete when:** all **Req** checklist rows **Pass**; API 26/33/34 launch stability confirmed (rows 03–05); traceability row 12 matches M4-10 build; Release sign-off filled.

---

## 7. Relationship to M4 exit gates

| Gate | This document |
|------|----------------|
| implementation-plan §6.2 Play pre-launch API 26/33/34 | Rows 03–05 + stability 01–02 |
| §6.2 after M4-10 matrix | Prerequisite §1 |
| Fastlane prod (`DUPBUSTER_PROD_PROMOTE_APPROVED`) | Manual sign-off before setting flag |
| M4-12 TestFlight external | `tests/manual/m4-testflight-external-beta.md` — independent; Android pre-launch does not replace iOS beta |

Re-run pre-launch after changes to `AndroidManifest.xml`, FGS, permission flows, or native crash paths on startup.
