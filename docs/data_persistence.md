# Data Persistence in Curio

This doc explains what data the app stores locally, where it lives, and what
survives (or doesn't) across updates, re-installs, and schema migrations.

---

## 1. Storage Layers

Curio uses three local storage mechanisms:

| Layer | Technology | Location | Purpose |
|-------|-----------|----------|---------|
| **SharedPreferences** | Android `SharedPreferences` | `curio_prefs.xml` | Key-value settings & lightweight state |
| **Room Database** | SQLite via Room | `curio_journal.db` | Tables: journal entries, bookmarks, offline cache, novel data, annotations |
| **App Cache** | Temp files on disk | `app/cache/` | TTS audio files generated at runtime |

---

## 2. What's Stored Where

### 2.1 SharedPreferences (`PreferencesHelper`)

| Key | Type | Example | Used For |
|-----|------|---------|----------|
| `selected_categories` | `Set<String>` | `{"Science", "Space"}` | Feed filtering by user interest |
| `onboarding_done` | `Boolean` | `true` | Whether first-launch flow is complete |
| `dark_theme` | `Boolean` | `true` | Theme preference |
| `bookmarked_content_ids` | `Set<String>` | `{"210000001", "220000013"}` | List of bookmarked content IDs (metadata stored in Room) |
| `device_uuid` | `String` | UUID | Unique device identifier for API calls |
| `device_info_submitted` | `Boolean` | `true` | Whether device info has been sent to backend |
| `journal_font_family` | `String` | `"serif"` | Journal editor font preference |
| `journal_font_size` | `String` | `"medium"` | Journal editor font size |
| `journal_line_spacing` | `Float` | `1.8` | Journal editor line spacing |

### 2.2 Room Database (`curio_journal.db`)

| Table | Entity Class | Content |
|-------|-------------|---------|
| `journal_entries` | `JournalEntry` | Journal entries (title, body, prompt, mood, date) |
| `bookmarked_content` | `BookmarkedContent` | Full Content data saved for offline reading |
| `cached_content` | `CachedContent` | ~25 items per L1 category populated on first launch |
| `offline_novels` | `OfflineNovel` | Downloaded novel metadata |
| `offline_novel_chapters` | `OfflineNovelChapter` | Downloaded novel chapter text |
| `local_novel_progress` | `LocalNovelProgress` | Current reading position in each novel |
| `saved_annotations` | `SavedAnnotation` | User highlights & notes on content |

### 2.3 Temp Files (App Cache)

| Data | Location | Lifetime |
|------|----------|----------|
| TTS audio MP3s | `context.cacheDir/tts_*.mp3` | Temp — created on play, eligible for system cache cleanup |

---

## 3. Persistence Behavior

### 3.1 On App Update (version upgrade via Play Store / APK install)

| Data | Survives? | Notes |
|------|-----------|-------|
| SharedPreferences (all keys) | ✅ Yes | Never wiped on update |
| Room Database tables | ✅ Yes | Data survives, **unless** Room version is bumped |
| TTS audio cache | ✅ Yes | Temp files persist in cache dir |
| **Room on schema version bump** | ⚠️ **No — wiped** | Uses `fallbackToDestructiveMigration()` — ALL tables dropped & recreated |

> ⚠️ **Critical**: The Room database uses `fallbackToDestructiveMigration()`.
> Whenever `JournalDatabase.version` is incremented, **all user data is lost**:
> journal entries, offline novels, reading progress, bookmarked content,
> cached content, and annotations. Every migration needs careful handling
> to avoid this.

### 3.2 On Uninstall + Reinstall

| Data | Survives? | Notes |
|------|-----------|-------|
| Everything | ❌ **No** | Android removes all app-specific data on uninstall |
| **Android Auto Backup** | ✅ **Maybe** | `AndroidManifest.xml` has `android:allowBackup="true"` |

**Android Auto Backup details:**
- Available on Android 6.0+ (API 23)
- Backs up to the user's Google Drive (up to 25MB per app)
- Restored automatically on reinstall if the user has backup enabled
- **Caveats:** Not all devices/ROMs support it. Backup pauses if the app isn't used for 14+ days. Users can opt out in device settings.
- Default backup includes: `shared_prefs/` and `databases/` (Room DB)

### 3.3 On App Data Clear (Settings → Apps → Curio → Clear Data)

| Data | Survives? |
|------|-----------|
| SharedPreferences | ❌ Cleared |
| Room Database | ❌ Deleted |
| TTS temp files | ❌ Deleted |

### 3.4 On Force Stop or Crash

| Data | Survives? |
|------|-----------|
| All layers | ✅ Yes — data is written to disk synchronously |

---

## 4. Offline Cache Behavior

On first launch, `FeedViewModel.populateOfflineCacheIfNeeded()` populates
the `cached_content` table with ~25 items per L1 category
(Facts, Poems, Short Stories, Puzzles).

- Runs **once** (checked via `cacheDao.count() > 0`)
- Used as **fallback** when the network is unavailable (`loadFeed()` on failure)
- Cleared on Room migration / uninstall

---

## 5. Recommendations

### 5.1 Before Next Schema Bump

Replace `fallbackToDestructiveMigration()` with proper migrations:

```kotlin
// Instead of:
.fallbackToDestructiveMigration()

// Use:
.addMigrations(MIGRATION_6_7, MIGRATION_7_8)
```

### 5.2 Backup Config (Optional)

To opt out of Auto Backup for certain files (preventing stale cache from
restoring):

```xml
<application
    android:allowBackup="true"
    android:fullBackupContent="@xml/backup_rules">
```

With `res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Don't restore stale offline cache -->
    <exclude domain="database" path="curio_journal.db"/>
</full-backup-content>
```

---

## 6. Debugging Local Data

```bash
# Pull SharedPreferences
adb shell cat /data/data/com.curio.app/shared_prefs/curio_prefs.xml

# Pull Room database
adb exec-out run-as com.curio.app cat databases/curio_journal.db > /tmp/journal.db

# Query with sqlite3
sqlite3 /tmp/journal.db ".tables"
sqlite3 /tmp/journal.db "SELECT COUNT(*) FROM journal_entries;"

# View app cache
adb shell ls /data/data/com.curio.app/cache/
```
