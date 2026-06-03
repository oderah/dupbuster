#!/usr/bin/env bash
# M4-07: iOS Release compile + unsigned archive for tag-push CI (signing in M4-08 Fastlane).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "iOS release build requires macOS (Xcode)." >&2
  exit 1
fi

echo "==> CocoaPods"
cd ios
bundle install --quiet
bundle exec pod install

WORKSPACE="Dupbuster.xcworkspace"
SCHEME="Dupbuster"
ARCHIVE_PATH="${RUNNER_TEMP:-$ROOT/build/ci}/Dupbuster.xcarchive"
mkdir -p "$(dirname "$ARCHIVE_PATH")"

echo "==> DupbusterScanEngineTests (simulator)"
xcodebuild test \
  -workspace "$WORKSPACE" \
  -scheme "$SCHEME" \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  -only-testing:DupbusterScanEngineTests \
  CODE_SIGNING_ALLOWED=NO

echo "==> Release archive (unsigned)"
xcodebuild archive \
  -workspace "$WORKSPACE" \
  -scheme "$SCHEME" \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=''

if [[ ! -d "$ARCHIVE_PATH" ]]; then
  echo "Missing archive at $ARCHIVE_PATH" >&2
  exit 1
fi
echo "Archived $ARCHIVE_PATH"
