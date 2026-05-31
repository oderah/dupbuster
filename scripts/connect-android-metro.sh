#!/usr/bin/env bash
# Re-apply adb reverse after emulator restart (WSL → Windows emulator Metro bridge).
set -euo pipefail

SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
PORT="${RCT_METRO_PORT:-8081}"
DEVICE="${ANDROID_SERIAL:-emulator-5554}"

if [[ ! -x "$ADB" ]]; then
  echo "Run: bash scripts/setup-wsl-adb.sh" >&2
  exit 1
fi

if ! "$ADB" devices | grep -q "${DEVICE}[[:space:]]*device"; then
  echo "Device $DEVICE not found. Start the emulator, then: $ADB devices" >&2
  exit 1
fi

"$ADB" -s "$DEVICE" reverse "tcp:${PORT}" "tcp:${PORT}"
echo "Metro bridge: $DEVICE tcp:${PORT} → WSL tcp:${PORT}"
echo "Ensure Metro is running: npm start"
