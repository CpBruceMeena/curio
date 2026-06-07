# Curio — The Infinite Curiosity App

> **One interesting thing at a time.**

Discover fascinating facts, inspiring quotes, historical events, startup stories, AI insights, and mind-blowing discoveries through an addictive swipe-based experience.

## Architecture

- **Backend**: Go + Gin + GORM + PostgreSQL
- **Android**: Kotlin + Jetpack Compose + Material 3
- **Database**: PostgreSQL 18

## Quick Start

### Prerequisites

- Go 1.26+
- PostgreSQL 18+
- Android Studio (for Android development)
- Java 17+ (for Android builds)

### Backend Setup

1. **Navigate to backend:**
   ```bash
   cd backend
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your database credentials
   ```

3. **Set up the database:**
   ```bash
   createdb curio
   ```

4. **Seed the database:**
   ```bash
   go run cmd/seed/main.go
   ```

5. **Start the server:**
   ```bash
   go run main.go
   ```

   The server starts on `http://localhost:8080`.

### Android Setup

1. **Open the Android project:**
   ```bash
   open android/
   ```

2. **Set ANDROID_HOME:**
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk
   ```

3. **Build debug APK:**
   ```bash
   cd android && ./gradlew assembleDebug
   ```

   APK location: `android/app/build/outputs/apk/debug/app-debug.apk`

4. **Install on emulator:**
   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

   The Android app connects to the backend at `http://10.0.2.2:8080/api/v1/` (emulator → host localhost).

### Environment Variables

| Variable        | Default                                                              | Description           |
|-----------------|----------------------------------------------------------------------|-----------------------|
| `PORT`          | `8080`                                                               | Server port           |
| `DATABASE_URL`  | `postgres://postgres:password@localhost:5432/curio?sslmode=disable`  | PostgreSQL connection |

## API Endpoints

| Method | Path                     | Description             |
|--------|--------------------------|-------------------------|
| GET    | `/health`                | Health check            |
| GET    | `/api/v1/feed`           | Paginated feed          |
| GET    | `/api/v1/content/:id`    | Content detail          |
| POST   | `/api/v1/content/:id/like` | Like content          |
| GET    | `/api/v1/categories`     | List categories         |

### Feed Query Parameters

- `page` (default: 1) — Page number
- `page_size` (default: 10, max: 50) — Items per page
- `category_id` — Filter by category

## Project Structure

```
curio/
├── backend/
│   ├── cmd/seed/          # Database seeder
│   ├── config/            # App configuration
│   ├── data/              # Seed data
│   ├── database/          # DB connection & migrations
│   ├── handlers/          # HTTP handlers
│   ├── middleware/        # CORS middleware
│   ├── models/            # Data models
│   ├── router/            # Route definitions
│   ├── main.go            # Entry point
│   └── .env.example       # Environment template
├── android/
│   └── app/src/main/java/com/curio/app/
│       ├── data/          # API, models, repository
│       ├── ui/            # Screens, components, theme, navigation
│       └── viewmodel/     # ViewModels
├── .gitignore
└── README.md
```

## Tech Stack

- **Backend**: Go 1.26, Gin, GORM, PostgreSQL, godotenv
- **Android**: Kotlin, Jetpack Compose, Material 3, Retrofit, Coil, Navigation Compose
- **Design**: Hanken Grotesk, Dark Mode, Glassmorphism, Emerald/Cyan/Gold palette
