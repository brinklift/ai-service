#!/bin/bash
# ─────────────────────────────────────────────
#  Restart script — Blift AI Service
#  Profile: dev  |  Port: dynamic (Eureka)
#  Eureka ID: AI-SERVICE
#  Usage: bash restart-ai-service.sh [--tail]
# ─────────────────────────────────────────────

SERVICE_NAME="ai-service"
SERVICE_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="/Volumes/Development/blift/local/logs"
LOG_FILE="${LOG_DIR}/${SERVICE_NAME}.log"
mkdir -p "$LOG_DIR"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; RESET='\033[0m'

echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${CYAN} Restarting: ${SERVICE_NAME}${RESET}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

# ── Kill existing process (mvnw parent + forked Spring Boot JVM) ───────────────
kill_service() {
  local PIDS
  PIDS=$(pgrep -f "${SERVICE_DIR}.*mvnw" 2>/dev/null || true)
  PIDS+=" $(pgrep -f "AiServiceApplication" 2>/dev/null || true)"
  PIDS=$(echo "$PIDS" | tr ' ' '\n' | sort -u | grep -v '^$' | tr '\n' ' ')
  if [ -n "$(echo "$PIDS" | tr -d ' ')" ]; then
    echo -e "${YELLOW}→ Stopping process(es): $PIDS${RESET}"
    echo "$PIDS" | xargs kill 2>/dev/null || true
    sleep 3
    PIDS=$(pgrep -f "${SERVICE_DIR}.*mvnw" 2>/dev/null || true)
    PIDS+=" $(pgrep -f "AiServiceApplication" 2>/dev/null || true)"
    PIDS=$(echo "$PIDS" | tr ' ' '\n' | sort -u | grep -v '^$' | tr '\n' ' ')
    if [ -n "$(echo "$PIDS" | tr -d ' ')" ]; then
      echo -e "${YELLOW}  Force-killing: $PIDS${RESET}"
      echo "$PIDS" | xargs kill -9 2>/dev/null || true
      sleep 1
    fi
    echo -e "${GREEN}  ✓ Old process stopped${RESET}"
  else
    echo "→ No existing process found for ${SERVICE_NAME}"
  fi
}
kill_service

# ── Load .env if present ───────────────────────────────────────────────────────
if [ -f "${SERVICE_DIR}/.env" ]; then
  set -o allexport
  # shellcheck disable=SC1091
  source "${SERVICE_DIR}/.env"
  set +o allexport
  echo "→ Loaded environment from .env"
fi

# ── Start service ──────────────────────────────────────────────────────────────
echo ""
echo "→ Building and starting ${SERVICE_NAME} (profile: dev)..."
cd "$SERVICE_DIR"
nohup ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -DskipTests > "$LOG_FILE" 2>&1 &
NEW_PID=$!
disown $NEW_PID
echo -e "${GREEN}  ✓ Started — PID: ${NEW_PID}${RESET}"
echo -e "  Log: ${LOG_FILE}"
echo ""
echo -e "  ${CYAN}Registered as AI-SERVICE in Eureka → http://localhost:8020${RESET}"
echo ""

# ── Optionally tail the log ────────────────────────────────────────────────────
if [[ "$1" == "--tail" || "$1" == "-t" ]]; then
  echo "Tailing log (Ctrl+C to stop)..."
  tail -f "$LOG_FILE"
fi

