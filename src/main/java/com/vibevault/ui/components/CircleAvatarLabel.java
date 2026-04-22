package com.vibevault.ui.components;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

public class CircleAvatarLabel extends JLabel {
    private String seedText;

    public CircleAvatarLabel(String seedText) {
        setOpaque(false);
        setSeedText(seedText);
    }

    public void setSeedText(String seedText) {
        this.seedText = seedText == null ? "" : seedText.trim();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight());
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        Color start = new Color(0x00B8F5);
        Color end = new Color(0x0D5C75);
        LinearGradientPaint paint = new LinearGradientPaint(
                x, y, x + size, y + size,
                new float[]{0f, 1f},
                new Color[]{start, end}
        );
        Ellipse2D circle = new Ellipse2D.Float(x, y, size, size);
        g2.setPaint(paint);
        g2.fill(circle);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.draw(circle);

        String initials = initials(seedText);
        g2.setColor(Theme.TEXT_PRIMARY);
        g2.setFont(Theme.heading(Math.max(12f, size * 0.32f)));
        FontMetrics metrics = g2.getFontMetrics();
        int textX = x + (size - metrics.stringWidth(initials)) / 2;
        int textY = y + ((size - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(initials, textX, textY);
        g2.dispose();
    }

    private static String initials(String text) {
        if (text == null || text.isBlank()) {
            return "VV";
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}
