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

is_pid_running() {
    local pid
    pid="$(get_pid)"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        return 0
    fi
    return 1
}

# Check if anything is listening on the port
is_port_in_use() {
    lsof -ti:"$PORT" &>/dev/null
}

# Get the PID of whatever is listening on the port
get_port_pid() {
    lsof -ti:"$PORT" 2>/dev/null
}

clean_pid() {
    rm -f "$PID_FILE"
}

free_port() {
    local port_pid
    port_pid="$(get_port_pid)"
    if [ -n "$port_pid" ]; then
        log "Port $PORT is in use by PID $port_pid — freeing..."
        kill "$port_pid" 2>/dev/null || true
        sleep 1
        if is_port_in_use; then
            kill -9 "$port_pid" 2>/dev/null || true
            sleep 1
        fi
    fi
}

health_check() {
    local url="http://localhost:$PORT/health"
    if curl -sf "$url" >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

# ── commands ─────────────────────────────────────────────────────────

cmd_start() {
    if is_pid_running; then
        log "Backend is already running (PID $(get_pid))"
        return 0
    fi

    # If something else is on the port (stale process), free it
    if is_port_in_use; then
        log "Port $PORT is occupied by a different process."
        free_port
    fi

    log "Building backend..."
    cd "$BACKEND_DIR"
    go build -o "$BINARY" .
    cd "$ROOT_DIR"

    log "Starting Curio backend on port $PORT..."
    nohup "$BINARY" > "$ROOT_DIR/backend/server.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"

    # Wait for the process to start and become healthy
    local waited=0
    while [ "$waited" -lt 8 ]; do
        sleep 1
        waited=$((waited + 1))

        if ! kill -0 "$pid" 2>/dev/null; then
            # Process died — report error with log tail
            err "Backend failed to start. Check backend/server.log for details."
            err "Last 10 lines of log:"
            tail -10 "$ROOT_DIR/backend/server.log" >&2
            clean_pid
            return 1
        fi

        if health_check; then
            log "Backend started (PID $pid) — listening on :$PORT"
            return 0
        fi
    done

    # Timed out waiting for health check
    err "Backend started but not responding on :$PORT after ${waited}s."
    err "Last 10 lines of log:"
    tail -10 "$ROOT_DIR/backend/server.log" >&2
    clean_pid
    return 1
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
    local port_pid
    port_pid="$(get_port_pid || true)"

    if is_pid_running; then
        echo "● Curio backend is running (PID $pid, port :$PORT)"
    elif [ -n "$port_pid" ]; then
        if [ -n "$pid" ]; then
            echo "○ Mismatch — PID file says $pid but port $PORT is held by PID $port_pid"
        else
            echo "○ Port $PORT is in use by PID $port_pid (not tracked by PID file)"
        fi
        echo "  Run './run.sh restart' to reclaim."
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
