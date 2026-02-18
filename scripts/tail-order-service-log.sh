#!/usr/bin/env bash
set -euo pipefail

tail -n 200 -f logs/order-service/application.log
