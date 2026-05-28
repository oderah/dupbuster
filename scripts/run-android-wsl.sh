#!/usr/bin/env bash
# WSL workflow: Gradle assembleDebug (no DDMLib device check) + Windows-emulator adb install.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
DEVICE="${ANDROID_SERIAL:-emulator-5554}"
PORT="${RCT_METRO_PORT:-8081}"

if [[ ! -x "$ADB" ]]; then
  echo "Run: bash scripts/setup-wsl-adb.sh" >&2
  exit 1
fi

if ! "$ADB" devices | grep -q "${DEVICE}[[:space:]]*device"; then
  echo "Device $DEVICE not found. Start the Windows emulator, then: $ADB devices" >&2
  exit 1
fi

cd "$ROOT/android"
./gradlew assembleDebug

"$ADB" -s "$DEVICE" install -r "$APK"
"$ADB" -s "$DEVICE" reverse "tcp:${PORT}" "tcp:${PORT}"
"$ADB" -s "$DEVICE" shell am start -n "com.dupbuster/.MainActivity"

echo "Installed on $DEVICE. Ensure Metro is running: npm start"
