#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CONNECTOR_FILE="${ROOT}/platform/kafka/connectors/finledger-outbox.json"

echo "Waiting for Connect at ${CONNECT_URL}..."
for i in $(seq 1 60); do
  if curl -fsS "${CONNECT_URL}/" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "Registering finledger-outbox connector (POST)..."
HTTP_CODE=$(curl -sS -o /tmp/payhub-connector-resp.json -w "%{http_code}" \
  -X POST -H 'Content-Type: application/json' \
  --data @"${CONNECTOR_FILE}" \
  "${CONNECT_URL}/connectors" || true)

if [[ "${HTTP_CODE}" == "409" ]]; then
  echo "Connector exists; updating config..."
  # PUT expects the config object only
  python3 -c "import json,sys; print(json.dumps(json.load(open(sys.argv[1]))['config']))" \
    "${CONNECTOR_FILE}" > /tmp/payhub-connector-config.json
  curl -fsS -X PUT -H 'Content-Type: application/json' \
    --data @/tmp/payhub-connector-config.json \
    "${CONNECT_URL}/connectors/finledger-outbox/config"
  echo
elif [[ "${HTTP_CODE}" != "200" && "${HTTP_CODE}" != "201" ]]; then
  echo "Failed to register connector HTTP ${HTTP_CODE}:" >&2
  cat /tmp/payhub-connector-resp.json >&2 || true
  exit 1
fi

echo "Connector status:"
curl -fsS "${CONNECT_URL}/connectors/finledger-outbox/status"
echo
