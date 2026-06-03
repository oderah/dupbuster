#!/usr/bin/env bash
# M4-05 / M4-06: fixture-backed security CI gates (JS manifest + Android native).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> Jest security CI gate descriptors"
npm test -- --testPathPattern='securityCiGate'

echo "==> Android native security CI gates"
cd android
./gradlew :app:testDebugUnitTest \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityCiGateTest' \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityUriCiGateTest' \
  --tests 'com.dupbuster.scanengine.security.RedactionFilterTest' \
  --tests 'com.dupbuster.scanengine.security.SafUriRulesTest'
