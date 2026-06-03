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

echo "==> Android equivalence + video/security fixtures (M1-18, decode, blob, progress)"
cd android
./gradlew :app:testDebugUnitTest \
  --tests 'com.dupbuster.scanengine.fixtures.EquivFixturesTest' \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityCiGateTest' \
  --tests 'com.dupbuster.scanengine.fixtures.SecurityUriCiGateTest'

echo "==> Manual §6.2 gates (confirm before prod promote)"
echo "  - FGS cancel notification cleared ≤ 120 s (AC-integrity-cancel-01)"
echo "  - Play pre-launch report clean API 26/33/34"
echo "  - TestFlight external beta complete"
echo "  - Manual VoiceOver/TalkBack smoke signed off"

if [[ "${DUPBUSTER_PROD_PROMOTE_APPROVED:-}" != "1" ]]; then
  echo "Set DUPBUSTER_PROD_PROMOTE_APPROVED=1 after manual sign-off." >&2
  exit 1
fi

echo "Prod promote automated gates passed."
