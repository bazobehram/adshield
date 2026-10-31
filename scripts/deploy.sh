#!/usr/bin/env bash
# Build AdShield and install it on a connected device or emulator.
#
# Usage:
#   ./scripts/deploy.sh            # first connected device
#   ./scripts/deploy.sh -d         # physical USB device
#   ./scripts/deploy.sh -e         # running emulator
#   ./scripts/deploy.sh -s SERIAL  # a specific serial
set -euo pipefail

cd "$(dirname "$0")/.."

PACKAGE="com.local.adshield"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
ADB_TARGET=("$@")

if ! command -v adb >/dev/null 2>&1; then
    echo "error: adb not found on PATH (install Android SDK platform-tools)" >&2
    exit 1
fi

if ! adb "${ADB_TARGET[@]}" get-state >/dev/null 2>&1; then
    echo "error: no authorized device found. Enable USB debugging and accept the RSA prompt." >&2
    adb devices
    exit 1
fi

echo "[1/4] Building debug APK..."
./gradlew :app:assembleDebug

echo "[2/4] Installing $APK_PATH..."
adb "${ADB_TARGET[@]}" install -r "$APK_PATH"

echo "[3/4] Granting notification permission..."
adb "${ADB_TARGET[@]}" shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

echo "[4/4] Launching AdShield..."
adb "${ADB_TARGET[@]}" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null

echo "Done. Tap the toggle on the dashboard and accept the one-time VPN prompt."
