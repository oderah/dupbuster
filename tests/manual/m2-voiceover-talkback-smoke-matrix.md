# M2 — VoiceOver / TalkBack manual smoke matrix

**Task:** M2-11  
**Owner:** QA executes; Accessibility signs off  
**Companion:** M2-10 automated gate — `npm run test:a11y` (zero critical violations on gate components)

This matrix covers screen-reader behavior that Jest cannot assert: interruption timing, focus order on device, live-region cadence under real VO/TB, dynamic type clipping, RTL layout, and reduced-motion visuals.

---

## 1. Prerequisites

| Item | Detail |
|------|--------|
| Build | Debug/internal build from current `dev` branch (`npm run android` / `npm run ios`) |
| M2 shell | `App.tsx` uses `createNativeScanEnginePort(NativeScanEngine)` — progress + duplicate groups from live scan/catalog |
| Automated gate | `npm run test:a11y` passes before manual session |
| Devices | **One iOS 15+** device/simulator with VoiceOver; **One Android API 26+** device/emulator with TalkBack |
| Screen readers | iOS: Settings → Accessibility → VoiceOver. Android: Settings → Accessibility → TalkBack |
| Dynamic type baseline | iOS: Settings → Display & Brightness → Text Size → default. Android: font scale **1.0** unless row specifies otherwise |

---

## 2. How to record results

Copy the sign-off table (§6) into your QA ticket or spreadsheet. For each row:

| Column | Fill |
|--------|------|
| **Result** | `Pass` / `Fail` / `Blocked` / `N/A` |
| **Platform** | `iOS-VO` / `Android-TB` / both |
| **Notes** | Failure steps, build SHA, screenshot/recording link |
| **Tester / Date** | Initials + ISO date |

**Blocked** = component or milestone not shipped yet (see §4). Do not fail M2 sign-off for blocked rows; re-run when the blocking task lands.

---

## 3. Scenario setup (appendix)

Use these setups before running rows that reference them.

### S0 — Fresh app session

1. Force-quit DupBuster.  
2. Relaunch (cold start).  
3. Enable VoiceOver or TalkBack **before** first interaction.

### S1 — Partial coverage (Android)

1. S0.  
2. Deny one or more `READ_MEDIA_*` permissions; grant at least one (or grant none but leave not permanently blocked).  
3. Observe `CoverageBanner` variant `partial`.

### S2 — Denied coverage

1. S0.  
2. Deny all media permissions and tap **Don't ask again** (Android) or deny Photos (iOS) until blocked.  
3. Observe `CoverageBanner` variant `denied`.

### S3 — Limited library (iOS only)

1. S0 on iOS.  
2. When Photos permission prompts, choose **Select Photos…** and pick a subset.  
3. Observe `CoverageBanner` variant `limited-library`.

### S4 — Mock scan (progress + groups)

1. S0 with permissions sufficient to reach scan screen (any coverage state OK).  
2. Focus **Start scan** (`tokens.notification.scan.title`) and activate.  
3. Mock engine runs phases: `discovering` → `hashing` (includes `contentKind=video_content` step) → `grouping` → `complete` (~50 ms total).  
4. After `complete`, duplicate group list + unscannable summary appear from mock catalog.

### S5 — Pause during scan

1. Start S4.  
2. As soon as footer controls appear, activate **Pause scan** (`a11y.scan.pause`).  
3. Confirm paused phase before scan completes.

### S6 — Cancel during scan

1. Start S4.  
2. Activate **Cancel scan** (`a11y.scan.cancel`) before `complete`.  
3. Wait for terminal `cancelled` phase.

### S7 — Rescan prompt

Requires catalog meta `fullRescanRequired: true`. Until live bridge exposes this from a migrated DB, use a **QA-only dev build**:

```typescript
// App.tsx — revert before merge; QA session only
createMockScanEnginePort({
  catalogMeta: { schemaVersion: 2, fullRescanRequired: true },
});
```

Rebuild, S0, launch — `RescanPromptBanner` appears below coverage (if any).

### S8 — Dynamic type / large font

| Platform | Setting |
|----------|---------|
| iOS | Settings → Accessibility → Display & Text Size → Larger Text → **AX5 (Extra Extra Extra Large)** or max slider |
| Android | Settings → Display → Font size → **Largest** (target **fontScale ≥ 1.3**; verify in Developer options if needed) |

