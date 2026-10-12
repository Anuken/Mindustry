#!/usr/bin/env bash
# Start/stop the interactive client in the background.
#   tests/driver/client-driver.sh start   # boots (30-120s), prints READY when commands are accepted
#   tests/driver/client-driver.sh stop
#   tests/driver/client-driver.sh cmd 'click 100 100'
set -euo pipefail
PORT="${CLIENTDRIVER_PORT:-8765}"
LOG="${CLIENTDRIVER_LOG:-/tmp/mindustry-client-driver.log}"

#repository root, wherever the script is called from
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
case "${1:-}" in
  start)
    if curl -fs "localhost:$PORT" -d ping >/dev/null 2>&1; then echo "already running"; exit 0; fi
    nohup ${GRADLE:-./gradlew} :tests:clientDriver -Pclientdriver.port="$PORT" --console=plain >"$LOG" 2>&1 &
    for _ in $(seq 1 300); do
      if grep -q CLIENT_DRIVER_READY "$LOG" 2>/dev/null; then echo "READY (log: $LOG)"; exit 0; fi
      if ! pgrep -f ":tests:clientDriver" >/dev/null; then echo "driver exited, see $LOG"; tail -40 "$LOG"; exit 1; fi
      sleep 1
    done
    echo "timed out waiting for the client, see $LOG"; exit 1 ;;
  stop)  curl -s "localhost:$PORT" -d quit || true ;;
  cmd)   shift; curl -s "localhost:$PORT" -d "$*" ;;
  *)     echo "usage: $0 start|stop|cmd <command>"; exit 2 ;;
esac
