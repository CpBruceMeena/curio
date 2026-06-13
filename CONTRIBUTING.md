# Contributing to Curio

## Prerequisites

- **Android** — Android Studio, JDK 17, an emulator or device running API 34+
- **Backend** — Go 1.26+, PostgreSQL 15+
- **Scraper** — Python 3.11+, `venv`

## Getting Started

### 1. Database

```bash
createdb curio
```

The backend will auto-migrate tables on first run. To seed initial content:

```bash
cd scripts
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python -m scraper --batch 100
python -m scraper --novels-batch 20
```

### 2. Backend

```bash
cd backend
cp .env.example .env    # edit DATABASE_URL if needed
go mod tidy
go build -o backend .
./backend               # runs on :8080
```

Default database URL: `postgres://postgres:password@localhost:5432/curio?sslmode=disable`

### 3. Android

Open `android/` in Android Studio or build via CLI:

```bash
cd android
./gradlew assembleDebug installDebug
```

The app connects to the backend at the address configured in `CurioApi.kt`. For local development, use `10.0.2.2:8080` (Android emulator → host loopback).

## Project Structure

```
├── android/          # Jetpack Compose app
│   └── app/src/main/java/com/curio/app/
│       ├── data/     # API, models, repository, local storage (Room)
│       ├── ui/       # Screens, components, theme, navigation
│       └── viewmodel/# ViewModels per screen
├── backend/          # Go API server (Gin + GORM)
│   ├── handlers/     # HTTP handlers
│   ├── models/       # GORM models
│   ├── router/       # Route definitions
│   └── database/     # DB connection & migrations
└── scripts/          # Python scraper
    └── scraper/
        ├── handlers/ # Fetch functions per source
        └── sources.yaml  # Source registry
```

## Scraping Content

```bash
cd scripts && source venv/bin/activate

# Feed content (facts, poems, stories, puzzles)
python -m scraper --batch 200

# Novels (curated public-domain list)
python -m scraper --novels-batch 20

# Single category (e.g. after archiving)
python -m scraper --archive "Science" --batch 50

# Preview without writing to DB
python -m scraper --limit 5 --dry-run
```

Sources are defined in `scripts/scraper/sources.yaml`. To add a new source:
1. Write a handler function in `scripts/scraper/handlers/`
2. Add an entry in `sources.yaml`
3. Register it in `scripts/scraper/handlers/__init__.py`

## Adding Novels

Curated novels live in `scripts/scraper/handlers/novels.py`. Each entry needs a Gutenberg ID, title, author, description, and ideally a PDF URL from Global Grey Ebooks or similar. The scraper tries PDF → EPUB → plain text in order.

## Code Style

- **Kotlin** — Follow existing Compose patterns. Run `./gradlew ktlintCheck` before committing.
- **Go** — `gofmt` + standard Go conventions. Run `go vet ./...` before committing.
- **Python** — PEP 8, 4-space indentation.
