# Play Console — Data safety answers (M4-09)

**Console path:** Play Console → *Your app* → **App content** → **Data safety**.

Complete on the **production candidate** build. Answers assume v1 **on-device-only** behavior and **opt-in crash analytics default off** (M4-13 not shipping collection in default prod).

---

## Overview

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | **N/A** (no collection) |
| Do you provide a way for users to request data deletion? | **N/A** or “Data not collected” — no account/backend |
| Independent security review | Per Play policy when applicable |

---

## Data types (required declarations)

For each type below, use **Not collected** / **Not shared** unless a future release enables opt-in telemetry (then re-file per `privacy-store-disclosure.md` §6).

| Data type | Collected? | Shared? | Notes |
|-----------|------------|---------|-------|
| Location | No | No | No location APIs in manifest |
| Personal info (name, email, IDs) | No | No | No accounts in v1 |
| Financial info | No | No | |
| Health and fitness | No | No | |
| Messages | No | No | |
| Photos and videos | **No** | **No** | Read on-device for hashing; not transmitted |
| Audio files | **No** | **No** | `READ_MEDIA_AUDIO` for local scan only |
| Files and docs | **No** | **No** | SAF / MediaStore read on-device |
| Calendar | No | No | |
| Contacts | No | No | |
| App activity (in-app actions) | No* | No | *If M4-13 ships opt-in analytics, declare minimal diagnostic data only — not file paths or unscannable codes |
| Web browsing | No | No | |
| App info and performance (crash logs) | No** | No | **Default off; no SDK egress in default prod |

**Ephemeral / on-device processing:** Duplicate detection is processing only; Play “collected” means transmitted off the device. Document in the Data safety **store listing / details** field if offered:

> DupBuster scans and compares files on your device. Your photos, videos, and documents are not uploaded to our servers.

---

## Security practices

| Practice | Answer |
|----------|--------|
| Data encrypted in transit | N/A (no off-device user file data) |
| Users can request deletion | No server-held user files; local catalog cleared on uninstall |
| Committed to Play Families Policy | Per target audience |

---

## Permissions alignment (pre-launch)

Declared permissions in `android/app/src/main/AndroidManifest.xml` must match this form:

| Permission | Store justification |
|------------|---------------------|
| `READ_MEDIA_*` / `READ_EXTERNAL_STORAGE` | Find duplicates in media the user granted |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Ongoing user-initiated scan notification |
| `INTERNET` | React Native runtime; **not** used to upload user files in v1 |

**FGS copy:** Channel `com.dupbuster.scan.foreground.v1`; title/body from `tokens.notification.*` — no “delete” or “remove” in ongoing notification (project-rules §6 Android).

---

## Sign-off

| Role | Date | Build version |
|------|------|---------------|
| Release owner | | |
| Privacy review | | |

After sign-off, check **implementation-plan.md** §12 Play Data safety bullet.
