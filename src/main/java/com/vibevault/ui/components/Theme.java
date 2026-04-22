package com.vibevault.ui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

public final class Theme {
    public static final Color BG_DEEP = new Color(0x0D1117);
    public static final Color BG_SURFACE = new Color(0x161B22);
    public static final Color BG_ELEVATED = new Color(0x111827);
    public static final Color BG_HOVER = new Color(0x1C2230);
    public static final Color BG_BORDER = new Color(0x2A3244);

    public static final Color TEXT_PRIMARY = new Color(0xE6EDF3);
    public static final Color TEXT_MUTED = new Color(0x7D8FA8);
    public static final Color TEXT_SUBTLE = new Color(0x9FB1C7);

    public static final Color ACCENT = new Color(0x00C8FF);
    public static final Color ACCENT_SOFT = new Color(0x0B3A49);
    public static final Color ACCENT_GLOW = new Color(0x00, 0xC8, 0xFF, 40);
    public static final Color DANGER = new Color(0xFF4D6D);

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
