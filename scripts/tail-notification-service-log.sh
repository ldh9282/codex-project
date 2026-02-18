#!/usr/bin/env bash
set -euo pipefail

tail -n 200 -f logs/notification-service/application.log
