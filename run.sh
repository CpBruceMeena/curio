#!/bin/bash
# run.sh — Curio backend lifecycle manager
# Usage: ./run.sh {start|stop|restart|status}

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
PID_FILE="$ROOT_DIR/.backend.pid"
BINARY="$BACKEND_DIR/curio-server"
PORT="${PORT:-8080}"

# ── helpers ──────────────────────────────────────────────────────────

print_usage() {
    echo "Usage: $0 {start|stop|restart|status}"
    echo ""
    echo "  start    Build and start the backend server (daemon)"
    echo "  stop     Stop the running backend server"
    echo "  restart  Stop then start the backend server"
    echo "  status   Show whether the server is running"
}

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
err()  { log "ERROR: $*" >&2; }

get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

is_running() {
    local pid
    pid="$(get_pid)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        return 0
    fi
    return 1
}

clean_pid() {
    rm -f "$PID_FILE"
}

# ── commands ─────────────────────────────────────────────────────────

cmd_start() {
    if is_running; then
        log "Backend is already running (PID $(get_pid))"
        return 0
    fi

    log "Building backend..."
    cd "$BACKEND_DIR"
    go build -o "$BINARY" .
    cd "$ROOT_DIR"

    log "Starting Curio backend on port $PORT..."
    nohup "$BINARY" > "$ROOT_DIR/backend/server.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"

    # Give it a moment to boot, then check
    sleep 2
    if kill -0 "$pid" 2>/dev/null; then
        log "Backend started (PID $pid) — listening on :$PORT"
    else
        err "Backend failed to start. Check backend/server.log for details."
        clean_pid
        return 1
    fi
}

cmd_stop() {
    local pid
    pid="$(get_pid)"

    if [ -z "$pid" ]; then
        log "No PID file found — nothing to stop."
        return 0
    fi

    if ! kill -0 "$pid" 2>/dev/null; then
        log "Process $pid is not running — cleaning up."
        clean_pid
        return 0
    fi

    log "Stopping backend (PID $pid)..."
    kill "$pid" 2>/dev/null || true

    # Wait up to 10 seconds for graceful shutdown
    for i in $(seq 1 10); do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        sleep 1
    done

    # Force kill if still alive
    if kill -0 "$pid" 2>/dev/null; then
        log "Process did not exit gracefully — force killing..."
        kill -9 "$pid" 2>/dev/null || true
    fi

    clean_pid
    log "Backend stopped."
}

cmd_restart() {
    cmd_stop
    cmd_start
}

cmd_status() {
    local pid
    pid="$(get_pid)"
    if is_running; then
        echo "● Curio backend is running (PID $pid, port :$PORT)"
    else
        if [ -n "$pid" ]; then
            echo "○ Stale PID file found (PID $pid not running)"
        fi
        echo "○ Curio backend is not running"
    fi
}

# ── dispatch ─────────────────────────────────────────────────────────

if [ $# -lt 1 ]; then
    print_usage
    exit 1
fi

case "${1:-}" in
    start)   cmd_start   ;;
    stop)    cmd_stop    ;;
    restart) cmd_restart ;;
    status)  cmd_status  ;;
    *)       print_usage; exit 1 ;;
esac
