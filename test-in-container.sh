#!/usr/bin/env bash
# Run unit tests inside the Docker container (no device needed).
set -euo pipefail

IMAGE_NAME="immich-tv-builder"

echo "==> Building image (if not cached)..."
docker build -t "$IMAGE_NAME" .

echo "==> Running unit tests..."
docker run --rm "$IMAGE_NAME" ./gradlew test --no-daemon

echo "✓ All unit tests passed."
