#!/usr/bin/env bash
# Build and install the WSL adb proxy into the Linux Android SDK.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
PT="$SDK/platform-tools"

if [[ ! -d "$PT" ]]; then
  echo "platform-tools not found at $PT — install the Linux SDK first." >&2
  exit 1
fi

gcc -O2 -o "$ROOT/scripts/adb-proxy" "$ROOT/scripts/adb-proxy.c"

if [[ -f "$PT/adb" && ! -f "$PT/adb.linux" ]]; then
  if file "$PT/adb" | grep -q ELF; then
    cp "$PT/adb" "$PT/adb.linux"
  fi
fi

cp "$ROOT/scripts/adb-proxy" "$PT/adb"
chmod +x "$PT/adb"

echo "Installed adb proxy to $PT/adb"
"$PT/adb" devices
