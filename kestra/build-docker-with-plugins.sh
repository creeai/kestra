#!/usr/bin/env bash
# Builds the Kestra Docker image with all open-source plugins pre-installed.
# Requires: curl, jq, docker, gradle.
# Usage: ./build-docker-with-plugins.sh [image:tag]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Version from gradle.properties (strip -SNAPSHOT for API)
VERSION=$(grep '^version=' gradle.properties | sed 's/version=//' | sed 's/-SNAPSHOT//' | tr -d ' \r')
if [ -z "$VERSION" ]; then
  echo "[ERROR] Could not read version from gradle.properties"
  exit 1
fi

IMAGE_TAG="${1:-kestra/kestra:${VERSION}-with-plugins}"

echo "Building Kestra with plugins (version=${VERSION}, image=${IMAGE_TAG})"

echo "Fetching plugin list from Kestra API..."
RESPONSE=$(curl -s "https://api.kestra.io/v1/plugins/artifacts/core-compatibility/${VERSION}/latest")
if [ -z "$RESPONSE" ] || [ "$RESPONSE" = "null" ]; then
  echo "[WARN] No plugin list from API for ${VERSION}; building image without plugins. Try a released version (e.g. 0.20.0) if you need plugins."
  KESTRA_PLUGINS=""
else
  KESTRA_PLUGINS=$(echo "$RESPONSE" | jq -r '.[] | select(.license == "OPEN_SOURCE" and (.groupId != "io.kestra.plugin.ee") and (.groupId != "io.kestra.ee.secret")) | "\(.groupId):\(.artifactId):\(.version)"' | tr '\n' ' ')
  if [ -z "$KESTRA_PLUGINS" ]; then
    echo "[WARN] No open-source plugins found for ${VERSION}; building without plugins."
  else
    echo "Installing ${VERSION} plugins into image..."
  fi
fi

echo "Building executable JAR..."
./gradlew -q executableJar --no-daemon

echo "Preparing docker/app..."
mkdir -p docker/app
cp build/executable/kestra-* docker/app/kestra 2>/dev/null || cp build/executable/kestra docker/app/kestra
chmod +x docker/app/kestra

echo "Building Docker image..."
export DOCKER_BUILDKIT=1
docker build \
  --build-arg "KESTRA_PLUGINS=${KESTRA_PLUGINS}" \
  --build-arg "APT_PACKAGES=python3 python-is-python3 python3-pip curl jattach" \
  --build-arg "PYTHON_LIBRARIES=kestra" \
  -t "$IMAGE_TAG" \
  -f Dockerfile .

echo "Done. Run with: docker run -p 8080:8080 -v kestra_data:/app/storage $IMAGE_TAG server local"
echo "Image: $IMAGE_TAG"
