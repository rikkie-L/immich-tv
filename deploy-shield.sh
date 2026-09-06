#!/usr/bin/env bash
# Build the Immich TV APK in a container and deploy it to a Shield TV / Android TV via ADB.
#
# Usage:
#   ./deploy-shield.sh <SHIELD_IP> [ADB_PORT]
#
# Example:
#   ./deploy-shield.sh 192.168.1.50          # connects to 192.168.1.50:5555
#   ./deploy-shield.sh 192.168.1.50 5555
#
# Prerequisites on the host:
#   - Docker
#   - adb (Android Platform Tools)
#
# One-time setup on the TV:
#   Settings -> Device Preferences -> About -> click "Build" 7x
#   -> Developer options -> enable "USB debugging" AND "ADB over network"
#   -> confirm the authorization prompt on the TV the first time you connect.
#   (Without "ADB over network" the TV does not listen on tcp:5555; connect once
#    over USB and run `adb tcpip 5555` to bootstrap it.)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHIELD_IP="${1:-}"
ADB_PORT="${2:-5555}"
APK_HOST_DIR="$SCRIPT_DIR/output"
APK_NAME="app-debug.apk"
IMAGE_NAME="immich-tv-builder"
PKG="com.immichtv.app"

if [[ -z "$SHIELD_IP" ]]; then
    echo "Usage: $0 <SHIELD_IP> [ADB_PORT]"
    echo "  Example: $0 192.168.1.50"
    exit 1
fi

TARGET="$SHIELD_IP:$ADB_PORT"

# --- Preflight -------------------------------------------------------------
for cmd in docker adb; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "ERROR: '$cmd' not found on PATH. Install it first." >&2
        exit 1
    fi
done

# --- Build ---------------------------------------------------------------
echo "==> Building APK in Docker container..."
docker build -t "$IMAGE_NAME" "$SCRIPT_DIR"

echo "==> Extracting APK from container..."
mkdir -p "$APK_HOST_DIR"
docker run --rm \
    -v "$APK_HOST_DIR:/output" \
    "$IMAGE_NAME" \
    sh -c "cp /project/app/build/outputs/apk/debug/$APK_NAME /output/$APK_NAME"

echo "==> APK written to: $APK_HOST_DIR/$APK_NAME"

# --- Connect ------------------------------------------------------------
echo "==> Connecting to $TARGET ..."
adb connect "$TARGET" >/dev/null || true

state=""
for _ in 1 2 3 4 5; do
    state="$(adb -s "$TARGET" get-state 2>/dev/null || true)"
    [[ "$state" == "device" ]] && break
    sleep 1
done

if [[ "$state" != "device" ]]; then
    echo "ERROR: no usable adb connection to $TARGET (state: ${state:-unreachable})." >&2
    echo "  On the TV: Settings -> Device Preferences -> About -> click 'Build' 7x," >&2
    echo "  then Developer options -> enable 'USB debugging' and 'ADB over network'." >&2
    echo "  Also confirm the authorization prompt shown on the TV." >&2
    exit 1
fi

# --- Install & launch --------------------------------------------------
echo "==> Installing APK on $TARGET ..."
adb -s "$TARGET" install -r "$APK_HOST_DIR/$APK_NAME"

echo "==> Launching app..."
adb -s "$TARGET" shell am start -n "$PKG/.MainActivity" >/dev/null

echo ""
echo "OK - 'Immich TV' should now be running on the TV."
echo ""
echo "  Useful ADB commands:"
echo "    adb -s $TARGET logcat --pid=\$(adb -s $TARGET shell pidof -s $PKG)   # all app output"
echo "    adb -s $TARGET logcat -s OkHttp:V AndroidRuntime:E                   # HTTP calls + crashes"
echo "    adb -s $TARGET shell pm clear $PKG                                   # wipe config, back to setup screen"
echo "    adb -s $TARGET shell am force-stop $PKG                              # stop the app"
echo "    adb -s $TARGET uninstall $PKG                                        # uninstall"
echo ""
echo "  Note: HTTP log lines only appear once the app is connected and you open"
echo "  the Photos tab; the setup/login requests are not logged."