Relaunch DupBuster after changing system setting.

### S9 — RTL layout (optional M2 row)

Android: Developer options → **Force RTL layout direction**.  
iOS: Settings → General → Language & Region → add Arabic/Hebrew, set as primary (or use RTL-capable simulator locale).  
Relaunch app, run S4, inspect progress track direction.

### S10 — Reduced motion

| Platform | Setting |
|----------|---------|
| iOS | Settings → Accessibility → Motion → **Reduce Motion** ON |
| Android | Settings → Accessibility → **Remove animations** (or Reduce motion if present) ON |

Run S4; progress indicator should use **static pulse dot** instead of animated fill (`ScanProgress` `reducedMotion` follows system via `useReducedMotion` in `App.tsx`).

### S11 — Open duplicate group detail

1. Complete S4.  
2. Activate the mock duplicate group list item (`SAME_CONTENT_VIDEO`, 2 members).  
3. Detail modal opens with `KeeperSelector`.

### S12 — Coverage escalation (partial → denied)

1. S1 (partial banner visible).  
2. Without dismissing banner, revoke all media permissions in system settings (Android app permissions / iOS Photos → None).  
3. Return to DupBuster (background → foreground triggers permission refresh).  
4. Banner should escalate to `denied` with **new** `coverageSessionKey` → first-display **alert** once (A11Y-03).

---

## 4. Blocked / deferred rows

| Row IDs | Blocked until | Reason |
|---------|---------------|--------|
| M2-SMOKE-30 | M3-01 | `DeleteConfirmModal` not implemented |
| M2-SMOKE-31 | M3-02 | `PathChipList` not implemented |
| M2-SMOKE-29 (delete focus order) | M3 | Delete button not implemented — notice-before-delete partial until M3 |

---

## 5. Smoke matrix

**Legend — Live region / role**

| Shorthand | Meaning |
|-----------|---------|
| **Alert** | `accessibilityRole="alert"` — interrupts VO/TB |
| **Polite** | `accessibilityLiveRegion="polite"` — queued announcement |
| **Link** | CTA uses `accessibilityRole="link"` + hint, no raw URI spoken |

### 5.1 CoverageBanner

| ID | Ref | Setup | Steps | Expected (VoiceOver / TalkBack) | iOS | Android |
|----|-----|-------|-------|----------------------------------|-----|---------|
| M2-SMOKE-01 | A11Y-01, AC-a11y-coverage-01 | S1 | Focus banner on first launch | Message read as **Alert** (interrupts). Copy matches `tokens.partial.android` (Android) or partial iOS variant. | ✓ | ✓ |
| M2-SMOKE-02 | A11Y-01 | S1 | Dismiss banner, kill app, relaunch same partial state | Second display: **Polite** live region; **not** alert interruption | ✓ | ✓ |
| M2-SMOKE-03 | A11Y-02, AC-a11y-coverage-02 | S3 | Focus limited-library banner | **Polite** always; **never** alert role | ✓ | N/A |
| M2-SMOKE-04 | A11Y-01 | S2 | Focus denied banner (first display) | **Alert**; copy matches `tokens.denied.blocking` | ✓ | ✓ |
| M2-SMOKE-05 | AC-a11y-coverage-03 | S1 or S2 | Locate dismiss control | Label `a11y.coverage.dismiss`; hint `a11y.coverage.dismissHint`; touch target ≥ 44×44 dp | ✓ | ✓ |
| M2-SMOKE-06 | AC-a11y-coverage-03 | S2 | Locate Settings / expand CTA | Denied: **link** role + `a11y.coverage.settingsHint`; **no** raw `settings:` URI spoken | ✓ | ✓ |
| M2-SMOKE-07 | A11Y-03, AC-a11y-coverage-01 | S12 | Escalate partial → denied | New session key → **alert** fires once on escalation; subsequent display polite | ✓ | ✓ |
| M2-SMOKE-08 | A11Y-11 | S1 + S8 | Inspect banner at large font | No clipped dismiss/CTA text; readable at iOS XL / Android ≥1.3 scale | ✓ | ✓ |

### 5.2 RescanPromptBanner

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-09 | FR-IX-05 | S7 | Focus rescan banner (first display) | **Alert**; body `tokens.rescan.required` | ✓ | ✓ |
| M2-SMOKE-10 | — | S7 | Dismiss, relaunch without clearing flag | **Polite** on repeat; dismiss control same as coverage (`a11y.coverage.dismiss`) | ✓ | ✓ |

