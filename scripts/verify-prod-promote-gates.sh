#!/usr/bin/env bash
# M4-08 / implementation-plan.md §6.2 — automated checks before Fastlane prod lane.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "${DUPBUSTER_SKIP_PROD_GATES:-}" == "1" ]]; then
  echo "WARN: DUPBUSTER_SKIP_PROD_GATES=1 — prod gates skipped (local debug only)" >&2
  exit 0
fi

echo "==> Security CI gates (AC-security-redact-01, AC-security-uri-01)"
bash scripts/run-security-ci-gates.sh

echo "==> Automated a11y gate (M2-10)"
npm run test:a11y

echo "==> Store privacy alignment (M4-09)"
npm run test:store-privacy

echo "==> Store matrix QA contract (M4-10)"
npm run test:store-matrix

echo "==> Play pre-launch report contract (M4-11)"
npm run test:play-prelaunch

echo "==> TestFlight external beta contract (M4-12)"
npm run test:testflight-external

echo "==> Android equivalence + video/security fixtures (M1-18, decode, blob, progress)"
cd android
./gradlew :app:testDebugUnitTest \
  --tests 'com.dupbuster.scanengine.fixtures.EquivFixturesTest' \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityCiGateTest' \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityUriCiGateTest'

echo "==> Manual §6.2 gates (confirm before prod promote)"
echo "  - Store-matrix QA signed off: tests/manual/m4-store-matrix-qa.md (M4-10)"
echo "  - FGS cancel notification cleared ≤ 120 s (AC-integrity-cancel-01)"
echo "  - Play pre-launch signed off: tests/manual/m4-play-prelaunch-report.md (M4-11, after M4-10)"
echo "  - TestFlight external signed off: tests/manual/m4-testflight-external-beta.md (M4-12, after M4-10 iOS)"
echo "  - Manual VoiceOver/TalkBack smoke signed off (M2-11)"

if [[ "${DUPBUSTER_PROD_PROMOTE_APPROVED:-}" != "1" ]]; then
  echo "Set DUPBUSTER_PROD_PROMOTE_APPROVED=1 after manual sign-off." >&2
  exit 1
fi

echo "Prod promote automated gates passed."
