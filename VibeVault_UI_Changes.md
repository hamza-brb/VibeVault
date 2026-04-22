# VibeVault — UI & DB Change Plan

---

## 1. Remove Album & Genre from the Database Schema

**Decision:** Strip these columns and tables from the schema entirely.

### 1a. `schema.sql`
- Remove the `albums` table definition.
- Remove the `genre TEXT` column from the `songs` table.
- Remove the `album_id INTEGER` column from the `songs` table.
- Remove the foreign key `FOREIGN KEY (album_id) REFERENCES albums(album_id)`.
- Remove the index `idx_songs_album`.

### 1b. `Song.java` (model)
- Remove fields: `albumId`, `genre`, `trackNumber`, `year`.
- Remove their getters and setters.
- Update the constructor accordingly.

### 1c. `AlbumDAO.java`
- Delete this file entirely — it is no longer needed.

### 1d. `SongDAO.java`
- Remove all references to `album_id` and `genre` in INSERT/SELECT/UPDATE queries.

### 1e. `LibraryService.java`
- Remove `AlbumLibrarySummary` record and `getAlbumBrowseSummaries()` method.
- Remove `getSongsByAlbumInUserLibrary()` method.
- Remove `albumId` from `SongImportRequest`.

### 1f. `VibeVaultFrame.java`
- Remove `albumTitleCache` map and `lookupAlbumTitle()` method.
- Remove `albumField` text field and all references to it.
- Remove `AlbumBrowseCard` inner class.
- Remove the "Albums" toggle button and `showBrowseAlbums()` / `openAlbumBrowseDetail()` methods from the Browse panel.
- Remove `browseDetailAlbumsPanel` and the album strip `JSplitPane` from the artist detail view — replace with a plain `JScrollPane` for the songs table only.
- Remove `Album` and `Genre` columns from `libraryTableModel` (keep: `#` hidden, `Title`, `Artist`, `Duration`).
- Remove `Album` and `Genre` columns from `browseSongsTableModel` (keep: `#`, `Title`, `Artist`, `Duration`).
- Remove `Album` column from the "Add Songs to Playlist" dialog table.
- Update `applyLibrarySearchFilter()` — currently filters columns 1–5; adjust indices to match the reduced column set (Title = col 1, Artist = col 2, Duration = col 3).
- Remove calls to `getAlbumBrowseSummaries()` inside `refreshLibraryAndStats()`.
- Remove `favoriteAlbum` stat and its display from `summaryLabel` text.

### 1g. `StatsService.java`
- Remove `getFavoriteAlbum()` method and `AlbumPlayStat` record.

---

## 2. Fix the Search Bar (Global Top Bar)

**Problem:** The global search field in the top bar is built but has no `DocumentListener` wired up — it is purely decorative.

**Decision:** Wire it to search across Songs, Artists, and Playlists simultaneously.

### `VibeVaultFrame.java` — `buildTopBar()`

- Store `globalSearch` as a class field (e.g., `globalSearchField`) so it is accessible outside the builder method.
- Add a `DocumentListener` to `globalSearchField`:
  - On any change, read the query via `readTextInput(globalSearchField)`.
  - If the query is blank, clear all filters and return.
  - If the query is non-blank:
    1. **Songs** — apply `RowFilter.regexFilter("(?i)" + Pattern.quote(query), 1, 2)` to `librarySorter` (Title = col 1, Artist = col 2 after album/genre removal). Switch the content panel to `CONTENT_LIBRARY`.
    2. **Artists** — call `refreshBrowseGrid()` after filtering `getArtistBrowseSummaries()` to only those whose `artistName` contains the query (case-insensitive). Results show in the Browse grid.
    3. **Playlists** — filter `playlistScreenListModel` rows to those whose name contains the query.
  - Because switching panels on every keystroke would be disruptive, instead show results in the **current active panel** and indicate match counts in the other sections (e.g., update a small badge or the section label: "Artists (2 matches)").
  - Alternatively (simpler approach): the global search switches the view to a unified "Search Results" card that lists matching songs, artists, and playlists in three labelled groups within one scroll pane.

> **Recommended simple approach:** Add a new `CONTENT_SEARCH` card to `contentCardPanel`. When `globalSearchField` has text, switch to this card and populate three sub-tables (Songs matches, Artist matches, Playlist matches). When the field is cleared, return to the previously active card.

### Fix the In-Panel Library Search (`librarySearchField`)

**Problem:** The search filter runs on the placeholder text too (since `readTextInput` is not called — it directly reads `getText()`).

- In `applyLibrarySearchFilter()`, replace `librarySearchField.getText()` with `readTextInput(librarySearchField)` so that placeholder text ("Search your library...") is not treated as a real query.
- After removing Album/Genre, update the `RowFilter` column indices: filter columns 1 (Title) and 2 (Artist) only.

---

## 3. Artist Cards — Smaller Square Tiles

**Problem:** Cards are currently `160×120px` rectangular buttons in a `GridLayout(0, 4, 12, 12)`, making them too large and visually heavy.