### 5.3 ScanProgress

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-11 | A11Y-04, AC-a11y-scan-01 | S4 | Enable screen reader before scan | Progress region is **polite** live region | ✓ | ✓ |
| M2-SMOKE-12 | A11Y-05 | S4 | Listen at phase start | Hears **`Scanning, discovering files`** (`a11y.scan.discovering`) once on `discovering` entry | ✓ | ✓ |
| M2-SMOKE-13 | A11Y-04 | S4 | Listen during hashing | Progress announcement uses template `a11y.scan.progress` (percent, files, groups). Mock totals 100 files → cadence at 10% buckets, **not** every native 4 Hz tick | ✓ | ✓ |
| M2-SMOKE-14 | AC-a11y-match-05, A11Y-04 | S4 | Listen during hashing | Hears **`Analyzing video content`** (`a11y.scan.videoContent`) **once** when mock emits `contentKind=video_content` | ✓ | ✓ |
| M2-SMOKE-34 | M2-14 | S4 | View `ScanProgress` during mock video hashing step | Phase title remains **`Hashing files`**; on-screen subcopy shows **`Analyzing video content…`** (`scan.phase.videoContent`, testID `scan-progress-phase-subcopy`); subcopy absent on other hashing ticks | ✓ | ✓ |
| M2-SMOKE-15 | A11Y-05 | S4 | Listen at complete | Hears **`Scan complete`** (`a11y.scan.complete`) | ✓ | ✓ |
| M2-SMOKE-16 | A11Y-05 | S5 | Pause scan | Hears **`Scan paused`** (`a11y.scan.paused`); **Resume scan** control labeled `a11y.scan.resume` | ✓ | ✓ |
| M2-SMOKE-17 | A11Y-05 | S6 | Cancel scan | Hears **`Cancelling scan`** then **`Scan cancelled`** (terminal announcements) | ✓ | ✓ |
| M2-SMOKE-18 | A11Y-09 | S10 | S4 with reduced motion | Progress uses non-animated indicator (pulse dot); no continuous fill animation | ✓ | ✓ |
| M2-SMOKE-19 | A11Y-10 | S9 | S4 in RTL | Progress track mirrors (RTL fill direction); path strings in group detail stay LTR | ✓ | ✓ |
| M2-SMOKE-20 | A11Y-11 | S4 + S8 | Focus progress at large font | Phase title, stats, footer buttons not clipped at max scale | ✓ | ✓ |

### 5.4 Duplicate groups + keeper

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-21 | AC-a11y-match-01 | S4 | Focus list item after complete | Label matches `a11y.group.videoContent` with `{count}` → **"Same video at different quality, 2 files"** (mock group) | ✓ | ✓ |
| M2-SMOKE-22 | AC-a11y-match-01 | S4 | Compare EXACT_BYTES group | Label uses `a11y.group.exact` → **"Identical files, 2 copies"** (mock exact-bytes group) | ✓ | ✓ |
| M2-SMOKE-23 | A11Y-06, AC-a11y-keeper-01 | S11 | Traverse keeper radiogroup | Group label **`Choose file to keep`**; largest member **suggested** but **not** `selected` until explicit activation | ✓ | ✓ |
| M2-SMOKE-24 | AC-action-keeper-01 | S11 | Swipe through radio items before tap | No item announces `selected` until user activates one | ✓ | ✓ |
| M2-SMOKE-25 | FR-AC-02 | S11 | Activate preset chips | Preset buttons speak `keeper.preset.*` labels; selection updates | ✓ | ✓ |
| M2-SMOKE-26 | FR-AC-05 | S11 | Toggle remember-session checkbox (if shown) | Checkbox role; `accessibilityState.checked` toggles; label readable | ✓ | ✓ |

### 5.5 Match kind + content notice

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-27 | AC-a11y-match-03 | S11 | Inspect match kind badge | Readable label text for both variants (`tokens.match.*.label`); not color-only | ✓ | ✓ |
| M2-SMOKE-28 | AC-a11y-match-02 | S11 | Enter SAME_CONTENT_VIDEO detail | Polite **`ContentMatchNotice`** once on mount (`tokens.match.videoContent.notice`); absent on EXACT_BYTES detail | ✓ | ✓ |
| M2-SMOKE-29 | AC-a11y-match-04 | S11 | Traverse detail focus order | Notice before keeper controls; **re-verify before delete** when M3 delete lands | ✓ | ✓ |

