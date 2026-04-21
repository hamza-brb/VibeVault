# VibeVault — Technical Design Document

**Project:** VibeVault Desktop Music Player  
**Course:** CS-220 Database Systems  
**Version:** 1.0  
**Date:** March 29, 2026  
**Authors:** Muhammad Hamza Hashim (571509), Muhammad Shehryar Azhar (570992)  
**Submitted To:** Ms. Maryam Sajjad | Section BSCS-15B

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Goals & Scope](#2-goals--scope)
3. [System Architecture](#3-system-architecture)
4. [Technology Stack](#4-technology-stack)
5. [Feature Requirements](#5-feature-requirements)
6. [Database Design](#6-database-design)
7. [Application Layer Design](#7-application-layer-design)
8. [UI/UX Design Plan](#8-uiux-design-plan)
9. [Project Phases & Milestones](#9-project-phases--milestones)
10. [Risk Assessment](#10-risk-assessment)
11. [Appendix: SQL Schema](#11-appendix-sql-schema)

---

## 1. Project Overview

VibeVault is a cross-platform **desktop music player** built with Java and SQLite. It functions like a personal, local version of Spotify — allowing users to browse their music library by artist and album, manage playlists and a playback queue, and most distinctively, **track detailed listening statistics** over time. The app supports multiple user accounts on a single machine, each with their own library, playlists, and stats.

The project's primary academic purpose is to demonstrate core database concepts: relational schema design, referential integrity, CRUD operations, aggregate reporting queries, and multi-user data isolation — all surfaced through a polished Java Swing GUI.

---

## 2. Goals & Scope

### 2.1 In-Scope (MVP)

| # | Feature |
|---|---------|
| 1 | User registration and login (multi-user support) |
| 2 | Local music library import and management |
| 3 | Artist and album browsing |
| 4 | Full audio playback: play, pause, skip, seek, volume |
| 5 | Playlists: create, edit, delete, reorder songs |
| 6 | Playback queue management |
| 7 | Listening stats and reports dashboard |

### 2.2 Out-of-Scope (Future Enhancements)

- Online streaming or music discovery
- Social/sharing features
- Mobile platform support
- External API integration (e.g., Last.fm scrobbling)
- Advanced audio effects or equalizer

---

## 3. System Architecture

VibeVault follows a classic **3-layer desktop architecture**:

```
┌──────────────────────────────────────┐
│         Presentation Layer           │
│         (Java Swing GUI)             │
│  Views: Library, Player, Playlists,  │
│         Stats, Login                 │
└────────────────┬─────────────────────┘
                 │ calls
┌────────────────▼─────────────────────┐
│          Business Logic Layer        │
│  Services: AuthService, PlayerService│
│  LibraryService, PlaylistService,    │
│  StatsService                        │
└────────────────┬─────────────────────┘
                 │ reads/writes
┌────────────────▼─────────────────────┐
│            Data Access Layer         │
│  DAOs: UserDAO, SongDAO, ArtistDAO,  │
│  AlbumDAO, PlaylistDAO, PlayHistoryDAO│
└────────────────┬─────────────────────┘
                 │ SQL via JDBC
┌────────────────▼─────────────────────┐
│         SQLite Database File         │
│         (vibevault.db)               │
└──────────────────────────────────────┘
```

### 3.1 Design Pattern

The application uses a **Model-View-Controller (MVC)** pattern:

- **Model** — Java POJOs representing entities (Song, Artist, Album, User, Playlist) mapped to database tables.
- **View** — Java Swing panels/frames for each screen.
- **Controller** — Event handlers in each view delegate logic to service classes; services use DAOs to communicate with SQLite.

---

## 4. Technology Stack

| Component | Technology | Justification |
|-----------|-----------|---------------|
| Language | Java 17+ | Cross-platform JVM, strong OOP, rich library ecosystem |
| GUI Framework | Java Swing | Built into the JDK, no extra dependency, sufficient for desktop apps |
| Audio Playback | JLayer (MP3) / Java Sound API (WAV) | Lightweight, open-source, easy JDBC integration |
| Database | SQLite 3 via `sqlite-jdbc` | Serverless, file-based, zero config; perfect for single-machine apps |
| Database Driver | `org.xerial:sqlite-jdbc` (Maven/Gradle) | Official SQLite JDBC driver for Java |
| Build Tool | Maven or Gradle | Dependency management, reproducible builds |
| Version Control | Git + GitHub | Collaboration and history |
| IDE | IntelliJ IDEA / Eclipse | Standard Java IDEs |

### 4.1 Key Dependencies (Maven `pom.xml`)

```xml
<!-- SQLite JDBC Driver -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>

<!-- MP3 Playback (JLayer) -->
<dependency>
    <groupId>javazoom</groupId>
    <artifactId>jlayer</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- MP3 tag reading (for metadata import) -->
<dependency>
    <groupId>org</groupId>
    <artifactId>jaudiotagger</artifactId>
    <version>2.2.5</version>
</dependency>
```

---

## 5. Feature Requirements

### 5.1 User Authentication

- Users can **register** with a username and password.
- Passwords are stored as **SHA-256 hashed** strings (never plaintext).
- Users can **log in** and **log out**. The session is held in memory as a `currentUser` object.
- All data (library, playlists, stats) is scoped to the logged-in user.
- Admin user can see aggregate stats across all users (optional stretch goal).

### 5.2 Music Library & Import

- Users can **add songs** by selecting local audio files (MP3, WAV) via a file chooser dialog.
- On import, the app reads **ID3 metadata** (title, artist, album, duration, year, genre) using jaudiotagger and stores it in the database.
- If a song's artist or album doesn't exist yet, new records are created automatically.
- Users can **view their library** in a table view, sortable by title, artist, album, duration.
- Songs can be **removed** from the library (with confirmation).

### 5.3 Artist & Album Browsing

- A **Browse** panel shows all artists in the user's library, with song count and total duration.
- Clicking an artist shows their albums and songs in a detail view.
- Clicking an album shows all tracks in that album in order.
- Cover art (if embedded in the MP3) is extracted and displayed.

### 5.4 Playback

- **Now Playing** bar at the bottom of the window shows: song title, artist, album art thumbnail, current time, total duration.
- Controls: **Play/Pause**, **Previous**, **Next**, **Seek** (progress slider), **Volume** slider.
- Playback supports **shuffle** mode (randomizes queue order) and **repeat** modes: Off, Repeat One, Repeat All.
- Every play event (song ID, user ID, timestamp, duration listened) is **logged to the database** when a song is played for more than 10 seconds.

### 5.5 Playlists & Queue

- Users can **create** named playlists and **add songs** to them from the library or album views.
- Songs can be **reordered** within a playlist (drag-and-drop or up/down arrows).
- Songs can be **removed** from a playlist.
- Playlists can be **deleted**.
- A **Play Queue** sidebar shows the upcoming song order and allows manual reordering mid-session (queue is not persisted between sessions).

### 5.6 Listening Stats & Reports

This is the defining feature of VibeVault. The Stats panel surfaces:

| Stat | Description |
|------|-------------|
| Top 5 Most Played Songs | Ranked by total play count |
| Top 5 Most Played Artists | Ranked by total plays across all their songs |
| Total Listening Time | Sum of all logged durations for this user (in hours/minutes) |
| Weekly Activity Chart | Bar chart showing listening minutes per day for the last 7 days |
| Listening by Genre | Pie/bar breakdown of play counts by genre |
| Favourite Album | Album with most plays |
| Longest Listening Session | Single session with highest continuous listening time |
| Recently Played | Last 20 songs played, with timestamps |

Stats are computed by running **aggregate SQL queries** (`GROUP BY`, `COUNT`, `SUM`, `AVG`) on the `play_history` table at runtime — no precomputed caches, demonstrating live database reporting.

---

## 6. Database Design

### 6.1 Entity-Relationship Summary

```
User ──< PlayHistory >── Song >── Album >── Artist
User ──< UserPlaylist ──< PlaylistSong >── Song
Song >── Genre
```

### 6.2 Table Definitions

#### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| user_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| username | TEXT | NOT NULL UNIQUE |
| password_hash | TEXT | NOT NULL |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |

#### `artists`
| Column | Type | Constraints |
|--------|------|-------------|
| artist_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| name | TEXT | NOT NULL UNIQUE |
| bio | TEXT | |

#### `albums`
| Column | Type | Constraints |
|--------|------|-------------|
| album_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| title | TEXT | NOT NULL |
| artist_id | INTEGER | FOREIGN KEY → artists |
| release_year | INTEGER | |
| cover_art_path | TEXT | |

#### `songs`
| Column | Type | Constraints |
|--------|------|-------------|
| song_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| title | TEXT | NOT NULL |
| artist_id | INTEGER | FOREIGN KEY → artists |
| album_id | INTEGER | FOREIGN KEY → albums (nullable) |
| genre | TEXT | |
| duration_seconds | INTEGER | |
| file_path | TEXT | NOT NULL UNIQUE |
| track_number | INTEGER | |
| year | INTEGER | |

#### `user_library`
*(Links users to the songs they've imported — allows per-user libraries)*

| Column | Type | Constraints |
|--------|------|-------------|
| user_id | INTEGER | FOREIGN KEY → users |
| song_id | INTEGER | FOREIGN KEY → songs |
| added_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| PRIMARY KEY | (user_id, song_id) | |

#### `playlists`
| Column | Type | Constraints |
|--------|------|-------------|
| playlist_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| user_id | INTEGER | FOREIGN KEY → users |
| name | TEXT | NOT NULL |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |

#### `playlist_songs`
| Column | Type | Constraints |
|--------|------|-------------|
| playlist_id | INTEGER | FOREIGN KEY → playlists |
| song_id | INTEGER | FOREIGN KEY → songs |
| position | INTEGER | NOT NULL (for ordering) |
| PRIMARY KEY | (playlist_id, song_id) | |

#### `play_history`
*(Core stats table — one row per play event)*

| Column | Type | Constraints |
|--------|------|-------------|
| play_id | INTEGER | PRIMARY KEY AUTOINCREMENT |
| user_id | INTEGER | FOREIGN KEY → users |
| song_id | INTEGER | FOREIGN KEY → songs |
| played_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |
| duration_listened | INTEGER | Seconds actually listened |

### 6.3 Key Queries

```sql
-- Top 5 most played songs for a user
SELECT s.title, a.name AS artist, COUNT(*) AS play_count
FROM play_history ph
JOIN songs s ON ph.song_id = s.song_id
JOIN artists a ON s.artist_id = a.artist_id
WHERE ph.user_id = ?
GROUP BY ph.song_id
ORDER BY play_count DESC
LIMIT 5;

-- Total listening time for a user (in minutes)
SELECT ROUND(SUM(duration_listened) / 60.0, 1) AS total_minutes
FROM play_history
WHERE user_id = ?;

-- Daily listening minutes for last 7 days
SELECT DATE(played_at) AS day, ROUND(SUM(duration_listened)/60.0,1) AS minutes
FROM play_history
WHERE user_id = ? AND played_at >= DATE('now', '-7 days')
GROUP BY DATE(played_at)
ORDER BY day;

-- Top artist by play count
SELECT a.name, COUNT(*) AS plays
FROM play_history ph
JOIN songs s ON ph.song_id = s.song_id
JOIN artists a ON s.artist_id = a.artist_id
WHERE ph.user_id = ?
GROUP BY a.artist_id
ORDER BY plays DESC
LIMIT 5;
```

---

## 7. Application Layer Design

### 7.1 Package Structure

```
com.vibevault/
├── Main.java                    ← App entry point
├── db/
│   └── DatabaseManager.java     ← Connection pool, schema init
├── model/
│   ├── User.java
│   ├── Song.java
│   ├── Artist.java
│   ├── Album.java
│   └── Playlist.java
├── dao/
│   ├── UserDAO.java
│   ├── SongDAO.java
│   ├── ArtistDAO.java
│   ├── AlbumDAO.java
│   ├── PlaylistDAO.java
│   └── PlayHistoryDAO.java
├── service/
│   ├── AuthService.java         ← Login, register, hashing
│   ├── LibraryService.java      ← Import, search, browse
│   ├── PlayerService.java       ← Playback engine, queue
│   ├── PlaylistService.java     ← CRUD for playlists
│   └── StatsService.java        ← All stats queries
└── ui/
    ├── MainWindow.java           ← JFrame shell, nav bar
    ├── LoginPanel.java
    ├── LibraryPanel.java
    ├── BrowsePanel.java          ← Artist/Album view
    ├── PlaylistPanel.java
    ├── StatsPanel.java
    ├── NowPlayingBar.java        ← Persistent bottom bar
    └── components/
        ├── SongTable.java
        ├── AlbumCard.java
        └── StatChart.java        ← Simple bar/pie charts using Java2D
```

### 7.2 Player Service Design

The `PlayerService` will run audio playback on a **dedicated background thread** (not the Swing EDT) to prevent UI freezing. It will:

1. Maintain a **queue** (`LinkedList<Song>`) representing upcoming songs.
2. Use JLayer's `Player` class for MP3 decoding and playback.
3. Fire **property change events** (via `PropertyChangeSupport`) so the `NowPlayingBar` can update its progress slider without polling.
4. Log to `play_history` via `PlayHistoryDAO` when a song has played for ≥ 10 seconds.

### 7.3 Stats Chart Rendering

Rather than including a heavy charting library, the `StatChart` component will draw bar and pie charts using **Java2D Graphics2D** directly on a `JPanel`. This keeps dependencies minimal and demonstrates Java graphics concepts.

---

## 8. UI/UX Design Plan

### 8.1 Main Layout

```
┌──────────────────────────────────────────────────────┐
│  🎵 VibeVault          [User: Hamza ▼]  [Logout]     │  ← Top bar
├──────────────┬───────────────────────────────────────┤
│              │                                        │
│  NAV SIDEBAR │          MAIN CONTENT AREA             │
│  ─────────── │                                        │
│  📚 Library  │   (Switches based on nav selection)    │
│  🎨 Browse   │                                        │
│  🎵 Playlists│                                        │
│  📊 Stats    │                                        │
│              │                                        │
├──────────────┴───────────────────────────────────────┤
│  NOW PLAYING BAR                                      │  ← Always visible
│  [Album Art] Song Title - Artist    ⏮ ⏸ ⏭  🔀 🔁    │
│              ────────────●───────────  🔊────────     │
└──────────────────────────────────────────────────────┘
```

### 8.2 Screen Breakdown

| Screen | Key UI Elements |
|--------|----------------|
| Login / Register | Username + Password fields, Login/Register buttons, error labels |
| Library | Searchable `JTable` of all songs, Add Songs button, right-click context menu (Add to playlist, Remove) |
| Browse | Artist list on left, album grid in center, song list on right |
| Playlists | Playlist list on left, song list on right with reorder controls |
| Stats | Cards showing key numbers at top, bar chart (weekly), top-5 lists |

### 8.3 Visual Style

- **Color scheme:** Dark theme (charcoal background, white text, purple/teal accent)
- **Font:** Segoe UI or SansSerif fallback
- Custom `UIManager` overrides to give Swing components a modern dark look
- Album art displayed wherever possible (extracted from MP3 or default placeholder)

---

## 9. Project Phases & Milestones

### Phase 1 — Foundation (Week 1–2)
- [ ] Set up Maven project, GitHub repo, project folder structure
- [ ] Implement `DatabaseManager` with SQLite connection and schema auto-creation
- [ ] Implement all DAOs with basic CRUD
- [ ] Unit test DAOs against an in-memory SQLite database

### Phase 2 — Auth & Library (Week 3)
- [ ] Build `AuthService` with SHA-256 hashing
- [ ] Build Login/Register UI (`LoginPanel`)
- [ ] Build `LibraryService` with file import and metadata extraction
- [ ] Build `LibraryPanel` with song table and search

### Phase 3 — Browse & Playback (Week 4–5)
- [ ] Build `BrowsePanel` (Artist → Album → Song hierarchy)
- [ ] Implement `PlayerService` with JLayer, background thread, queue
- [ ] Build `NowPlayingBar` with controls and progress tracking
- [ ] Wire up play event logging to `play_history`

### Phase 4 — Playlists & Queue (Week 6)
- [ ] Build `PlaylistService` and `PlaylistPanel`
- [ ] Implement queue management and shuffle/repeat modes

### Phase 5 — Stats & Polish (Week 7–8)
- [ ] Build `StatsService` with all aggregate queries
- [ ] Build `StatsPanel` with charts and stat cards
- [ ] Apply dark theme styling across all panels
- [ ] End-to-end testing, bug fixes, performance checks

### Phase 6 — Final Deliverable (Week 9)
- [ ] Code cleanup, documentation (Javadoc)
- [ ] Final demo video or live walkthrough
- [ ] Project report submission

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Audio playback format compatibility | Medium | High | Stick to MP3 for MVP; add WAV support later |
| ID3 tag metadata missing/malformed | High | Medium | Graceful fallback to filename as title; allow manual edit |
| SQLite concurrent write from UI thread | Medium | High | Enforce single-threaded DB writes; player thread queues log calls |
| Large music libraries causing slow UI | Low | Medium | Paginate `JTable` results; load metadata lazily |
| Swing look-and-feel inconsistency across OS | Medium | Low | Test on Windows primarily; use `UIManager.setLookAndFeel` |

---

## 11. Appendix: SQL Schema

Complete `schema.sql` for reference and schema initialization:

```sql
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS artists (
    artist_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT    NOT NULL UNIQUE,
    bio       TEXT
);

CREATE TABLE IF NOT EXISTS albums (
    album_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    title          TEXT    NOT NULL,
    artist_id      INTEGER NOT NULL,
    release_year   INTEGER,
    cover_art_path TEXT,
    FOREIGN KEY (artist_id) REFERENCES artists(artist_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS songs (
    song_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title            TEXT    NOT NULL,
    artist_id        INTEGER NOT NULL,
    album_id         INTEGER,
    genre            TEXT,
    duration_seconds INTEGER,
    file_path        TEXT    NOT NULL UNIQUE,
    track_number     INTEGER,
    year             INTEGER,
    FOREIGN KEY (artist_id) REFERENCES artists(artist_id),
    FOREIGN KEY (album_id)  REFERENCES albums(album_id)
);

CREATE TABLE IF NOT EXISTS user_library (
    user_id  INTEGER NOT NULL,
    song_id  INTEGER NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(song_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlists (
    playlist_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    name        TEXT    NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlist_songs (
    playlist_id INTEGER NOT NULL,
    song_id     INTEGER NOT NULL,
    position    INTEGER NOT NULL,
    PRIMARY KEY (playlist_id, song_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists(playlist_id) ON DELETE CASCADE,
    FOREIGN KEY (song_id)     REFERENCES songs(song_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS play_history (
    play_id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER NOT NULL,
    song_id           INTEGER NOT NULL,
    played_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    duration_listened INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(song_id) ON DELETE CASCADE
);

-- Indexes for stats query performance
CREATE INDEX IF NOT EXISTS idx_play_history_user    ON play_history(user_id);
CREATE INDEX IF NOT EXISTS idx_play_history_song    ON play_history(song_id);
CREATE INDEX IF NOT EXISTS idx_play_history_date    ON play_history(played_at);
CREATE INDEX IF NOT EXISTS idx_songs_artist         ON songs(artist_id);
CREATE INDEX IF NOT EXISTS idx_songs_album          ON songs(album_id);
```

---

*Document maintained in Git alongside the source code. Update version and date with each significant design change.*
