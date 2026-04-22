package com.vibevault.service;

import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.PlayHistory;
import com.vibevault.model.Song;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
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
    private static final String PLAYING_PROPERTY = "playing";

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
    private volatile VolumeAwareAudioDevice activeAudioDevice;
    private volatile boolean stopPlaybackRequested;
    private volatile long playbackSessionId;
    private volatile boolean naturallyEnded;
    private volatile javax.sound.sampled.FloatControl gainControl;
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
        naturallyEnded = false;
        finalizeCurrentSongSessionIfNeeded();
        stopAudioPlayback();
        queue.clear();
        queue.addAll(songs);
        rebuildPlayOrder();
        orderPosition = queue.isEmpty() ? -1 : 0;
        currentSecond = 0;
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
        stopAudioPlayback();
        queue.clear();
        playOrder.clear();
        orderPosition = -1;
        currentSecond = 0;
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
        }
        queue.remove(queueIndex);
        rebuildPlayOrder();

        if (queue.isEmpty()) {
            orderPosition = -1;
            currentSecond = 0;
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
        naturallyEnded = false;
        orderPosition = targetOrderPosition;
        currentSecond = 0;
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
            currentSecond = 0;
            if (playing) {
                setPlaying(true);
                markPlaybackSessionStart();
                startAudioPlaybackIfPossible();
            }
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        finalizeCurrentSongSessionIfNeeded();
        orderPosition = orderPosition < playOrder.size() - 1 ? orderPosition + 1 : 0;
        currentSecond = 0;
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
            currentSecond = 0;
            if (playing) {
                markPlaybackSessionStart();
                startAudioPlaybackIfPossible();
            }
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        finalizeCurrentSongSessionIfNeeded();
        orderPosition = orderPosition > 0 ? orderPosition - 1 : playOrder.size() - 1;
        currentSecond = 0;
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

        if (repeatMode == RepeatMode.ONE) {
            naturallyEnded = false;
            currentSecond = 0;
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
            setPlaying(true);
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
            propertyChangeSupport.firePropertyChange("currentSong", null, getCurrentSong().orElse(null));
            return getCurrentSong();
        }

        naturallyEnded = false;
        currentSecond = 0;
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
        currentSecond = targetSecond;
        if (playing) {
            markPlaybackSessionStart();
            startAudioPlaybackIfPossible();
        }
    }

    public int getCurrentSecond() {
        VolumeAwareAudioDevice audioDevice = activeAudioDevice;
        if (playing && audioDevice != null) {
            currentSecond = Math.max(currentSecond, audioDevice.getPosition() / 1000);
        }
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

        stopAudioPlayback();
        stopPlaybackRequested = false;
        long sessionId = ++playbackSessionId;
        Path path = candidate.get();
        int startSecond = currentSecond;
        Thread thread = new Thread(() -> runPlayback(path, startSecond, sessionId), "vibevault-player-thread");
        thread.setDaemon(true);
        playbackThread = thread;
        thread.start();
    }

    private void runPlayback(Path path, int startSecond, long sessionId) {
        AdvancedPlayer localPlayer = null;
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            VolumeAwareAudioDevice audioDevice = new VolumeAwareAudioDevice();
            activeAudioDevice = audioDevice;
            localPlayer = new AdvancedPlayer(inputStream, audioDevice);
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
            activeAudioDevice = null;
            playbackThread = null;
            if (!stopPlaybackRequested && playbackSessionId == sessionId) {
                naturallyEnded = true;
                finalizeCurrentSongSessionIfNeeded();
                setPlaying(false);
            }
        }
    }

    private void stopAudioPlayback() {
        stopPlaybackRequested = true;
        gainControl = null;
        activeAudioDevice = null;
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

    private void setPlaying(boolean newValue) {
        boolean previous = this.playing;
        this.playing = newValue;
        if (previous != newValue) {
            propertyChangeSupport.firePropertyChange(PLAYING_PROPERTY, previous, newValue);
        }
    }

    private void applyVolumeToActiveOutput() {
        javax.sound.sampled.FloatControl control = gainControl;
        if (control == null) {
            return;
        }
        float dB;
        if (volumePercent == 0) {
            dB = control.getMinimum();
        } else {
            dB = (float) (20.0 * Math.log10(volumePercent / 100.0));
        }
        dB = Math.max(control.getMinimum(), Math.min(control.getMaximum(), dB));
        control.setValue(dB);
    }

    private static Optional<Path> resolvePlayablePath(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        try {
            Path candidate = source.startsWith("file:")
                    ? Path.of(java.net.URI.create(source))
                    : Paths.get(source);
            Path normalized = candidate.toAbsolutePath().normalize();
            if (isPlayableMp3(normalized)) {
                return Optional.of(normalized);
            }

            String fileName = candidate.getFileName() == null ? null : candidate.getFileName().toString();
            Optional<Path> fallback = resolveFromSharedSongsByFileName(fileName);
            if (fallback.isPresent()) {
                return fallback;
            }
        } catch (IllegalArgumentException e) {
            // Try a tolerant fallback using just filename extraction from raw source.
        }

        String rawFileName = extractFileName(source);
        return resolveFromSharedSongsByFileName(rawFileName);
    }

    private static Optional<Path> resolveFromSharedSongsByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        try {
            Path sharedCandidate = Path.of(System.getProperty("user.dir"), "songs", fileName)
                    .toAbsolutePath()
                    .normalize();
            if (isPlayableMp3(sharedCandidate)) {
                return Optional.of(sharedCandidate);
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

    private static String extractFileName(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        int slash = source.lastIndexOf('/');
        int backslash = source.lastIndexOf('\\');
        int idx = Math.max(slash, backslash);
        return idx >= 0 && idx + 1 < source.length() ? source.substring(idx + 1) : source;
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

    private final class VolumeAwareAudioDevice extends JavaSoundAudioDevice {
        @Override
        protected void createSource() throws JavaLayerException {
            super.createSource();
            try {
                Field sourceField = JavaSoundAudioDevice.class.getDeclaredField("source");
                sourceField.setAccessible(true);
                Object source = sourceField.get(this);
                if (source instanceof javax.sound.sampled.SourceDataLine line
                        && line.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    gainControl = (javax.sound.sampled.FloatControl) line.getControl(
                            javax.sound.sampled.FloatControl.Type.MASTER_GAIN
                    );
                    applyVolumeToActiveOutput();
                }
            } catch (ReflectiveOperationException ignored) {
                gainControl = null;
            }
        }
    }
}
