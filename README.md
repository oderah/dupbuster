# DupBuster

On-device duplicate finder for Android and iOS (React Native bare workflow).

## Prerequisites

- Node.js ≥ 22.11 (see `package.json` `engines`)
- JDK 17+ (21 recommended)
- Android SDK with API 26+ emulator or device
- macOS + Xcode 15+ for iOS builds (Pods: `bundle install` then `cd ios && bundle exec pod install`)

## Quick start

```bash
npm install
npm start
```

In separate terminals:

```bash
npm run android   # API 26+ device/emulator
npm run ios       # iOS 15+ simulator or device (macOS)
```

## Project layout

| Path | Purpose |
|------|---------|
| `android/` | Android app + future `scanengine` native module |
| `ios/` | iOS app + `ScanEngine/` native module |
| `src/` | RN UI, controllers, tokens (M2+) |
| `tests/fixtures/dupbuster/v1/` | Hash/security fixture matrix (M1) |
| `docs/` | Requirements, architecture, implementation plan |

Authority and milestone gates: see [docs/README.md](docs/README.md).
