#!/usr/bin/env bash
# Build the Immich TV APK in a container and deploy it to a Shield TV Pro via ADB.
#
# Usage:
#   ./deploy-shield.sh <SHIELD_IP>
#
# Prerequisites on the host:
#   - Docker
#   - adb (Android Platform Tools)
#
# On the Shield TV Pro (one-time setup):
#   Settings → Device Preferences → Developer options → USB debugging ON
#   (For network ADB, also enable "ADB over network" if available, or just plug in USB first)

set -euo pipefail

SHIELD_IP="${1:-}"
APK_HOST_DIR="$(pwd)/output"
APK_NAME="app-debug.apk"
IMAGE_NAME="immich-tv-builder"

if [[ -z "$SHIELD_IP" ]]; then
    echo "Usage: $0 <SHIELD_IP>"
    echo "  Example: $0 192.168.1.50"
    exit 1
fi

echo "==> Building APK in Docker container..."
docker build -t "$IMAGE_NAME" .

echo "==> Extracting APK from container..."
mkdir -p "$APK_HOST_DIR"
docker run --rm \
    -v "$APK_HOST_DIR:/output" \
    "$IMAGE_NAME" \
    sh -c "cp /project/app/build/outputs/apk/debug/$APK_NAME /output/$APK_NAME"

echo "==> APK written to: $APK_HOST_DIR/$APK_NAME"

echo "==> Connecting to Shield TV Pro at $SHIELD_IP:5555 ..."
adb connect "$SHIELD_IP:5555"

# Wait briefly for the connection
sleep 2

echo "==> Installing APK on Shield TV Pro..."
adb -s "$SHIELD_IP:5555" install -r "$APK_HOST_DIR/$APK_NAME"

echo ""
echo "✓ Done! Launch 'Immich TV' from the Shield TV home screen."
echo ""
echo "  Useful ADB commands:"
echo "    adb -s $SHIELD_IP:5555 logcat -s ImmichTV    # view logs"
echo "    adb -s $SHIELD_IP:5555 shell am force-stop com.immichtv.app  # stop app"
echo "    adb -s $SHIELD_IP:5555 uninstall com.immichtv.app            # uninstall"
