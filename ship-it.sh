#!/usr/bin/env bash
set -euo pipefail

# ship-it.sh (tests always run)
# - Primary region from fly.toml
# - Secondary regions from secondary-regions.txt (one region code per line, optional \# comments)

SECONDARY_REGIONS_FILE="secondary-regions.txt"

# Locate fly CLI
if command -v fly >/dev/null 2>&1; then FLY=fly
elif command -v flyctl >/dev/null 2>&1; then FLY=flyctl
else echo "ERROR: fly CLI not found." >&2; exit 1; fi

[ -x ./gradlew ] || { echo "ERROR: gradlew missing or not executable."; exit 1; }
[ -f fly.toml ] || { echo "ERROR: fly.toml missing."; exit 1; }

APP_NAME=$(awk -F'=' '/^app *=/ {gsub(/"/,"");gsub(/ /,"");print $2}' fly.toml | head -1)
[ -n "${APP_NAME}" ] || { echo "ERROR: app name not found in fly.toml"; exit 1; }

PRIMARY_REGION=$(awk -F'=' '/^primary_region *=/ {gsub(/"/,"");gsub(/ /,"");print $2}' fly.toml | head -1)
[ -n "${PRIMARY_REGION}" ] || { echo "ERROR: primary_region not set in fly.toml"; exit 1; }

# Load secondary regions (exclude primary)
if [ -f "${SECONDARY_REGIONS_FILE}" ]; then
  SECONDARY_REGIONS=$(sed 's/#.*//' "${SECONDARY_REGIONS_FILE}" | awk '/^[a-z0-9]{3}$/' | grep -v "^${PRIMARY_REGION}$" || true)
else
  SECONDARY_REGIONS=""
fi

echo "==> App: ${APP_NAME}"
echo "==> Primary region: ${PRIMARY_REGION}"
[ -n "${SECONDARY_REGIONS}" ] && echo "==> Secondary regions: $(echo "${SECONDARY_REGIONS}" | tr '\n' ' ')" || echo "==> No secondary regions configured"

echo "==> Building (tests enforced)"
./gradlew --quiet clean build

ARTIFACT=$(find build/libs -maxdepth 1 -type f -name "*.jar" | head -1 || true)
[ -n "${ARTIFACT}" ] && echo "==> Artifact: ${ARTIFACT}"

VERSION_LABEL="$(date -u +%Y%m%d%H%M%S)-$(git rev-parse --short HEAD 2>/dev/null || echo nogit)"

echo "==> Deploying (uses primary_region from fly.toml)"
${FLY} deploy --image-label "${VERSION_LABEL}" --wait-timeout 300

# Retrieve machines (id region)
get_machines() {
  if command -v jq >/dev/null 2>&1; then
    ${FLY} machine list --app "${APP_NAME}" --json 2>/dev/null \
      | jq -r '.[] | "\(.id) \(.region)"' | grep -E '^[0-9a-f]+ [a-z0-9]{3}$' || true
  else
    ${FLY} machine list --app "${APP_NAME}" 2>/dev/null | awk '
      {
        for(i=1;i<=NF;i++){
          if($i ~ /^[0-9a-f]+$/ && length($i)>=12){id=$i}
          if($i ~ /^[a-z0-9]{3}$/){region=$i}
        }
        if(id && region){print id,region; id=""; region=""}
      }' | sort -u || true
  fi
}

echo "==> Fetching machines"
MACHINES="$(get_machines)"
[ -n "${MACHINES}" ] || { echo "ERROR: No machines parsed after deploy."; exit 1; }

# Select base (prefer primary region)
BASE_ID=$(echo "${MACHINES}" | awk -v pr="${PRIMARY_REGION}" '$2==pr {print $1; exit}')
[ -z "${BASE_ID}" ] && BASE_ID=$(echo "${MACHINES}" | head -1 | awk '{print $1}')
echo "${BASE_ID}" | grep -qE '^[0-9a-f]{12,}$' || { echo "ERROR: Invalid base machine id: ${BASE_ID}"; exit 1; }
echo "==> Base machine: ${BASE_ID}"

region_exists() {
  echo "${MACHINES}" | awk '{print $2}' | grep -q "^$1$"
}

# Clone for missing secondary regions
if [ -n "${SECONDARY_REGIONS}" ]; then
  while read -r region; do
    [ -z "${region}" ] && continue
    if region_exists "${region}"; then
      echo "==> Region ${region} already present (skip)"
      continue
    fi
    echo "==> Cloning ${BASE_ID} -> ${region}"
    ${FLY} machine clone "${BASE_ID}" --region "${region}" --app "${APP_NAME}"
    MACHINES="$(get_machines)"
  done <<<"${SECONDARY_REGIONS}"
fi

echo "==> Final status"
${FLY} status --app "${APP_NAME}" || true

echo "==> Done. Version label: ${VERSION_LABEL}"
