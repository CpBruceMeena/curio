#!/bin/bash
# Curio Daily Content Scraper Cron Script
# Add to crontab: 0 6 * * * /path/to/curio/scripts/cron.sh >> /var/log/curio_scraper.log 2>&1

set -euo pipefail

cd "$(dirname "$0")"

# Load env vars if .env exists
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

if [ -z "${DATABASE_URL:-}" ]; then
    echo "[$(date)] ERROR: DATABASE_URL not set"
    exit 1
fi

echo "[$(date)] Starting daily scrape..."
echo "[$(date)] Target: 200 new items"
python3 -m scraper --batch 200
echo "[$(date)] Scrape complete"
