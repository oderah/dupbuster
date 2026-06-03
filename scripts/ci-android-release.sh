#!/usr/bin/env bash
# M4-07: Android release bundle for tag-push CI (unsigned debug keystore until M4-08 Fastlane).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Jest + lint (release verify)"
npm run lint
npm test

echo "==> Security CI gates (Android native)"
bash scripts/run-security-ci-gates.sh

echo "==> Release AAB"
cd android
./gradlew :app:bundleRelease --no-daemon

AAB="app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "$AAB" ]]; then
  echo "Missing $AAB" >&2
  exit 1
fi
echo "Built $(realpath "$AAB")"
