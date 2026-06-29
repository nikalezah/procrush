#!/usr/bin/env bash
# Build ProCrush application images and load them into the kind cluster.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
CLUSTER_NAME="${KIND_CLUSTER_NAME:-procrush}"

cd "$ROOT"

images=(
#  "procrush-api:local|deploy/Dockerfile.api"
#  "procrush-personality:local|deploy/Dockerfile.personality"
#  "procrush-matching:local|deploy/Dockerfile.matching"
  "procrush-frontend:local|deploy/Dockerfile.frontend"
)

for entry in "${images[@]}"; do
  tag="${entry%%|*}"
  dockerfile="${entry##*|}"
  echo "Building $tag ..."
  docker build -f "$dockerfile" -t "$tag" .
  echo "Loading $tag into kind cluster $CLUSTER_NAME ..."
  kind load docker-image "$tag" --name "$CLUSTER_NAME"
done

echo "All images built and loaded."
