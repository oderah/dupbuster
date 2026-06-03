# DupBuster Fastlane (M4-08)

Store deploy lanes per `docs/implementation-plan.md` §11.2:

| Lane | Android | iOS |
|------|---------|-----|
| `internal` | Play **internal** | **TestFlight** (internal testers) |
| `prod` | Play **production** | App Store upload (no auto-submit) |

Internal lanes do **not** require M4 prod promote gates. Prod lanes run `scripts/verify-prod-promote-gates.sh` first.

## Prerequisites

- Ruby ≥ 2.6, Bundler: `bundle install` at repo root
- Node 22.11+ for gate scripts
- **Android:** Play Console service account JSON; release keystore
- **iOS:** App Store Connect API key (`.p8`); distribution cert + App Store provisioning profile (Xcode automatic signing or CI secrets)

## Commands

```bash
export DUPBUSTER_RELEASE_VERSION=v1.0.0
export DUPBUSTER_VERSION_CODE=100   # optional; integer Play/App Store build number

# Internal (Play internal + TestFlight)
bundle exec fastlane android internal
bundle exec fastlane ios internal

# Production (after §6.2 sign-off)
export DUPBUSTER_PROD_PROMOTE_APPROVED=1
bundle exec fastlane android prod
bundle exec fastlane ios prod
```

Wrappers: `scripts/run-fastlane-lane.sh internal|prod android|ios`

## Environment variables

### Android signing

| Variable | Description |
|----------|-------------|
| `ANDROID_KEYSTORE_FILE` | Path to `.keystore` / `.jks` |
| `ANDROID_KEYSTORE_BASE64` | Alternative: base64 keystore (CI writes temp file) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |

### Play Console

| Variable | Description |
|----------|-------------|
| `PLAY_STORE_JSON_KEY` | Service account JSON **content** (CI secret) |
| `PLAY_STORE_JSON_KEY_PATH` | Or path to JSON file (local) |

### App Store Connect

| Variable | Description |
|----------|-------------|
| `APP_STORE_CONNECT_API_KEY_ID` | Key ID |
| `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID |
| `APP_STORE_CONNECT_API_KEY_CONTENT` | `.p8` contents, base64-encoded |
| `FASTLANE_APPLE_ID` | Optional Apple ID email |
| `FASTLANE_TEAM_ID` | Developer Portal team ID |
| `FASTLANE_ITC_TEAM_ID` | App Store Connect team ID |

### Prod promote

| Variable | Description |
|----------|-------------|
| `DUPBUSTER_PROD_PROMOTE_APPROVED` | Must be `1` after manual §6.2 checklist |
| `DUPBUSTER_SKIP_PROD_GATES` | `1` only for local lane debugging (not CI) |

Before **prod** submit, complete Play Data safety and App Store App Privacy using `docs/store/` (M4-09). `npm run test:store-privacy` must be green on the release tag.

## CI

`.github/workflows/fastlane-deploy.yml` — `workflow_dispatch` with `lane` (`internal` / `prod`) and `platform` (`android` / `ios` / `both`). Configure repository secrets before first run.

Release train: tag push → **Release matrix build** (M4-07) → dispatch Fastlane **internal** → after gates green, dispatch **prod**.
