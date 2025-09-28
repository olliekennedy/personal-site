#!/usr/bin/env bash
set -euo pipefail

# ship-it.sh (lean deploy + regional cloning)
# Build/tests expected to run in CI workflow. Locally, will build if no jar exists.

SECONDARY_REGIONS_FILE="secondary-regions.txt"

if command -v fly >/dev/null 2>&1; then FLY=fly
elif command -v flyctl >/dev/null 2>&1; then FLY=flyctl
else echo "ERROR: fly CLI not found." >&2; exit 1; fi

[ -f fly.toml ] || { echo "ERROR: fly.toml missing."; exit 1; }

APP_NAME=$(awk -F'=' '/^app *=/ {gsub(/"/,"");gsub(/ /,"");print $2}' fly.toml | head -1)
PRIMARY_REGION=$(awk -F'=' '/^primary_region *=/ {gsub(/"/,"");gsub(/ /,"");print $2}' fly.toml | head -1)
[ -n "${APP_NAME}" ] || { echo "ERROR: app name not found."; exit 1; }
[ -n "${PRIMARY_REGION}" ] || { echo "ERROR: primary_region not set."; exit 1; }

# Secondary regions
if [ -f "${SECONDARY_REGIONS_FILE}" ]; then
  SECONDARY_REGIONS=$(sed 's/#.*//' "${SECONDARY_REGIONS_FILE}" | awk '/^[a-z0-9]{3}$/' | grep -v "^${PRIMARY_REGION}$" || true)
else
  SECONDARY_REGIONS=""
fi

echo "==> App: ${APP_NAME}"
echo "==> Primary region: ${PRIMARY_REGION}"
[ -n "${SECONDARY_REGIONS}" ] && echo "==> Secondary regions: $(echo "${SECONDARY_REGIONS}" | tr '\n' ' ')" || echo "==> No secondary regions configured"

# Build only if no jar (local convenience)
if ! ls build/libs/*.jar >/dev/null 2>&1; then
  echo "==> No jar found; performing local build (tests run)"
  [ -x ./gradlew ] || { echo "ERROR: gradlew missing."; exit 1; }
  ./gradlew --quiet clean build
fi

ARTIFACT=$(find build/libs -maxdepth 1 -type f -name "*.jar" | head -1 || true)
[ -n "${ARTIFACT}" ] && echo "==> Using artifact: ${ARTIFACT}"

VERSION_LABEL="${VERSION_LABEL:-$(date -u +%Y%m%d%H%M%S)-$(git rev-parse --short HEAD 2>/dev/null || echo nogit)}"
echo "==> Version label: ${VERSION_LABEL}"

echo "==> Deploying"
${FLY} deploy --image-label "${VERSION_LABEL}" --wait-timeout 300

echo "==> Fetching machines (JSON)"
MACHINES_JSON=$(${FLY} machine list --app "${APP_NAME}" --json 2>/dev/null | jq -c '.[]')
[ -n "${MACHINES_JSON}" ] || { echo "ERROR: No machines returned."; exit 1; }

BASE_ID=$(echo "${MACHINES_JSON}" | jq -r "select(.region==\"${PRIMARY_REGION}\") | .id" | head -1)
[ -z "${BASE_ID}" ] && BASE_ID=$(echo "${MACHINES_JSON}" | jq -r '.id' | head -1)
echo "${BASE_ID}" | grep -qE '^[0-9a-f]{12,}$' || { echo "ERROR: Invalid base machine id: ${BASE_ID}"; exit 1; }
echo "==> Base machine: ${BASE_ID}"

have_region() {
  echo "${MACHINES_JSON}" | jq -r '.region' | grep -q "^$1$"
}

if [ -n "${SECONDARY_REGIONS}" ]; then
  while read -r region; do
    [ -z "${region}" ] && continue
    if have_region "${region}"; then
      echo "==> Region ${region} already present (skip)"
      continue
    fi
    echo "==> Cloning ${BASE_ID} -> ${region}"
    ${FLY} machine clone "${BASE_ID}" --region "${region}" --app "${APP_NAME}"
    MACHINES_JSON=$(${FLY} machine list --app "${APP_NAME}" --json 2>/dev/null | jq -c '.[]')
  done <<<"${SECONDARY_REGIONS}"
fi

echo "==> Final status"
${FLY} status --app "${APP_NAME}" || true
echo "==> Done"
