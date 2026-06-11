#!/bin/bash
# run.sh — Curio backend lifecycle manager + TTS Docker
# Usage: ./run.sh {start|stop|restart|status}
#
# Manages two services:
#   1. Go backend  (port 8080)
#   2. TTS Docker  (port 5050) — Microsoft Edge TTS via travisvn/openai-edge-tts
#
# The Go backend's TTS handler connects to the Docker container internally.
# ngrok is managed separately by the user.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
PID_FILE="$ROOT_DIR/.backend.pid"
BINARY="$BACKEND_DIR/curio-server"
PORT="${PORT:-8080}"
TTS_DOCKER_IMAGE="${TTS_DOCKER_IMAGE:-travisvn/openai-edge-tts:latest}"
TTS_DOCKER_NAME="${TTS_DOCKER_NAME:-edge-tts}"
TTS_PORT="${TTS_PORT:-5050}"

# ── helpers ──────────────────────────────────────────────────────────

print_usage() {
    echo "Usage: $0 {start|stop|restart|status}"
    echo ""
    echo "  start    Start TTS Docker + Go backend"
    echo "  stop     Stop TTS Docker + Go backend"
    echo "  restart  Stop then start all services"
    echo "  status   Show status of all services"
    echo ""
    echo "Environment variables (optional):"
    echo "  PORT=8080          Backend port"
    echo "  TTS_PORT=5050      TTS container port"
}

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
err()  { log "ERROR: $*" >&2; }

# ── PID helpers ──────────────────────────────────────────────────────

get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

is_pid_running() {
    local pid
    pid="${1:-$(get_pid)}"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        return 0
    fi
    return 1
}

clean_pid() {
    rm -f "$PID_FILE"
}

# ── Port helpers ─────────────────────────────────────────────────────

is_port_in_use() {
    local port="${1:-$PORT}"
    lsof -ti:"$port" &>/dev/null
}

get_port_pid() {
    local port="${1:-$PORT}"
    lsof -ti:"$port" 2>/dev/null
}

free_port() {
    local port="${1:-$PORT}"
    local port_pid
    port_pid="$(get_port_pid "$port")"
    if [ -n "$port_pid" ]; then
        log "Port $port is in use by PID $port_pid — freeing..."
        kill "$port_pid" 2>/dev/null || true
        sleep 1
        if is_port_in_use "$port"; then
            kill -9 "$port_pid" 2>/dev/null || true
            sleep 1
        fi
    fi
}

# ── Health checks ────────────────────────────────────────────────────

backend_health() {
    if curl -sf "http://localhost:$PORT/health" >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

tts_docker_health() {
    if docker ps --filter "name=$TTS_DOCKER_NAME" --format '{{.Status}}' | grep -q 'Up'; then
        return 0
    fi
    return 1
}

# ══════════════════════════════════════════════════════════════════════
# TTS Docker commands
# ══════════════════════════════════════════════════════════════════════

cmd_tts_docker_start() {
    if tts_docker_health; then
        log "TTS Docker container '$TTS_DOCKER_NAME' is already running."
        return 0
    fi

    # Remove old container if it exists (stopped or running)
    local exists
    exists="$(docker ps -a --filter "name=$TTS_DOCKER_NAME" --format '{{.Names}}' 2>/dev/null || true)"
    if [ -n "$exists" ]; then
        log "Removing old container '$TTS_DOCKER_NAME'..."
        docker rm -f "$TTS_DOCKER_NAME" >/dev/null 2>&1
    fi

    log "Starting TTS Docker container '$TTS_DOCKER_NAME' on port $TTS_PORT..."
    docker run -d \
        --name "$TTS_DOCKER_NAME" \
        -p "$TTS_PORT:$TTS_PORT" \
        -e REQUIRE_API_KEY=False \
        --restart unless-stopped \
        "$TTS_DOCKER_IMAGE" >/dev/null 2>&1

    # Wait for container to be healthy (up to 30s for image pull)
    local waited=0
    while [ "$waited" -lt 30 ]; do
        sleep 1
        waited=$((waited + 1))
        if tts_docker_health; then
            log "TTS Docker container started (port $TTS_PORT)"
            return 0
        fi
        # Print a progress dot every 5s
        if [ $((waited % 5)) -eq 0 ]; then
            docker logs "$TTS_DOCKER_NAME" 2>&1 | tail -1 || true
        fi
    done

    err "TTS Docker container failed to start. Check 'docker logs $TTS_DOCKER_NAME'"
    return 1
}

cmd_tts_docker_stop() {
    if ! tts_docker_health; then
        log "TTS Docker container is not running."
        return 0
    fi
    log "Stopping TTS Docker container '$TTS_DOCKER_NAME'..."
    docker stop "$TTS_DOCKER_NAME" >/dev/null 2>&1
    log "TTS Docker container stopped."
}

# ══════════════════════════════════════════════════════════════════════
# Backend commands
# ══════════════════════════════════════════════════════════════════════

cmd_backend_start() {
    if is_pid_running; then
        log "Backend is already running (PID $(get_pid))"
        return 0
    fi

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

    local waited=0
    while [ "$waited" -lt 8 ]; do
        sleep 1
        waited=$((waited + 1))

        if ! kill -0 "$pid" 2>/dev/null; then
            err "Backend failed to start. Check backend/server.log for details."
            err "Last 10 lines of log:"
            tail -10 "$ROOT_DIR/backend/server.log" >&2
            clean_pid
            return 1
        fi

        if backend_health; then
            log "Backend started (PID $pid) — listening on :$PORT"
            return 0
        fi
    done

    err "Backend started but not responding on :$PORT after ${waited}s."
    err "Last 10 lines of log:"
    tail -10 "$ROOT_DIR/backend/server.log" >&2
    clean_pid
    return 1
}

cmd_backend_stop() {
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

    for i in $(seq 1 10); do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        sleep 1
    done

    if kill -0 "$pid" 2>/dev/null; then
        log "Process did not exit gracefully — force killing..."
        kill -9 "$pid" 2>/dev/null || true
    fi

    clean_pid
    log "Backend stopped."
}

# ══════════════════════════════════════════════════════════════════════
# Composite commands
# ══════════════════════════════════════════════════════════════════════

cmd_start() {
    log "═══ Starting Curio services ═══"
    cmd_tts_docker_start
    cmd_backend_start
    log "═══ All services started ═══"
    echo ""
    echo "  Backend API:   http://localhost:$PORT"
    echo "  TTS Docker:    http://localhost:$TTS_PORT"
    echo ""
}

cmd_stop() {
    log "═══ Stopping Curio services ═══"
    cmd_backend_stop
    cmd_tts_docker_stop
    log "═══ All services stopped ═══"
}

cmd_restart() {
    cmd_stop
    cmd_start
}

cmd_status() {
    echo ""
    echo "═══ Curio Service Status ═══"
    echo ""

    local pid
    pid="$(get_pid)"
    if is_pid_running "$pid"; then
        echo "  ● Backend     running  (PID $pid, port :$PORT)"
    else
        echo "  ○ Backend     not running"
    fi

    if tts_docker_health; then
        local tts_uptime
        tts_uptime="$(docker ps --filter "name=$TTS_DOCKER_NAME" --format '{{.Status}}')"
        echo "  ● TTS Docker  running  (port $TTS_PORT — $tts_uptime)"
    else
        echo "  ○ TTS Docker  not running"
    fi

    echo ""
    echo "═══ Endpoints ═══"
    echo "  Backend API    http://localhost:$PORT"
    echo "  TTS Docker     http://localhost:$TTS_PORT"
    echo ""
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
