package com.vibevault.service;

import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.PlayHistory;
import com.vibevault.model.Song;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PlayerService {
    private static final String PLAYING_PROPERTY = "playing";

    private static final Object FX_INIT_LOCK = new Object();
    private static volatile boolean fxInitialized;

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
    private volatile long playbackSessionId;
    private volatile boolean naturallyEnded;
    private volatile MediaPlayer activeMediaPlayer;
    private volatile long activeMediaSessionId = -1L;
    private Integer activeUserId;
    private long playbackStartEpochMillis = -1;
    private int playbackStartSecond;
    private Integer activePlayHistoryId;
    private int activePlayListenedSeconds;

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
        naturallyEnded = false;
        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        stopAudioPlayback();
        queue.clear();
        queue.addAll(songs);
        rebuildPlayOrder();
        orderPosition = queue.isEmpty() ? -1 : 0;
        currentSecond = 0;
        resetActivePlayTracking();
        setPlaying(false);
        propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
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
        naturallyEnded = false;
        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        stopAudioPlayback();
        queue.clear();
        playOrder.clear();
        orderPosition = -1;
        currentSecond = 0;
        resetActivePlayTracking();
        setPlaying(false);
        propertyChangeSupport.firePropertyChange("currentSong", null, null);
    }

    public boolean removeQueueItem(int queueIndex) {
        validateManualQueueEditAllowed();
        if (queueIndex < 0 || queueIndex >= queue.size()) {
            throw new IllegalArgumentException("queueIndex out of range");
        }

        int currentQueueIndex = hasCurrentSong() ? playOrder.get(orderPosition) : -1;
        if (currentQueueIndex == queueIndex) {
            finalizeCurrentSongSessionIfNeeded();
            commitCurrentSongPlayIfNeeded();
        }
        queue.remove(queueIndex);
        rebuildPlayOrder();

        if (queue.isEmpty()) {
            orderPosition = -1;
            currentSecond = 0;
            resetActivePlayTracking();
            setPlaying(false);
            propertyChangeSupport.firePropertyChange("currentSong", null, null);
            return true;
        }
        if (currentQueueIndex == -1) {
            orderPosition = 0;
            return true;
        }
        if (currentQueueIndex == queueIndex) {
            orderPosition = Math.min(queueIndex, queue.size() - 1);
            currentSecond = 0;
            resetActivePlayTracking();
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
        commitCurrentSongPlayIfNeeded();
        naturallyEnded = false;
        orderPosition = targetOrderPosition;
        currentSecond = 0;
        resetActivePlayTracking();
        setPlaying(true);
        markPlaybackSessionStart();
        startAudioPlaybackIfPossible();
        propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
        return getCurrentSong();
    }

    public Optional<Song> play() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        naturallyEnded = false;
        setPlaying(true);
        markPlaybackSessionStart();
        startAudioPlaybackIfPossible();
        return getCurrentSong();
    }

    public void pause() {
        naturallyEnded = false;
        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        setPlaying(false);
        stopAudioPlayback();
    }

    public Optional<Song> resume() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        naturallyEnded = false;
        setPlaying(true);
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
        naturallyEnded = false;
        if (repeatMode == RepeatMode.ONE) {
            finalizeCurrentSongSessionIfNeeded();
            commitCurrentSongPlayIfNeeded();
            currentSecond = 0;
            resetActivePlayTracking();
            if (playing) {
                setPlaying(true);
                markPlaybackSessionStart();
                startAudioPlaybackIfPossible();
            }
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        orderPosition = orderPosition < playOrder.size() - 1 ? orderPosition + 1 : 0;
        currentSecond = 0;
        resetActivePlayTracking();
        markPlaybackSessionStart();
        if (playing) {
            startAudioPlaybackIfPossible();
        }
        propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
        return getCurrentSong();
    }

    public Optional<Song> previous() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        naturallyEnded = false;
        if (repeatMode == RepeatMode.ONE) {
            finalizeCurrentSongSessionIfNeeded();
            commitCurrentSongPlayIfNeeded();
            currentSecond = 0;
            resetActivePlayTracking();
            if (playing) {
                markPlaybackSessionStart();
                startAudioPlaybackIfPossible();
            }
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        orderPosition = orderPosition > 0 ? orderPosition - 1 : playOrder.size() - 1;
        currentSecond = 0;
        resetActivePlayTracking();
        markPlaybackSessionStart();
        if (playing) {
            startAudioPlaybackIfPossible();
        }
        propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
        return getCurrentSong();
    }

    public Optional<Song> handleTrackCompletion() {
        if (!hasCurrentSong()) {
            naturallyEnded = false;
            return Optional.empty();
        }
        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();

        if (repeatMode == RepeatMode.ONE) {
            naturallyEnded = false;
            currentSecond = 0;
            resetActivePlayTracking();
            setPlaying(true);
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        if (orderPosition < playOrder.size() - 1) {
            naturallyEnded = false;
            orderPosition++;
            currentSecond = 0;
            resetActivePlayTracking();
            setPlaying(true);
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        if (repeatMode == RepeatMode.ALL && !playOrder.isEmpty()) {
            naturallyEnded = false;
            orderPosition = 0;
            currentSecond = 0;
            resetActivePlayTracking();
            setPlaying(true);
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        naturallyEnded = false;
        currentSecond = 0;
        resetActivePlayTracking();
        setPlaying(false);
        propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
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

        naturallyEnded = false;
        if (playing) {
            finalizeCurrentSongSessionIfNeeded();
        }
        currentSecond = targetSecond;

        if (!playing) {
            return;
        }
        markPlaybackSessionStart();

        MediaPlayer mediaPlayer = activeMediaPlayer;
        if (mediaPlayer != null) {
            runOnFxThread(() -> {
                if (mediaPlayer != activeMediaPlayer) {
                    return;
                }
                mediaPlayer.seek(Duration.seconds(targetSecond));
                if (mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                    mediaPlayer.play();
                }
            });
            return;
        }

        startAudioPlaybackIfPossible();
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
        applyVolumeToActiveOutput();
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
        if (this.activeUserId != null && userId == null) {
            finalizeCurrentSongSessionIfNeeded();
            commitCurrentSongPlayIfNeeded();
            resetActivePlayTracking();
        }
        this.activeUserId = userId;
    }

    public boolean isNaturallyEnded() {
        return naturallyEnded;
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
        naturallyEnded = false;

        Optional<Path> candidate = resolvePlayablePath(currentSong.getFilePath());
        if (candidate.isEmpty()) {
            propertyChangeSupport.firePropertyChange(
                    "playbackError",
                    null,
                    "File not found on this device: " + currentSong.getTitle()
                            + "\nPath: " + currentSong.getFilePath()
            );
            setPlaying(false);
            return;
        }

        if (!ensureJavaFxRuntime()) {
            propertyChangeSupport.firePropertyChange(
                    "playbackError",
                    null,
                    "JavaFX media runtime could not be initialized."
            );
            return;
        }

        stopAudioPlayback();
        long sessionId = ++playbackSessionId;
        Path path = candidate.get();
        int startSecond = Math.max(0, currentSecond);

        runOnFxThread(() -> {
            if (sessionId != playbackSessionId) {
                return;
            }
            try {
                Media media = new Media(path.toUri().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(volumePercent / 100.0);
                activeMediaPlayer = mediaPlayer;
                activeMediaSessionId = sessionId;

                mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                    if (!isSessionActive(sessionId) || newVal == null || newVal.isUnknown()) {
                        return;
                    }
                    int seconds = (int) Math.floor(newVal.toSeconds());
                    if (seconds >= 0) {
                        currentSecond = seconds;
                    }
                });

                mediaPlayer.setOnReady(() -> {
                    if (!isSessionActive(sessionId)) {
                        mediaPlayer.dispose();
                        return;
                    }
                    Duration total = mediaPlayer.getTotalDuration();
                    Duration target = Duration.seconds(startSecond);
                    if (total != null && !total.isUnknown() && target.greaterThan(total)) {
                        target = total;
                    }
                    mediaPlayer.seek(target);
                    if (playing && isSessionActive(sessionId)) {
                        mediaPlayer.play();
                    }
                });

                mediaPlayer.setOnEndOfMedia(() -> {
                    if (!isSessionActive(sessionId)) {
                        return;
                    }
                    naturallyEnded = true;
                    Duration current = mediaPlayer.getCurrentTime();
                    if (current != null && !current.isUnknown()) {
                        currentSecond = Math.max(currentSecond, (int) Math.floor(current.toSeconds()));
                    }
                    finalizeCurrentSongSessionIfNeeded();
                    commitCurrentSongPlayIfNeeded();
                    setPlaying(false);
                });

                mediaPlayer.setOnError(() -> {
                    if (!isSessionActive(sessionId)) {
                        return;
                    }
                    Throwable error = mediaPlayer.getError();
                    String message = error == null ? "Playback failed" : error.getMessage();
                    handlePlaybackFailure(sessionId, message);
                });
            } catch (Exception ex) {
                if (isSessionActive(sessionId)) {
                    String message = ex.getMessage() == null ? "Playback failed" : ex.getMessage();
                    handlePlaybackFailure(sessionId, message);
                }
            }
        });
    }

    private void stopAudioPlayback() {
        MediaPlayer playerToDispose = activeMediaPlayer;
        activeMediaPlayer = null;
        activeMediaSessionId = -1L;
        if (playerToDispose == null) {
            return;
        }
        runOnFxThread(() -> {
            try {
                playerToDispose.stop();
            } catch (Exception ignored) {
            }
            try {
                playerToDispose.dispose();
            } catch (Exception ignored) {
            }
        });
    }

    private boolean isSessionActive(long sessionId) {
        return sessionId == playbackSessionId && sessionId == activeMediaSessionId;
    }

    private static boolean ensureJavaFxRuntime() {
        if (fxInitialized) {
            return true;
        }
        synchronized (FX_INIT_LOCK) {
            if (fxInitialized) {
                return true;
            }
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    fxInitialized = true;
                    latch.countDown();
                });
            } catch (IllegalStateException alreadyInitialized) {
                fxInitialized = true;
                return true;
            } catch (Throwable t) {
                return false;
            }

            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return fxInitialized;
        }
    }

    private static void runOnFxThread(Runnable runnable) {
        if (!ensureJavaFxRuntime()) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private void markPlaybackSessionStart() {
        if (!playing || !hasCurrentSong()) {
            return;
        }
        registerPlayStartIfNeeded();
        playbackStartEpochMillis = System.currentTimeMillis();
        playbackStartSecond = currentSecond;
    }

    private void finalizeCurrentSongSessionIfNeeded() {
        if (!hasCurrentSong()) {
            resetPlaybackSessionClock();
            return;
        }
        if (playbackStartEpochMillis <= 0) {
            resetPlaybackSessionClock();
            return;
        }

        int elapsedSeconds = (int) Math.max(0, (System.currentTimeMillis() - playbackStartEpochMillis) / 1000L);
        if (elapsedSeconds > 0 && activePlayHistoryId != null) {
            Song song = queue.get(playOrder.get(orderPosition));
            Integer duration = song.getDurationSeconds();
            int next = activePlayListenedSeconds + elapsedSeconds;
            activePlayListenedSeconds = duration == null ? next : Math.min(duration, next);
        }
        int positionFromClock = playbackStartSecond + elapsedSeconds;
        currentSecond = Math.max(currentSecond, positionFromClock);
        resetPlaybackSessionClock();
    }

    private void commitCurrentSongPlayIfNeeded() {
        if (activePlayHistoryId == null) {
            return;
        }
        playHistoryDAO.updateDurationListened(activePlayHistoryId, activePlayListenedSeconds);
    }

    private void registerPlayStartIfNeeded() {
        if (activePlayHistoryId != null || activeUserId == null || !hasCurrentSong()) {
            return;
        }
        Song currentSong = queue.get(playOrder.get(orderPosition));
        PlayHistory play = playHistoryDAO.logPlay(activeUserId, currentSong.getSongId(), 0);
        activePlayHistoryId = play.getPlayId();
        activePlayListenedSeconds = 0;
    }

    private void resetActivePlayTracking() {
        activePlayHistoryId = null;
        activePlayListenedSeconds = 0;
    }

    private void resetPlaybackSessionClock() {
        playbackStartEpochMillis = -1;
        playbackStartSecond = currentSecond;
    }

    private void setPlaying(boolean newValue) {
        boolean previous = this.playing;
        this.playing = newValue;
        if (previous != newValue) {
            propertyChangeSupport.firePropertyChange(PLAYING_PROPERTY, previous, newValue);
        }
    }

    private void applyVolumeToActiveOutput() {
        MediaPlayer mediaPlayer = activeMediaPlayer;
        if (mediaPlayer == null) {
            return;
        }
        runOnFxThread(() -> {
            if (mediaPlayer == activeMediaPlayer) {
                mediaPlayer.setVolume(volumePercent / 100.0);
            }
        });
    }

    private void handlePlaybackFailure(long sessionId, String message) {
        if (!isSessionActive(sessionId)) {
            return;
        }
        naturallyEnded = false;
        finalizeCurrentSongSessionIfNeeded();
        commitCurrentSongPlayIfNeeded();
        stopAudioPlayback();
        setPlaying(false);
        propertyChangeSupport.firePropertyChange("playbackError", null, message);
    }

    private static Optional<Path> resolvePlayablePath(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        try {
            Path candidate = source.startsWith("file:")
                    ? Path.of(java.net.URI.create(source))
                    : Path.of(source);
            Path normalized = candidate.toAbsolutePath().normalize();
            if (isPlayableMp3(normalized)) {
                return Optional.of(normalized);
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static boolean isPlayableMp3(Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        return name.endsWith(".mp3");
    }
}
