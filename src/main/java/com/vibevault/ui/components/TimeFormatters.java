package com.vibevault.ui.components;

public final class TimeFormatters {
    private TimeFormatters() {
    }

    public static String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds <= 0) {
            return "--:--";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
