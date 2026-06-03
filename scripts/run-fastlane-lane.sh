#!/usr/bin/env bash
# M4-08: Run Fastlane internal or prod lane for one or both platforms.
set -euo pipefail

usage() {
  echo "Usage: $0 <internal|prod> <android|ios|both> [release-version]" >&2
  echo "  release-version defaults to DUPBUSTER_RELEASE_VERSION or latest v* git tag" >&2
  exit 1
}

LANE="${1:-}"
PLATFORM="${2:-}"
VERSION_ARG="${3:-}"

[[ "$LANE" == "internal" || "$LANE" == "prod" ]] || usage
[[ "$PLATFORM" == "android" || "$PLATFORM" == "ios" || "$PLATFORM" == "both" ]] || usage

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -n "$VERSION_ARG" ]]; then
  export DUPBUSTER_RELEASE_VERSION="$VERSION_ARG"
elif [[ -z "${DUPBUSTER_RELEASE_VERSION:-}" ]]; then
  TAG="$(git describe --tags --match 'v*' --abbrev=0 2>/dev/null || true)"
  if [[ -n "$TAG" ]]; then
    export DUPBUSTER_RELEASE_VERSION="$TAG"
  fi
fi

if [[ -z "${DUPBUSTER_RELEASE_VERSION:-}" ]]; then
  echo "Set DUPBUSTER_RELEASE_VERSION or pass release-version (e.g. v1.0.0)" >&2
  exit 1
fi

run_platform() {
  local platform="$1"
  echo "==> fastlane $platform $LANE ($DUPBUSTER_RELEASE_VERSION)"
  bundle exec fastlane "$platform" "$LANE"
}

case "$PLATFORM" in
  android) run_platform android ;;
  ios) run_platform ios ;;
  both)
    run_platform android
    run_platform ios
    ;;
esac
