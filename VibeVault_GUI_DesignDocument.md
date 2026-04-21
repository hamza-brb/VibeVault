# VibeVault — GUI Design Document

**Project:** VibeVault Desktop Music Player  
**Document Type:** GUI & Visual Design Specification  
**Version:** 1.0  
**Date:** March 29, 2026  
**Authors:** Muhammad Hamza Hashim (571509), Muhammad Shehryar Azhar (570992)

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Component Design Language](#4-component-design-language)
5. [Application Layout](#5-application-layout)
6. [Screen-by-Screen Specification](#6-screen-by-screen-specification)
   - 6.1 Login & Register Screen
   - 6.2 Main Window Shell
   - 6.3 Left Sidebar
   - 6.4 Library Screen
   - 6.5 Browse Screen (Artists & Albums)
   - 6.6 Playlist Screen
   - 6.7 Stats Screen
   - 6.8 Now Playing Bar (Bottom)
   - 6.9 User Panel (Top Right)
7. [Interaction & Animation Guidelines](#7-interaction--animation-guidelines)
8. [Java Swing Implementation Notes](#8-java-swing-implementation-notes)

---

## 1. Design Philosophy

VibeVault's interface is guided by three principles:

- **Calm focus.** Deep, cool navy tones keep the eye relaxed for long listening sessions. Nothing competes with the music.
- **Minimalism.** Only what is needed is shown. No clutter, no unnecessary borders, no decorative elements. Whitespace is used intentionally.
- **Clarity over cleverness.** Every control is immediately recognizable. Labels accompany icons. Hierarchy is expressed through size and opacity, not color noise.

The closest reference point is Spotify's desktop client — a persistent sidebar, a dominant content area, and a locked playback bar at the bottom — adapted with VibeVault's own Deep Sea palette.

---

## 2. Color System

All colors are derived directly from the **Deep Sea** palette.

| Role | Name | Hex | Usage |
|------|------|-----|-------|
| Background (deepest) | Abyss | `#0D1321` | Main window background, sidebar base |
| Background (mid) | Deep Navy | `#1A2A3A` | Content panels, card backgrounds |
| Background (surface) | Steel Blue | `#2E5077` | Hover states, selected rows, active nav items |
| Accent / Interactive | Muted Blue | `#6B8FA8` | Buttons, progress bars, icons, highlights |
| Text Primary | Cream White | `#EDE8D0` | Headings, song titles, primary labels |
| Text Secondary | Faded Cream | `#EDE8D0` at 55% opacity | Subtitles, artist names, timestamps |
| Text Disabled | — | `#EDE8D0` at 25% opacity | Inactive items, placeholder text |
| Divider | — | `#FFFFFF` at 6% opacity | Subtle separators, panel borders |

### Accent Usage Rule

`#6B8FA8` (Muted Blue) is the **only** interactive color in the app. Every clickable element — buttons, links, the progress bar fill, playlist icons on hover — uses this single accent. This keeps the UI visually cohesive and prevents color overload.

### Do Not Use

- Pure white (`#FFFFFF`) anywhere — always use Cream White (`#EDE8D0`)
- Any warm or saturated color (red, orange, yellow) unless for an error/destructive action
- Black (`#000000`) — use `#0D1321` instead

---

## 3. Typography

| Level | Usage | Font | Size | Weight | Opacity |
|-------|-------|------|------|--------|---------|
| Title | Screen headings (Library, Browse, etc.) | Segoe UI | 20px | Bold | 100% |
| Section Header | Playlist group names, table column headers | Segoe UI | 13px | SemiBold | 70% |
| Body | Song titles, artist names in tables | Segoe UI | 14px | Regular | 100% |
| Caption | Duration, timestamps, genre tags | Segoe UI | 12px | Regular | 55% |
| Micro | Sidebar playlist names | Segoe UI | 13px | Regular | 80% |

**Fallback fonts:** SansSerif (Java default), then Dialog.

All text is left-aligned unless it is a single centered label (e.g., login screen title). Right-aligned text is used only for duration columns in song tables.

---

## 4. Component Design Language

### 4.1 Buttons

All buttons use **rounded corners** (`arc = 20px` for standard, `arc = 999px` for pill/icon buttons).

| Button Type | Background | Text/Icon Color | Border | Usage |
|-------------|-----------|-----------------|--------|-------|
| Primary | `#2E5077` | `#EDE8D0` | None | Login, Create Playlist |
| Ghost | Transparent | `#EDE8D0` at 80% | 1px `#6B8FA8` | Secondary actions |
| Icon Round | `#1A2A3A` | `#EDE8D0` | None | Play, Pause, Skip, Shuffle |
| Destructive | Transparent | `#EDE8D0` at 60% | None | Remove, Delete (subtle) |

**Hover state:** Lighten background by 10% (mix with white at 10% alpha).  
**Pressed state:** Darken background by 10%.  
**Disabled state:** 30% opacity on the whole button.

### 4.2 Input Fields

- Background: `#1A2A3A`
- Border: 1px `#2E5077`, rounded `arc = 10px`
- Focus border: 1px `#6B8FA8`
- Text: `#EDE8D0`
- Placeholder text: `#EDE8D0` at 30% opacity
- Padding: 8px vertical, 12px horizontal

### 4.3 Tables (Song Lists)

- Row height: 48px
- Alternating row backgrounds: `#0D1321` / `#1A2A3A` (very subtle)
- Selected row: `#2E5077` with no border
- Hover row: `#1A2A3A` lightened slightly
- No grid lines — rows are separated by 1px transparent gap only
- Currently playing row: accent dot (`#6B8FA8`) in the leftmost column instead of track number

### 4.4 Scrollbars

- Track: transparent
- Thumb: `#2E5077`, rounded, 4px wide
- Thumb hover: `#6B8FA8`
- No scrollbar arrows (hidden)

### 4.5 Cards (Album / Artist Cards)

- Size: 160px × 190px
- Background: `#1A2A3A`
- Corner radius: 10px
- Image area: 160×160px square at top (or circle for artist cards), with a default placeholder icon if no art
- Label below image: song/album title in Body, artist name in Caption
- Hover: subtle lift effect (border `1px #6B8FA8`, background lightens slightly)

### 4.6 Dividers

- 1px horizontal line, color `#FFFFFF` at 6% opacity
- Used sparingly — only between major layout zones (sidebar top/bottom, above Now Playing Bar)

---

## 5. Application Layout

The main window is divided into **four fixed zones**:

```
┌─────────────────────────────────────────────────────────────────┐
│  TOP BAR          [Search]            [Stats]  [👤 Username  ▾] │  48px
├───────────────┬─────────────────────────────────────────────────┤
│               │                                                  │
│  LEFT SIDEBAR │              MAIN CONTENT AREA                   │
│   240px wide  │              (fills remaining width)             │
│               │                                                  │
│  • Library    │   Switches between:                              │
│    nav items  │   - Library view                                 │
│               │   - Browse view (Artists / Albums)               │
│  • Playlists  │   - Playlist detail view                         │
│    list       │   - Stats view                                   │
│               │                                                  │
│  + New        │                                                  │
│  Playlist btn │                                                  │
│               │                                                  │
├───────────────┴─────────────────────────────────────────────────┤
│  NOW PLAYING BAR                                                 │  90px
│  [Art][Title - Artist]   ⏮  ⏸  ⏭  🔀  🔁   ──●──────  🔊───  │
└─────────────────────────────────────────────────────────────────┘
```

**Window minimum size:** 1000px × 650px  
**Recommended default:** 1200px × 750px  
**Resizable:** Yes — content area stretches; sidebar and bottom bar are fixed width/height.

---

## 6. Screen-by-Screen Specification

---

### 6.1 Login & Register Screen

**Layout:** Centered card on a full `#0D1321` background.

```
┌──────────────────────────────────┐
│                                  │
│         🎵  VibeVault            │  ← App name, 28px Bold, Cream
│                                  │
│   ┌──────────────────────────┐   │
│   │  Username                │   │  ← Input field
│   └──────────────────────────┘   │
│   ┌──────────────────────────┐   │
│   │  Password                │   │  ← Input field (masked)
│   └──────────────────────────┘   │
│                                  │
│   [ ────────  Login  ──────── ]  │  ← Primary button, full width
│                                  │
│   Don't have an account?         │
│   [ ───────  Register  ────── ]  │  ← Ghost button, full width
│                                  │
│   error message appears here     │  ← Red-tinted caption, hidden by default
└──────────────────────────────────┘
```

- Card size: 360px × 380px
- Card background: `#1A2A3A`, corner radius 16px
- Card has a very subtle shadow (2px blur, 10% black) to lift it off the background
- App name has a small music note icon (Unicode ♫ or a simple SVG) to the left
- No logo image required — wordmark only keeps it clean
- Register toggles the same card to show a "Confirm Password" field; no separate screen

---

### 6.2 Main Window Shell

After login, the full window appears. The shell is a `JFrame` with:

- Title bar: `VibeVault` (OS native title bar is acceptable; custom title bar is optional)
- Background: `#0D1321` applied to the root `JPanel`
- Three child zones laid out with `BorderLayout`: `WEST` (sidebar), `CENTER` (content), `SOUTH` (now playing bar)
- A `JPanel` at `NORTH` (top bar) spans full width above sidebar + content

---

### 6.3 Left Sidebar

**Width:** 240px fixed. Background: `#0D1321`.  
A thin 1px divider on the right edge separates it from the content area.

```
┌──────────────────────┐
│  LIBRARY             │  ← Section label, 11px uppercase, 50% opacity
│                      │
│  ♫  Home             │  ← Nav item
│  ☰  Songs            │  ← Nav item
│  👤 Artists          │  ← Nav item
│  💿 Albums           │  ← Nav item
│                      │
│  ─────────────────── │  ← Divider
│                      │
│  PLAYLISTS           │  ← Section label
│                      │
│  + New Playlist      │  ← Ghost button, small, full sidebar width
│                      │
│  ♪  Chill Vibes      │  ← Playlist item
│  ♪  Late Night       │  ← Playlist item
│  ♪  Workout Mix      │  ← Playlist item
│  ♪  ...              │
│  (scrollable)        │
│                      │
└──────────────────────┘
```

**Nav Items:**
- Height: 40px, left padding 20px, icon + label with 10px gap
- Inactive: text at 70% opacity, no background
- Hover: background `#1A2A3A`, text 100% opacity, smooth transition
- Active/Selected: background `#2E5077`, text 100% opacity, left accent bar (3px wide, `#6B8FA8`)

**Playlist Items:**
- Same styling as nav items but with a music note icon
- Right-click context menu: **Rename**, **Delete** (shown as a small `JPopupMenu` styled dark)
- The list is a `JScrollPane`-wrapped `JList` with custom cell renderer
- Max visible items before scroll: ~10

**"+ New Playlist" button:**
- Ghost style, 32px height, left-aligned with icon
- Opens a small inline text field directly below the button to type playlist name, confirmed with Enter

---

### 6.4 Library Screen (Songs View)

Activated when user clicks **Songs** in the sidebar. This is the default landing screen after login.

```
┌─────────────────────────────────────────────────────┐
│  Songs                    [ 🔍 Search songs...    ] │  ← Title + search right-aligned
│  324 songs                                          │  ← Subtitle, caption style
├──────┬──────────────────────┬───────────┬───────────┤
│  #   │  Title               │  Artist   │  Duration │  ← Column headers
├──────┼──────────────────────┼───────────┼───────────┤
│  ●   │  Midnight City       │  M83      │  4:03     │  ← Currently playing row (● accent dot)
│  2   │  Blinding Lights     │  Weeknd   │  3:20     │
│  3   │  Levitating          │  Dua Lipa │  3:23     │
│ ...  │  ...                 │  ...      │  ...      │
└──────┴──────────────────────┴───────────┴───────────┘
```

- Column widths: `#` (40px), Title (flexible, ~50%), Artist (25%), Duration (80px right-aligned)
- Album art thumbnail (32×32px, rounded 4px) shown to the left of the Title column — optional but adds polish
- Clicking a row starts playback of that song and loads the full library as the queue
- Double-click = play immediately
- Right-click context menu on a row: **Play**, **Add to Playlist ▶** (submenu listing playlists), **Remove from Library**
- Search bar filters the table in real time as the user types (no button press needed)
- Clicking column headers sorts the table by that column (toggle asc/desc)

---

### 6.5 Browse Screen (Artists & Albums)

Activated by clicking **Artists** or **Albums** in the sidebar.

**Artists View:**

```
┌─────────────────────────────────────────────────────┐
│  Artists                                            │
├──────────────────────────────────────────────────────┤
│                                                      │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐    │
│  │  (art) │  │  (art) │  │  (art) │  │  (art) │    │  ← Circle artist avatar
│  │  M83   │  │ Weeknd │  │D. Lipa │  │ Hozier │    │
│  │12 songs│  │8 songs │  │6 songs │  │9 songs │    │
│  └────────┘  └────────┘  └────────┘  └────────┘    │
│                                                      │
│  ┌────────┐  ┌────────┐  ...                        │
└──┴────────┴──┴────────┴────────────────────────────┘
```

- Grid layout, 4 columns, wraps on resize
- Artist avatar: 120px circle, placeholder icon if no image
- Clicking an artist card opens a **detail view** replacing the grid:

```
  ┌─────────────────────────────────────────────────────┐
  │  ← Back to Artists                                  │
  │                                                     │
  │  (avatar)  M83                                      │
  │            12 songs · 3 albums                      │
  │                                                     │
  │  Albums ──────────────────────────────────          │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
  │  │  (cover) │  │  (cover) │  │  (cover) │          │
  │  │ Hurry Up │  │  Before  │  │  Saturns │          │
  │  └──────────┘  └──────────┘  └──────────┘          │
  │                                                     │
  │  All Songs ──────────────────────────────────       │
  │  (song table same as Library view)                  │
  └─────────────────────────────────────────────────────┘
```

**Albums View:**
- Same card grid as artists but rectangular cards (see §4.5)
- Clicking an album shows its track listing in a song table

---

### 6.6 Playlist Screen

Activated when user clicks any playlist name in the sidebar.

```
┌─────────────────────────────────────────────────────┐
│  ┌──────────┐   Chill Vibes                         │
│  │  (cover  │   18 songs · 1h 12m                   │  ← Playlist header
│  │  mosaic) │   [ ▶ Play ]  [ ✏ Rename ]  [ 🗑 ]   │
│  └──────────┘                                       │
├─────────────────────────────────────────────────────┤
│  #   Title               Artist         Duration    │
├─────────────────────────────────────────────────────┤
│  ⠿  1   Midnight City    M83            4:03        │  ← ⠿ = drag handle
│  ⠿  2   Blinding Lights  The Weeknd     3:20        │
│  ⠿  3   ...              ...            ...         │
└─────────────────────────────────────────────────────┘
```

- **Cover mosaic:** A 2×2 grid of the first 4 songs' album art stitched into a 120×120px square. If fewer than 4 songs, use a single art or default icon.
- **Drag handle (⠿):** appears on row hover, allows drag-and-drop reordering
- **Play button:** Loads the playlist as the queue and starts from the first song
- **Rename:** Inline edit — the title text becomes an input field in place
- **Delete (🗑):** Confirmation dialog before deleting — `"Delete playlist 'Chill Vibes'? This cannot be undone."` — styled as a dark `JDialog` with two rounded buttons (Cancel / Delete)
- Right-click on any row: **Remove from Playlist**, **Add to another Playlist ▶**

---

### 6.7 Stats Screen

Activated by clicking **Stats** in the top bar. Replaces the entire main content area.

```
┌─────────────────────────────────────────────────────────────────┐
│  Your Stats                                  This Week ▾        │  ← Title + time filter dropdown
├──────────────────────┬──────────────────────┬───────────────────┤
│  🎵 Total Plays      │  ⏱ Listening Time    │  🎤 Top Artist    │
│     1,240            │     48h 32m           │     M83           │  ← Stat cards row
├──────────────────────┴──────────────────────┴───────────────────┤
│                                                                  │
│  Weekly Activity                                                 │  ← Bar chart
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  █                                                        │   │
│  │  █       █                   █                           │   │
│  │  █   █   █       █   █   █   █                           │   │
│  │  Mon Tue Wed     Thu Fri Sat Sun                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
├──────────────────────────────┬───────────────────────────────────┤
│  Top 5 Songs                 │  Top 5 Artists                    │
│  1. Midnight City  — 87 ▶    │  1. M83           — 142 ▶        │
│  2. Blinding Lights — 74 ▶   │  2. The Weeknd    — 98 ▶         │
│  3. Levitating    — 61 ▶     │  3. Dua Lipa      — 76 ▶         │
│  4. As It Was     — 55 ▶     │  4. Hozier        — 64 ▶         │
│  5. Cruel Summer  — 49 ▶     │  5. Arctic Monkeys — 51 ▶        │
├──────────────────────────────┴───────────────────────────────────┤
│  Recently Played                                                  │
│  (last 10 songs in a compact table: art · title · artist · time) │
└──────────────────────────────────────────────────────────────────┘
```

**Stat Cards:**
- Background: `#1A2A3A`, corner radius 12px, padding 20px
- Icon in `#6B8FA8`, large number in 28px Bold Cream, label in Caption style below

**Bar Chart:**
- Drawn with Java2D `Graphics2D` on a custom `JPanel`
- Bar fill: `#2E5077`, hovered bar: `#6B8FA8`
- Y-axis label: minutes listened, X-axis: day names
- No heavy charting library needed

**Top 5 Lists:**
- Numbered list, each row 36px tall
- Play count shown right-aligned in Caption style
- Clicking a row opens that song/artist in the Library/Browse view

**Time Filter Dropdown:**
- Options: This Week, This Month, All Time
- Styled as a rounded ghost button that opens a small `JPopupMenu`

---

### 6.8 Now Playing Bar (Bottom)

Fixed 90px bar. Background: `#0D1321` with a 1px top divider. Always visible.

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌────┐  Midnight City                 ⏮   ⏸   ⏭              │
│  │art │  M83 · Hurry Up, We're Dreaming  🔀        🔁           │
│  └────┘  ──────────────●──────────────    ───●──────  🔊        │
│          0:47                      4:03   (seek bar) (vol bar)  │
└──────────────────────────────────────────────────────────────────┘
```

**Left section (song info) — 25% width:**
- Album art thumbnail: 52×52px, corner radius 6px
- Song title: 14px, Cream, single line with ellipsis overflow
- Artist · Album: 12px, 55% opacity
- Small ♡ (favourite) icon after song title — toggles heart fill on click (no favourite feature needed in MVP; can be a visual-only placeholder)

**Center section (controls) — 50% width:**
- All controls centered horizontally
- Top row: **⏮ Previous**, **⏸/▶ Play/Pause** (larger, 42px icon button), **⏭ Next**
- Below transport: **🔀 Shuffle** toggle + seek bar + **🔁 Repeat** toggle
- Seek bar: custom `JSlider`, height 4px, filled portion `#6B8FA8`, thumb 12px circle, same color
- Timestamps (elapsed / total) on left and right of seek bar in Caption style

**Right section (volume) — 25% width:**
- 🔊 icon + volume slider (same style as seek bar but shorter, ~100px)
- Right-aligned within its section

**Play/Pause button** is the visual hero of this bar — slightly larger than skip buttons, uses a filled circle background (`#2E5077`) with the icon inside.

---

### 6.9 Top Bar & User Panel

**Height:** 48px. Background: `#0D1321`. Spans full window width.

```
┌────────────────────────────────────────────────────────────────┐
│  [← →]   [ 🔍  Search your library...          ]   [📊 Stats] [👤 Hamza  ▾] │
└────────────────────────────────────────────────────────────────┘
```

**Left:** Back / Forward navigation arrows (navigate between content views like a browser).

**Center:** Global search bar — 400px wide max, rounded pill shape, searches songs/artists/albums simultaneously and shows a dropdown of results below (max 8 results, grouped by type).

**Right:**
- **Stats button** — ghost button, activates the Stats screen
- **User button** — shows avatar initial (first letter of username in a `#2E5077` circle) + username + dropdown arrow

**User dropdown menu** (appears on click, `JPopupMenu`):
```
  ┌────────────────────┐
  │  👤  Hamza          │  ← Username header (not clickable)
  │  ────────────────  │
  │  ⚙   Preferences   │
  │  🚪  Log Out        │
  └────────────────────┘
```
Styled with `#1A2A3A` background, `#EDE8D0` text, hover rows use `#2E5077`.

---

## 7. Interaction & Animation Guidelines

Swing does not animate natively, but these small behaviors make the UI feel polished without complex code:

| Interaction | Behavior | Implementation |
|-------------|----------|----------------|
| Button hover | Background color lightens | Override `paintComponent`, check `mouseEntered` |
| Row hover in table | Background shifts to `#1A2A3A` | Custom `TableCellRenderer` |
| Sidebar item click | Smooth content swap | `CardLayout` on the main content panel |
| Seek bar drag | Timestamp updates live while dragging | `ChangeListener` on `JSlider` |
| Search input | Table filters on each keystroke | `DocumentListener` on `JTextField` |
| Playlist delete | Confirm dialog fades in | Standard `JDialog`, no animation needed |
| Now playing highlight | Currently playing row gets `●` dot | Renderer checks against `PlayerService.currentSong` |

**Transitions between content screens** are instant (no fade) — `CardLayout.show()` is sufficient and keeps the app feeling snappy.

---

## 8. Java Swing Implementation Notes

### 8.1 Applying the Dark Theme

Swing's default Look & Feel is light. Override it globally at startup:

```java
// In Main.java before creating any UI components
UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

// Then override individual component defaults
UIManager.put("Panel.background", new Color(0x0D1321));
UIManager.put("Table.background", new Color(0x0D1321));
UIManager.put("Table.alternateRowColor", new Color(0x1A2A3A));
UIManager.put("Table.selectionBackground", new Color(0x2E5077));
UIManager.put("Table.foreground", new Color(0xEDE8D0));
UIManager.put("Table.selectionForeground", new Color(0xEDE8D0));
UIManager.put("ScrollPane.background", new Color(0x0D1321));
UIManager.put("List.background", new Color(0x0D1321));
UIManager.put("List.selectionBackground", new Color(0x2E5077));
UIManager.put("TextField.background", new Color(0x1A2A3A));
UIManager.put("TextField.foreground", new Color(0xEDE8D0));
UIManager.put("TextField.caretForeground", new Color(0x6B8FA8));
UIManager.put("PasswordField.background", new Color(0x1A2A3A));
UIManager.put("PasswordField.foreground", new Color(0xEDE8D0));
```

### 8.2 Rounded Button Class

Create a reusable `RoundedButton` that extends `JButton`:

```java
public class RoundedButton extends JButton {
    private int arc;
    public RoundedButton(String text, int arc) {
        super(text);
        this.arc = arc;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        super.paintComponent(g);
        g2.dispose();
    }
}
```

### 8.3 Layout Strategy

| Zone | Swing Component | Layout Manager |
|------|----------------|----------------|
| Root window | `JFrame` | `BorderLayout` |
| Top bar | `JPanel` at `NORTH` | `BorderLayout` |
| Left sidebar | `JPanel` at `WEST`, fixed 240px | `BoxLayout (Y_AXIS)` |
| Main content | `JPanel` at `CENTER` | `CardLayout` |
| Now playing bar | `JPanel` at `SOUTH`, fixed 90px | `BorderLayout` (3 sections) |
| Stats screen | `JPanel` (a card in CardLayout) | `GridBagLayout` or `MigLayout` |

### 8.4 Custom Seek/Volume Slider

Override `BasicSliderUI` to draw the slim 4px track and circular thumb:

```java
slider.setUI(new BasicSliderUI(slider) {
    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(0x2E5077));
        g2.fillRoundRect(trackRect.x, trackRect.y + trackRect.height/2 - 2,
                         trackRect.width, 4, 4, 4);
        // filled portion
        g2.setColor(new Color(0x6B8FA8));
        int filled = (int)((double)slider.getValue() / slider.getMaximum() * trackRect.width);
        g2.fillRoundRect(trackRect.x, trackRect.y + trackRect.height/2 - 2,
                         filled, 4, 4, 4);
    }
    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0x6B8FA8));
        g2.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
    }
});
```

### 8.5 Scrollbar Styling

```java
JScrollBar vsb = scrollPane.getVerticalScrollBar();
vsb.setPreferredSize(new Dimension(6, 0));
vsb.setUI(new BasicScrollBarUI() {
    @Override protected void configureScrollBarColors() {
        thumbColor = new Color(0x2E5077);
        trackColor = new Color(0x0D1321);
    }
    @Override protected JButton createDecreaseButton(int o) { return invisibleButton(); }
    @Override protected JButton createIncreaseButton(int o) { return invisibleButton(); }
    private JButton invisibleButton() {
        JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
    }
});
```

---

*This document covers the complete GUI specification for VibeVault. All measurements are in pixels. Implementation should follow §8 closely to achieve visual consistency across Windows, macOS, and Linux.*