### `VibeVaultFrame.java` — `refreshBrowseGrid()`

- Change card `setPreferredSize` from `new Dimension(160, 120)` to `new Dimension(100, 100)`.
- Change the `GridLayout` from `new GridLayout(0, 4, 12, 12)` to `new GridLayout(0, 5, 10, 10)` to fit 5–6 per row.
- Reduce the font size of the card label HTML: change `<b>` artist name to a smaller font (use `<font size='3'>` or reduce the button's own font via `card.setFont(Theme.body(11f))`).
- Consider making cards circular or more compact by switching from `RoundedButton` to a custom square `RoundedPanel` with a centred `JLabel` — this removes the button border chrome and looks cleaner at small sizes.

---

## 4. Fix the Playback Bar — Centre-Lock the Controls

**Problem:** The now-playing bar uses `BorderLayout`. The seek slider and time labels are centred (`BorderLayout.CENTER`), but when the window is narrow or the left/right panels are wide, the centre panel gets squeezed and the controls drift off-centre visually.

### `VibeVaultFrame.java` — `buildNowPlayingBar()`

Replace the outer `BorderLayout(18, 0)` approach with a three-column layout where the centre is explicitly forced to a fixed width and anchored to the middle of the screen:

```
Option A — GridLayout(1, 3):
  - Column 1 (left): now-playing avatar + song meta
  - Column 2 (centre): seek row + playback buttons  ← fixed centre
  - Column 3 (right): volume + summary label

Change:
  bar.setLayout(new GridLayout(1, 3, 0, 0));
  bar.add(leftPanel);
  bar.add(centerPanel);
  bar.add(rightControls);
```

- This guarantees that `centerPanel` always occupies exactly one-third of the bar width and is visually centred regardless of window size.
- Remove the `BorderLayout.WEST / CENTER / EAST` `add()` calls and replace with sequential `add()`.
- Set `centerPanel.setAlignmentX(Component.CENTER_ALIGNMENT)` and ensure `centerControls` uses `FlowLayout.CENTER`.

---

## 5. Window Scalability / Minimum Size

**Problem:** When the window is resized smaller than ~1100px wide, the top bar and now-playing bar overflow or compress badly. The sidebar squeezes content. Stats cards truncate.

### `VibeVaultFrame.java` — constructor

- Increase minimum window size from `new Dimension(1000, 650)` to `new Dimension(1100, 680)`.

### Top Bar (`buildTopBar()`)
- Wrap the global search field's preferred width as a proportion: instead of `setPreferredSize(new Dimension(340, 36))`, use `setMaximumSize(new Dimension(500, 36))` and `setMinimumSize(new Dimension(160, 36))` so it shrinks gracefully.
- The brand label + search + buttons row should remain in `BorderLayout` — this already scales. No change needed here.

### Sidebar (`buildSidebar()`)
- Change `setPreferredSize(new Dimension(240, 0))` to `new Dimension(200, 0)` — 240px is wide for icon+text sidebar buttons, 200px is sufficient and reclaims space.

### Now-Playing Bar
- After switching to `GridLayout(1, 3)` (see §4), scalability is resolved for the bottom bar.
- Ensure `bar.setPreferredSize(new Dimension(0, 104))` — the `0` width lets the layout manager handle horizontal sizing correctly. Keep this as-is.

### Stats Screen (`buildStatsScreenPanel()`)
- The stats cards panel uses `setPreferredSize(new Dimension(0, 80))` which is fine.
- Wrap the entire stats content in the existing `JScrollPane` (already done via `mainScroll`) — confirm `mainScroll` is added to `BorderLayout.CENTER` of the panel (it is). No change needed.

---

## 6. Summary of Files to Change

| File | Changes |
|---|---|
| `schema.sql` | Remove `albums` table, `album_id`, `genre` from `songs` |
| `Song.java` | Remove `albumId`, `genre`, `trackNumber`, `year` fields |
| `AlbumDAO.java` | **Delete** |
| `SongDAO.java` | Remove album/genre from queries |
| `LibraryService.java` | Remove album methods and import fields |
| `StatsService.java` | Remove `getFavoriteAlbum()` |
| `VibeVaultFrame.java` | All UI changes above (search, cards, layout, scalability) |

---

## 7. Testing Checklist

- [ ] App launches and logs in without errors after schema change (may need to delete `vibevault.db` and re-import songs).
- [ ] Songs table shows only Title, Artist, Duration.
- [ ] In-panel library search filters correctly by title and artist; does not trigger on placeholder text.
- [ ] Global search bar filters songs, artists, and playlists simultaneously.
- [ ] Artist grid shows ~5–6 cards per row at default window size.
- [ ] Playback controls remain visually centred at window widths from 1100px to 1800px.
- [ ] Seek slider and time labels are not clipped at narrow widths.
- [ ] Window resizing does not cause layout overflow in the top bar or now-playing bar.
- [ ] Browse panel no longer shows "Albums" toggle button.
- [ ] Importing an MP3 still works without album/genre fields.
