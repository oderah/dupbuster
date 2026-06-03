# Store privacy disclosures (M4-09)

Operator-facing guides for **Play Console Data safety** and **App Store Connect App Privacy** (Privacy Nutrition labels). DupBuster v1 processes files **on-device only**; default builds do not collect or transmit user file content (see `docs/requirements.md` NFR-06, `docs/architecture.md` §8.2).

| Document | Use when |
|----------|----------|
| [`privacy-store-disclosure.md`](./privacy-store-disclosure.md) | Filling both store forms; prod promote sign-off |
| [`play-data-safety-answers.md`](./play-data-safety-answers.md) | Play Console → App content → Data safety (quick checklist) |
| [`ios-app-privacy-answers.md`](./ios-app-privacy-answers.md) | App Store Connect → App Privacy (quick checklist) |

**Related (not M4-09):** M4-14 privacy policy URL; M4-13 opt-in crash analytics UI default off.

**Automated alignment:** `npm run test:store-privacy` — `Info.plist` usage string vs `tokens.denied.blocking`; `PrivacyInfo.xcprivacy` declares no collected data types and no tracking.

**Device matrix QA (M4-10):** `tests/manual/m4-store-matrix-qa.md` — execute before prod promote; `npm run test:store-matrix` locks document contract on CI.

**Play pre-launch (M4-11):** `tests/manual/m4-play-prelaunch-report.md` — run on the **same** release AAB after M4-10; `npm run test:play-prelaunch` locks runbook + manifest contract on CI.

**TestFlight external (M4-12):** `tests/manual/m4-testflight-external-beta.md` — run on the **same** release IPA after M4-10 iOS matrix; `npm run test:testflight-external` locks runbook + iOS privacy contract on CI.
