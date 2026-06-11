# Curio — The Infinite Curiosity App

> **One interesting thing at a time.**

Curio is a knowledge discovery app for the endlessly curious. Swipe through bite-sized, fascinating facts across science, space, history, philosophy, poetry, and more. Each card delivers a single, well-sourced insight — giving you something new to wonder about in seconds.

---

## Features

### ✨ Splash
A cinematic video background with an animated cube logo and elegant text overlay — setting the tone for discovery the moment you open the app.

### 🧠 Onboarding
Tell Curio what excites you. Pick your interests from 21+ categories across 4 L1 sections (Facts, Poems, Short Stories, Puzzles) — from quantum physics to Urdu poetry — and your feed learns what fascinates you from day one.

### ♾️ Infinite Feed
Swipe through an endless stream of knowledge cards with a stunning 3D parallax effect. Each card is a beautifully presented insight with:
- **Bite-sized facts** — quick reads you can digest in seconds
- **Poetry & Shayari** — full poems with rich contextual descriptions and poet credits (including Devanagari Urdu script support)
- **Short stories** — micro-narratives you can finish in minutes
- **Interactive Puzzles** — Sudoku, Math, Logic, and Word puzzles with built-in validation
- **3D swipe navigation** — tilt and parallax effects as you glide from one discovery to the next
- **Share & Bookmark** — share insights with friends or save your favorites for later

### 🔍 Discover
Explore the full universe of categories. Browse by L1 sections (Facts, Poems, Short Stories, Puzzles), pick subcategories, apply filters, and find your next obsession in a clean 2-column grid with emoji icons. Content refreshes every time you open Discover.

### 📑 Bookmarks
Save your favorite insights with a single tap. Your bookmarks are stored locally on your device and persist across sessions. Access all your saved content from a dedicated bookmarks tab.

### 🃏 Shuffle
Hit the shuffle button to get a fresh batch of content from across **all categories** — the perfect way to discover something unexpected.

### 🧩 Puzzles
Test your mind with interactive puzzles:
- **Sudoku** — classic number puzzles
- **Math Puzzles** — number-based challenges
- **Logic Puzzles** — reasoning problems
- **Word Puzzles** — vocabulary and wordplay

Each puzzle supports validation, hints, and explanations.

### 👤 Profile & Personalization
Create a profile to help Curio personalize your experience. Share your name, age, gender, likes, and dislikes — all stored securely on the backend and linked to your device via a unique UUID. A dedicated profile tab lets you manage your information and preferences.

### 🔊 Text-to-Speech
Every card in your feed can be read aloud with a single tap. Curio uses Microsoft Edge TTS (via a self-hosted Docker container) to generate natural-sounding narration tailored to each category:
- **Facts & Stories** — narrated by Aria (warm, clear female voice)
- **Poetry & Shayari** — expressive voices: Jenny for English poems, Salman for Urdu Shayari, Swara for Hindi poems
- **Classic Literature** — Sonia (British English) for a classic feel
- **Puzzles** — Davis (calm, precise male voice)
- **Serialized Stories** — Guy (authoritative male voice)

**Auto-play mode** lets you sit back and listen to an entire feed session — audio plays through each card, pauses briefly between cards, and auto-swipes to the next. Tap the ▶️ button in the top bar to start, or use the per-card ▶️ button to play individual cards.

### 📱 Device-Based Identification
Curio generates a persistent UUID on first launch, stored securely in the app's local storage. All profile, feedback, and device-info API calls are attributed to this UUID — enabling personalized experiences without requiring an account or login.

### 💬 Feedback
Share your thoughts directly from the app via the feedback dialog — now linked to your device UUID so we can better understand and improve the experience.

---

## Categories

Curio covers **21+ categories** — each with its own distinct icon:

`🧬 Science` `🚀 Space` `📜 History` `🌿 Biology` `🧠 Psychology` `⚖️ Philosophy` `⚛️ Physics` `💡 Startups` `🤖 AI` `🏛️ Economics` `🌲 Nature` `💻 Technology` `📖 Poetry` `🎬 Movies` `🔬 Neuroscience` `📚 Literature` `🌍 Geography` `🎵 Music` `⚽ Sports` `🍜 Food` `✍️ Shayari`

Organized into 4 L1 sections:
- **Facts** — Science, Space, History, Biology, Psychology, Philosophy, Physics, Startups, AI, Economics, Nature, Technology, Movies, Neuroscience, Geography, Music, Sports, Food
- **Poems** — Poetry, Shayari
- **Short Stories** — Literature
- **Puzzles** — Sudoku, Math Puzzles, Logic Puzzles, Word Puzzles, Mixed Puzzles

---

## Product Philosophy

Curio is built around a simple belief: **knowledge should feel like discovery, not work.**

Every piece of content is condensed to a single insight — no fluff, no filler. Poetry and shayari come with rich contextual explanations, helping you understand *why* a poem matters, not just what it says.

The app is designed for the commute, the coffee break, or the moment you just want to learn something new without committing to a deep dive. One interesting thing at a time.

---

## Design

- **Dark Mode** — A thoughtfully designed dark interface with a refined emerald/cyan/gold palette. Easy on the eyes, beautiful at any hour.
- **Vibrant Gradients** — Dynamic gradient overlays on cards create depth and visual richness.
- **3D Parallax Cards** — Feed cards respond to swipe with subtle tilt and depth effects for an immersive reading experience.
- **Glassmorphism** — Subtle glass-like surfaces, soft borders, and layered depth.

---

## Tech Stack

- **Frontend:** Android (Jetpack Compose + Kotlin)
- **Backend:** Go (Gin + GORM)
- **Database:** PostgreSQL
- **Text-to-Speech:** Microsoft Edge TTS via Docker (travisvn/openai-edge-tts)
- **Data Scraping:** Python

---

## Running Locally

### Prerequisites
- [Go](https://go.dev/) (1.21+)
- [PostgreSQL](https://www.postgresql.org/) running on localhost:5432
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (for TTS)
- Android Studio & an Android emulator (for the app)

### Quick Start

```bash
# Start all services (TTS Docker + Go backend)
./run.sh start

# Check status
./run.sh status

# Stop all services
./run.sh stop
```

The `run.sh` script manages:
1. **TTS Docker** — starts the `edge-tts` container on port 5050
2. **Go backend** — builds and starts on port 8080

On the Android side, open `android/` in Android Studio and run on an emulator. The app connects to `https://helpful-supposedly-moose.ngrok-free.app` (managed separately) which tunnels to `localhost:8080`.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Backend port |
| `TTS_PORT` | `5050` | TTS container port |

---

## Screens

| Screen | Purpose |
|--------|---------|
| **Splash** | Cinematic video background with animated cube logo |
| **Onboarding** | L1/L2 interest selection from 4 sections (Facts, Poems, Stories, Puzzles) |
| **Feed** | Swipeable full-page knowledge cards with 3D parallax, share, bookmark & shuffle |
| **Discover** | 2-column category grid with emoji icons, L1 pills, and filters |
| **Bookmarks** | Locally-saved collection of your favorite content |
| **Puzzles** | Interactive Sudoku, Math, Logic, and Word puzzles |
| **Content Detail** | Full-screen view of any content item |
| **Profile** | Personal info form, device UUID, privacy notice & terms, dark mode toggle |
