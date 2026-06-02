#!/usr/bin/env bash
# Re-apply Metro bridge after emulator restart (WSL → Windows emulator).
# adb reverse + debug_http_host=localhost:8081 (10.0.2.2 alone often hangs when Metro runs in WSL).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
PORT="${RCT_METRO_PORT:-8081}"
DEVICE="${ANDROID_SERIAL:-emulator-5554}"
METRO_URL="http://127.0.0.1:${PORT}"
PREFS_TEMPLATE="$ROOT/scripts/com.dupbuster_preferences.xml"

if [[ ! -x "$ADB" ]]; then
  echo "Run: bash scripts/setup-wsl-adb.sh" >&2
  exit 1
fi

if [[ ! -f "$PREFS_TEMPLATE" ]]; then
  echo "Missing $PREFS_TEMPLATE" >&2
  exit 1
fi

if ! "$ADB" devices | grep -q "${DEVICE}[[:space:]]*device"; then
  echo "Device $DEVICE not found. Start the emulator, then: $ADB devices" >&2
  exit 1
fi

if ! curl -sf --connect-timeout 3 "${METRO_URL}/status" | grep -q 'packager-status:running'; then
  echo "Metro is not reachable at ${METRO_URL}." >&2
  echo "Start it in another terminal: cd ${ROOT} && npm start" >&2
  exit 1
fi

"$ADB" -s "$DEVICE" reverse "tcp:${PORT}" "tcp:${PORT}"
"$ADB" -s "$DEVICE" push "$PREFS_TEMPLATE" /data/local/tmp/dupbuster_rn_prefs.xml >/dev/null
"$ADB" -s "$DEVICE" shell run-as com.dupbuster cp /data/local/tmp/dupbuster_rn_prefs.xml \
  shared_prefs/com.dupbuster_preferences.xml

echo "Metro bridge: ${DEVICE} reverse tcp:${PORT} + debug_http_host=localhost:${PORT}"
echo "Metro OK at ${METRO_URL}"
echo "Reload the app: npm run android:reload  (or press R twice in the emulator)"
