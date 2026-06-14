#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

# Load environment variables from .env (if present)
if [ -f .env ]; then
    echo "→ Loading .env"
    set -a; source .env; set +a
fi

echo "→ Building curio-server..."
go build -o curio-server .

echo "→ Starting curio-server on port ${PORT:-8080}..."
echo "   (Ctrl+C to stop)"
echo
./curio-server
