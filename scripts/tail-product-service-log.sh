#!/usr/bin/env bash
set -euo pipefail

tail -n 200 -f logs/product-service/application.log
