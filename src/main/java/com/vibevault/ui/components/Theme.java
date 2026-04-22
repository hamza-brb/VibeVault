package com.vibevault.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public final class Theme {
    public static final Color BG_DEEP = new Color(0x000000);
    public static final Color BG_SURFACE = new Color(0x121212);
    public static final Color BG_ELEVATED = new Color(0x181818);
    public static final Color BG_HOVER = new Color(0x232323);
    public static final Color BG_BORDER = new Color(0x2E2E2E);

    public static final Color TEXT_PRIMARY = new Color(0xF5F5F5);
    public static final Color TEXT_MUTED = new Color(0xB3B3B3);
    public static final Color TEXT_SUBTLE = new Color(0x8A8A8A);

    public static final Color ACCENT = new Color(0x1ED760);
    public static final Color ACCENT_SOFT = new Color(0x13361F);
    public static final Color ACCENT_GLOW = new Color(0x1E, 0xD7, 0x60, 56);
    public static final Color DANGER = new Color(0xFF5A6B);

    private static final Font FONT_BODY_BASE = loadFont("/fonts/Outfit-Regular.ttf", Font.PLAIN, 13f);
    private static final Font FONT_HEADING_BASE = loadFont("/fonts/Outfit-Bold.ttf", Font.BOLD, 14f);

    private Theme() {
    }

    public static Font body(float size) {
        return FONT_BODY_BASE.deriveFont(size);
    }

    public static Font heading(float size) {
        return FONT_HEADING_BASE.deriveFont(size);
    }

    private static Font loadFont(String path, int fallbackStyle, float size) {
        try (InputStream stream = Theme.class.getResourceAsStream(path)) {
            if (stream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font.deriveFont(size);
            }
        } catch (Exception ignored) {
        }
        return new Font("Segoe UI", fallbackStyle, Math.round(size));
    }
}
