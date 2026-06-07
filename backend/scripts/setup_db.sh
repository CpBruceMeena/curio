#!/bin/bash
# Curio Database Setup Script
# Run this once to initialize PostgreSQL for Curio
# Prerequisites: PostgreSQL must be running (Postgres.app recommended)

set -e

echo "=== Curio Database Setup ==="

# Try common Postgres.app socket paths
if [ -e "/tmp/.s.PGSQL.5432" ]; then
    PG_HOST="/tmp"
elif [ -e "/var/run/postgresql/.s.PGSQL.5432" ]; then
    PG_HOST="/var/run/postgresql"
else
    PG_HOST="localhost"
fi

echo "Connecting via socket: $PG_HOST"

# Create user (ignore error if exists)
psql -h "$PG_HOST" -d postgres -c "CREATE USER curio WITH PASSWORD 'curio' CREATEDB;" 2>/dev/null || echo "✓ User 'curio' already exists"

# Create database (ignore error if exists)
psql -h "$PG_HOST" -d postgres -c "CREATE DATABASE curio OWNER curio;" 2>/dev/null || echo "✓ Database 'curio' already exists"

# Grant privileges
psql -h "$PG_HOST" -d postgres -c "GRANT ALL PRIVILEGES ON DATABASE curio TO curio;" 2>/dev/null

echo ""
echo "✓ Database setup complete!"
echo ""
echo "Next steps:"
echo "  1. cd backend"
echo "  2. cd ../scripts && source venv/bin/activate && python seed.py  (to populate with initial content)"
echo "  3. go run .            (to start the API server on :8080)"
echo ""
