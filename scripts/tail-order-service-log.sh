#!/usr/bin/env bash
set -euo pipefail

LOG_FILE="logs/order-service/app.log"
[ -f "$LOG_FILE" ] || { echo "log file not found: $LOG_FILE"; exit 1; }
tail -n 200 -f "$LOG_FILE"
