package com.vibevault.service;

import com.vibevault.dao.PlayHistoryDAO;
import com.vibevault.db.DatabaseManager;
import com.vibevault.model.PlayHistory;
import com.vibevault.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class PlayerService {
    public enum RepeatMode {
        OFF,
        ONE,
        ALL
    }

    private final PlayHistoryDAO playHistoryDAO;
    private final Random random;

    private final List<Song> queue = new ArrayList<>();
    private final List<Integer> playOrder = new ArrayList<>();

    private int orderPosition = -1;
    private boolean shuffleEnabled;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private boolean playing;
    private int currentSecond;
    private int volumePercent = 70;

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
        orderPosition = targetOrderPosition;
        currentSecond = 0;
        playing = true;
        return getCurrentSong();
    }

    public Optional<Song> play() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        playing = true;
        return getCurrentSong();
    }

    public void pause() {
        playing = false;
    }

    public Optional<Song> resume() {
        if (!hasCurrentSong()) {
            return Optional.empty();
        }
        playing = true;
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
            orderPosition++;
            currentSecond = 0;
            return getCurrentSong();
        }

        if (repeatMode == RepeatMode.ALL) {
            orderPosition = 0;
            currentSecond = 0;
            return getCurrentSong();
        }

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
            orderPosition--;
            currentSecond = 0;
            return getCurrentSong();
        }

        if (repeatMode == RepeatMode.ALL && !playOrder.isEmpty()) {
            orderPosition = playOrder.size() - 1;
            currentSecond = 0;
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
        currentSecond = targetSecond;
    }

    public int getCurrentSecond() {
        return currentSecond;
    }

    public void setVolumePercent(int volumePercent) {
        if (volumePercent < 0 || volumePercent > 100) {
            throw new IllegalArgumentException("volumePercent must be between 0 and 100");
        }
        this.volumePercent = volumePercent;
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    public Optional<PlayHistory> logCurrentSongPlayback(int userId, int durationListenedSeconds) {
        if (durationListenedSeconds <= 10 || !hasCurrentSong()) {
            return Optional.empty();
        }
        Song currentSong = queue.get(playOrder.get(orderPosition));
        PlayHistory play = playHistoryDAO.logPlay(userId, currentSong.getSongId(), durationListenedSeconds);
        return Optional.of(play);
    }

    private boolean hasCurrentSong() {
        return orderPosition >= 0 && orderPosition < playOrder.size();
    }

    private void validateManualQueueEditAllowed() {
        if (shuffleEnabled) {
            throw new IllegalStateException("Disable shuffle before manually editing queue order");
        }
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
}
