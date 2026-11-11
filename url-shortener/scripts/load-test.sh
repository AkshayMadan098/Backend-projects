#!/usr/bin/env bash
# ==============================================================================
# URL Shortener Load Testing Script (VYSONM2A1 - Q3 & Q4)
# ==============================================================================
set -e

PORT="${PORT:-8081}"
BASE_URL="http://localhost:${PORT}"

echo "================================================================"
echo " Starting URL Shortener Load Testing Suite against ${BASE_URL} "
echo "================================================================"

# Verify server is up
echo "[1/4] Checking server availability..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/redirect?code=healthcheck" || true)
if [ "$HEALTH_CHECK" = "000" ]; then
  echo "Error: Server is not responding on ${BASE_URL}."
  echo "Please start the server first with: ./mvnw spring-boot:run"
  exit 1
fi
echo "Server is reachable (HTTP status: ${HEALTH_CHECK})."

# Ensure we have a short code to test redirection
echo "[2/4] Seeding a test URL..."
SEED_RESPONSE=$(curl -s -X POST "${BASE_URL}/shorten" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}')
echo "Response: ${SEED_RESPONSE}"

SHORT_CODE=$(echo "${SEED_RESPONSE}" | grep -o '"shortCode"[^,]*' | cut -d'"' -f4)
if [ -z "$SHORT_CODE" ]; then
  SHORT_CODE="2JrUubV"
fi
echo "Using test short code: ${SHORT_CODE}"

# Check tool availability
if command -v oha &> /dev/null; then
  TOOL="oha"
elif command -v ab &> /dev/null; then
  TOOL="ab"
else
  echo "Neither 'oha' nor 'ab' found. Please install oha via: brew install oha"
  exit 1
fi
echo "Using benchmarking tool: ${TOOL}"

echo ""
echo "================================================================"
echo "[Q3] 10 Simultaneous Requests Benchmark"
echo "================================================================"
if [ "$TOOL" = "oha" ]; then
  echo "--- Benchmark: POST /shorten (Concurrency: 10, Total: 10) ---"
  oha -n 10 -c 10 --no-tui -m POST \
    -H "Content-Type: application/json" \
    -d '{"url":"https://example.com"}' \
    "${BASE_URL}/shorten"

  echo ""
  echo "--- Benchmark: GET /redirect (Concurrency: 10, Total: 10) ---"
  oha -n 10 -c 10 --no-tui "${BASE_URL}/redirect?code=${SHORT_CODE}"
else
  echo "Running Apache Bench (ab)..."
  ab -n 10 -c 10 "${BASE_URL}/redirect?code=${SHORT_CODE}"
fi

echo ""
echo "================================================================"
echo "[Q4] Concurrency Scaling Benchmarks (50, 100, 200, 500, 1000, 2000)"
echo "================================================================"

for C in 50 100 200 500 1000 2000; do
  N=$((C * 5))
  echo ""
  echo "----------------------------------------------------------------"
  echo " Testing Concurrency: ${C} (Total Requests: ${N}) on GET /redirect"
  echo "----------------------------------------------------------------"
  if [ "$TOOL" = "oha" ]; then
    oha -n "${N}" -c "${C}" --no-tui "${BASE_URL}/redirect?code=${SHORT_CODE}"
  else
    ab -n "${N}" -c "${C}" "${BASE_URL}/redirect?code=${SHORT_CODE}"
  fi
done

echo ""
echo "================================================================"
echo " Load Testing Completed Successfully! "
echo "================================================================"