### 5.6 Delete + paths (M3)

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-30 | A11Y-07, AC-a11y-delete-01 | — | Open delete confirm | **Blocked (M3-01)** — focus trap; heading focused; focus returns to delete trigger on dismiss | — | — |
| M2-SMOKE-31 | A11Y-08, AC-a11y-path-01 | — | Hard-link member with 2+ paths | **Blocked (M3-02)** — single label **`Same file, {n} locations`** (`a11y.path.sameFile`) | — | — |

### 5.7 Unscannable summary

| ID | Ref | Setup | Steps | Expected | iOS | Android |
|----|-----|-------|-------|----------|-----|---------|
| M2-SMOKE-32 | AC-unscan-01 | S4 | After complete, focus unscannable card | Rows for non-zero reasons only; labels from `unscan.reason.*` | ✓ | ✓ |
| M2-SMOKE-33 | AC-unscan-03 | S4 | Focus HASH_TIMEOUT row CTA | Activates retry affordance (`unscan.retry`) | ✓ | ✓ |

---

## 6. Sign-off record

| ID | Result | Platform | Tester | Date | Notes |
|----|--------|----------|--------|------|-------|
| M2-SMOKE-01 | | | | | |
| M2-SMOKE-02 | | | | | |
| M2-SMOKE-03 | | | | | |
| M2-SMOKE-04 | | | | | |
| M2-SMOKE-05 | | | | | |
| M2-SMOKE-06 | | | | | |
| M2-SMOKE-07 | | | | | |
| M2-SMOKE-08 | | | | | |
| M2-SMOKE-09 | | | | | |
| M2-SMOKE-10 | | | | | |
| M2-SMOKE-11 | | | | | |
| M2-SMOKE-12 | | | | | |
| M2-SMOKE-13 | | | | | |
| M2-SMOKE-14 | | | | | |
| M2-SMOKE-15 | | | | | |
| M2-SMOKE-16 | | | | | |
| M2-SMOKE-17 | | | | | |
| M2-SMOKE-18 | | | | | |
| M2-SMOKE-19 | | | | | |
| M2-SMOKE-20 | | | | | |
| M2-SMOKE-21 | | | | | |
| M2-SMOKE-22 | | | | | |
| M2-SMOKE-23 | | | | | |
| M2-SMOKE-24 | | | | | |
| M2-SMOKE-25 | | | | | |
| M2-SMOKE-26 | | | | | |
| M2-SMOKE-27 | | | | | |
| M2-SMOKE-28 | | | | | |
| M2-SMOKE-29 | | | | | |
| M2-SMOKE-30 | | | | | |
| M2-SMOKE-31 | | | | | |
| M2-SMOKE-32 | | | | | |
| M2-SMOKE-33 | | | | | |
| M2-SMOKE-34 | | | | | |

### Accessibility sign-off

| Role | Name | Date | Build (SHA / version) |
|------|------|------|------------------------|
| QA executed | | | |
| Accessibility approved | | | |

**M2-11 complete when:** all non-blocked rows **Pass** on at least one iOS (VoiceOver) and one Android (TalkBack) device; blocked rows documented; Accessibility row signed.

---

## 7. Relationship to M2 exit gate

| implementation-plan §4.2 bullet | Row IDs |
|----------------------------------|---------|
| Manual smoke: partial library, denied, progress cadence, cancel terminal | 01–07, 11–17, 34 (video subcopy) |
| Manual smoke: 200% font scale dismiss/CTA | 08, 20 |
| Manual smoke: delete modal focus | 30 (blocked M3) |
| Manual smoke: PathChipList multi-path | 31 (blocked M3) |
| Manual smoke: ContentMatchNotice polite on mount | 28 |
| Manual smoke: ContentMatchNotice before delete | 29 (partial until M3 delete) |
| AC-a11y-coverage-01 through AC-a11y-path-01 | Coverage + progress + keeper rows; path/delete blocked |
| AC-a11y-match-01 through AC-a11y-match-05 | 21–29 |

Re-run this matrix after M3 delete/path components land.
