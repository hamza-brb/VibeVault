package com.vibevault.service;

import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.PlayHistory;
import com.vibevault.model.Song;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class PlayerService {
    private static final int MP3_FRAMES_PER_SECOND_ESTIMATE = 38;

    public enum RepeatMode {
        OFF,
        ONE,
        ALL
    }

    private final PlayHistoryDAO playHistoryDAO;
    private final Random random;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private final List<Song> queue = new ArrayList<>();
    private final List<Integer> playOrder = new ArrayList<>();

    private int orderPosition = -1;
    private boolean shuffleEnabled;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean playing;
    private int currentSecond;
    private int volumePercent = 70;
    private Thread playbackThread;
    private volatile AdvancedPlayer activePlayer;
    private volatile boolean stopPlaybackRequested;
    private Integer activeUserId;
    private long playbackStartEpochMillis = -1;
    private int playbackStartSecond;

    public PlayerService(DatabaseManager databaseManager) {
        this(databaseManager, new Random());
    }

    PlayerService(DatabaseManager databaseManager, Random random) {
        Objects.requireNonNull(databaseManager, "databaseManager must not be null");
        this.playHistoryDAO = new PlayHistoryDAO(databaseManager);
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public void setQueue(List<Song> songs) {
        Objects.requireNonNull(songs, "songs must not be null");
        finalizeCurrentSongSessionIfNeeded();
        stopAudioPlayback();
        queue.clear();
        queue.addAll(songs);
        rebuildPlayOrder();
        orderPosition = queue.isEmpty() ? -1 : 0;
        currentSecond = 0;
        playing = false;
    }

    public void enqueue(Song song) {
        Objects.requireNonNull(song, "song must not be null");
        int newIndex = queue.size();
        queue.add(song);
        if (shuffleEnabled) {
            int insertionPoint = random.nextInt(playOrder.size() + 1);
            playOrder.add(insertionPoint, newIndex);
            if (orderPosition >= insertionPoint) {
                orderPosition++;
            }
        } else {
            playOrder.add(newIndex);
        }

        if (orderPosition == -1) {
            orderPosition = 0;
        }
    }

    public void clearQueue() {
        finalizeCurrentSongSessionIfNeeded();
        stopAudioPlayback();
        queue.clear();
        playOrder.clear();
        orderPosition = -1;
        currentSecond = 0;
        playing = false;
    }

    public boolean removeQueueItem(int queueIndex) {
        validateManualQueueEditAllowed();
        if (queueIndex < 0 || queueIndex >= queue.size()) {
            throw new IllegalArgumentException("queueIndex out of range");
        }

        int currentQueueIndex = hasCurrentSong() ? playOrder.get(orderPosition) : -1;
        if (currentQueueIndex == queueIndex) {
            finalizeCurrentSongSessionIfNeeded();
        }
        queue.remove(queueIndex);
        rebuildPlayOrder();

        if (queue.isEmpty()) {
            orderPosition = -1;
            currentSecond = 0;
            playing = false;
            return true;
        }
        if (currentQueueIndex == -1) {
            orderPosition = 0;
            return true;
        }
        if (currentQueueIndex == queueIndex) {
            orderPosition = Math.min(queueIndex, queue.size() - 1);
            currentSecond = 0;
            return true;
        }

        orderPosition = currentQueueIndex > queueIndex ? currentQueueIndex - 1 : currentQueueIndex;
        return true;
    }

    public boolean moveQueueItem(int fromIndex, int toIndex) {
        validateManualQueueEditAllowed();
        if (fromIndex < 0 || fromIndex >= queue.size() || toIndex < 0 || toIndex >= queue.size()) {
            throw new IllegalArgumentException("queue index out of range");
        }
        if (fromIndex == toIndex) {
            return true;
        }

        int currentQueueIndex = hasCurrentSong() ? playOrder.get(orderPosition) : -1;
        Song moved = queue.remove(fromIndex);
        queue.add(toIndex, moved);
        rebuildPlayOrder();

        if (currentQueueIndex == -1) {
            orderPosition = queue.isEmpty() ? -1 : 0;
            return true;
        }
        orderPosition = remapMovedIndex(currentQueueIndex, fromIndex, toIndex);
        return true;
    }

    public List<Song> getQueueSnapshot() {
        return List.copyOf(queue);
    }

    public Optional<Song> getCurrentSong() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        return Optional.of(queue.get(playOrder.get(orderPosition)));
    }

    public Optional<Song> playAt(int queueIndex) {
        if (queueIndex < 0 || queueIndex >= queue.size()) {
            throw new IllegalArgumentException("queueIndex out of range");
        }
        int targetOrderPosition = playOrder.indexOf(queueIndex);
        if (targetOrderPosition < 0) {
            throw new IllegalStateException("Queue and play order are out of sync");
        }
        finalizeCurrentSongSessionIfNeeded();
        orderPosition = targetOrderPosition;
        currentSecond = 0;
        playing = true;
        markPlaybackSessionStart();
        startAudioPlaybackIfPossible();
        return getCurrentSong();
    }

    public Optional<Song> play() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        playing = true;
        markPlaybackSessionStart();
        startAudioPlaybackIfPossible();
        return getCurrentSong();
    }

    public void pause() {
        finalizeCurrentSongSessionIfNeeded();
        playing = false;
        stopAudioPlayback();
    }

    public Optional<Song> resume() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        playing = true;
        markPlaybackSessionStart();
        startAudioPlaybackIfPossible();
        return getCurrentSong();
    }

    public boolean isPlaying() {
        return playing;
    }

    public Optional<Song> next() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentSong();
        }

        if (orderPosition < playOrder.size() - 1) {
            finalizeCurrentSongSessionIfNeeded();
            orderPosition++;
            currentSecond = 0;
            markPlaybackSessionStart();
            if (playing) {
                startAudioPlaybackIfPossible();
            }
            return getCurrentSong();
        }

        if (repeatMode == RepeatMode.ALL) {
            finalizeCurrentSongSessionIfNeeded();
            orderPosition = 0;
            currentSecond = 0;
            markPlaybackSessionStart();
            if (playing) {
                startAudioPlaybackIfPossible();
            }
            return getCurrentSong();
        }

        finalizeCurrentSongSessionIfNeeded();
        playing = false;
        stopAudioPlayback();
        return Optional.empty();
    }

    public Optional<Song> previous() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentSong();
        }

        if (orderPosition > 0) {
            finalizeCurrentSongSessionIfNeeded();
            orderPosition--;
            currentSecond = 0;
            markPlaybackSessionStart();
            if (playing) {
                startAudioPlaybackIfPossible();
            }
            return getCurrentSong();
        }

        if (repeatMode == RepeatMode.ALL && !playOrder.isEmpty()) {
            finalizeCurrentSongSessionIfNeeded();
            orderPosition = playOrder.size() - 1;
            currentSecond = 0;
            markPlaybackSessionStart();
            if (playing) {
                startAudioPlaybackIfPossible();
            }
            return getCurrentSong();
        }

        return Optional.empty();
    }

    public void setShuffleEnabled(boolean shuffleEnabled) {
        if (this.shuffleEnabled == shuffleEnabled) {
            return;
        }

        Integer currentQueueIndex = hasCurrentSong() ? playOrder.get(orderPosition) : null;
        this.shuffleEnabled = shuffleEnabled;
        rebuildPlayOrder();

        if (currentQueueIndex == null) {
            orderPosition = queue.isEmpty() ? -1 : 0;
            return;
        }
        orderPosition = playOrder.indexOf(currentQueueIndex);
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = Objects.requireNonNull(repeatMode, "repeatMode must not be null");
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void seekToSecond(int targetSecond) {
        if (!hasCurrentSong()) {
            throw new IllegalStateException("No current song selected");
        }
        if (targetSecond < 0) {
            throw new IllegalArgumentException("targetSecond must be non-negative");
        }
        Song currentSong = queue.get(playOrder.get(orderPosition));
        Integer duration = currentSong.getDurationSeconds();
        if (duration != null && targetSecond > duration) {
            throw new IllegalArgumentException("targetSecond exceeds song duration");
        }
        currentSecond = targetSecond;
        if (playing) {
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
        }
    }

    public int getCurrentSecond() {
        return currentSecond;
    }

    public void setVolumePercent(int volumePercent) {
        if (volumePercent < 0 || volumePercent > 100) {
            throw new IllegalArgumentException("volumePercent must be between 0 and 100");
        }
        int previous = this.volumePercent;
        this.volumePercent = volumePercent;
        propertyChangeSupport.firePropertyChange("volumePercent", previous, this.volumePercent);
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public Optional<PlayHistory> logCurrentSongPlayback(int userId, int durationListenedSeconds) {
        if (durationListenedSeconds < 5 || !hasCurrentSong()) {
            return Optional.empty();
        }
        Song currentSong = queue.get(playOrder.get(orderPosition));
        PlayHistory play = playHistoryDAO.logPlay(userId, currentSong.getSongId(), durationListenedSeconds);
        return Optional.of(play);
    }

    public void setActiveUserId(Integer userId) {
        this.activeUserId = userId;
    }

    private boolean hasCurrentSong() {
        return orderPosition >= 0 && orderPosition < playOrder.size();
    }

    private void validateManualQueueEditAllowed() {
        // Shuffle restriction removed to allow queue editing even in shuffle mode
    }

    private static int remapMovedIndex(int index, int from, int to) {
        if (index == from) {
            return to;
        }
        if (from < to && index > from && index <= to) {
            return index - 1;
        }
        if (from > to && index >= to && index < from) {
            return index + 1;
        }
        return index;
    }

    private void rebuildPlayOrder() {
        playOrder.clear();
        for (int i = 0; i < queue.size(); i++) {
            playOrder.add(i);
        }
        if (shuffleEnabled) {
            Collections.shuffle(playOrder, random);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void startAudioPlaybackIfPossible() {
        Song currentSong = getCurrentSong().orElse(null);
        if (currentSong == null || !playing) {
            return;
        }

        Optional<Path> candidate = resolvePlayablePath(currentSong.getFilePath());
        if (candidate.isEmpty()) {
            propertyChangeSupport.firePropertyChange(
                    "playbackError",
                    null,
                    "File not found on this device: " + currentSong.getTitle()
                            + "\nPath: " + currentSong.getFilePath()
            );
            playing = false;
            return;
        }

        stopAudioPlayback();
        stopPlaybackRequested = false;
        Path path = candidate.get();
        int startSecond = currentSecond;
        Thread thread = new Thread(() -> runPlayback(path, startSecond), "vibevault-player-thread");
        thread.setDaemon(true);
        playbackThread = thread;
        thread.start();
    }

    private void runPlayback(Path path, int startSecond) {
        AdvancedPlayer localPlayer = null;
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            localPlayer = new AdvancedPlayer(inputStream);
            localPlayer.setPlayBackListener(new PlaybackListener() {
                @Override
                public void playbackFinished(PlaybackEvent evt) {
                    int finishedSecond = secondsFromFrame(evt.getFrame());
                    if (finishedSecond > 0) {
                        currentSecond = Math.max(currentSecond, finishedSecond);
                    }
                }
            });
            activePlayer = localPlayer;
            int startFrame = frameFromSecond(startSecond);
            if (startFrame > 0) {
                localPlayer.play(startFrame, Integer.MAX_VALUE);
            } else {
                localPlayer.play();
            }
        } catch (IOException | JavaLayerException e) {
            propertyChangeSupport.firePropertyChange("playbackError", null, e.getMessage());
        } finally {
            activePlayer = null;
            playbackThread = null;
            if (!stopPlaybackRequested) {
                finalizeCurrentSongSessionIfNeeded();
                boolean previousPlaying = playing;
                playing = false;
                propertyChangeSupport.firePropertyChange("playing", previousPlaying, false);
            }
        }
    }

    private void stopAudioPlayback() {
        stopPlaybackRequested = true;
        AdvancedPlayer playerToClose = activePlayer;
        if (playerToClose != null) {
            playerToClose.close();
            activePlayer = null;
        }
    }

    private void markPlaybackSessionStart() {
        if (!playing || !hasCurrentSong()) {
            return;
        }
        playbackStartEpochMillis = System.currentTimeMillis();
        playbackStartSecond = currentSecond;
    }

    private void finalizeCurrentSongSessionIfNeeded() {
        if (!playing || activeUserId == null || !hasCurrentSong()) {
            resetPlaybackSessionClock();
            return;
        }
        if (playbackStartEpochMillis <= 0) {
            resetPlaybackSessionClock();
            return;
        }

        int elapsedSeconds = (int) Math.max(0, (System.currentTimeMillis() - playbackStartEpochMillis) / 1000L);
        int totalListened = Math.max(currentSecond, playbackStartSecond + elapsedSeconds);
        logCurrentSongPlayback(activeUserId, totalListened);
        currentSecond = totalListened;
        resetPlaybackSessionClock();
    }

    private void resetPlaybackSessionClock() {
        playbackStartEpochMillis = -1;
        playbackStartSecond = currentSecond;
    }

    private static Optional<Path> resolvePlayablePath(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        try {
            Path candidate = source.startsWith("file:")
                    ? Path.of(java.net.URI.create(source))
                    : Paths.get(source);
            if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                return Optional.empty();
            }
            String fileName = candidate.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".mp3")) {
                return Optional.empty();
            }
            return Optional.of(candidate);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static int frameFromSecond(int second) {
        if (second <= 0) {
            return 0;
        }
        return second * MP3_FRAMES_PER_SECOND_ESTIMATE;
    }

    private static int secondsFromFrame(int frame) {
        if (frame <= 0) {
            return 0;
        }
        return frame / MP3_FRAMES_PER_SECOND_ESTIMATE;
    }
}
