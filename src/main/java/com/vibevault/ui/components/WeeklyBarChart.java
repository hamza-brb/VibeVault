package com.vibevault.ui.components;

import com.vibevault.service.StatsService;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

public class WeeklyBarChart extends JPanel {
    private final Color backgroundColor;
    private final Color barColor;
    private final Color textPrimary;
    private final Color textMuted;
    private List<StatsService.DailyListeningStat> data = new ArrayList<>();

    public WeeklyBarChart(Color backgroundColor, Color barColor, Color textPrimary, Color textMuted) {
        this.backgroundColor = backgroundColor;
        this.barColor = barColor;
        this.textPrimary = textPrimary;
        this.textMuted = textMuted;
    }

    public void setData(List<StatsService.DailyListeningStat> data) {
        this.data = data == null ? List.of() : List.copyOf(data);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padLeft = 40;
        int padRight = 16;
        int padTop = 16;
        int padBottom = 36;
        int chartWidth = Math.max(1, w - padLeft - padRight);
        int chartHeight = Math.max(1, h - padTop - padBottom);

        g2.setColor(backgroundColor);
        g2.fillRect(0, 0, w, h);

        if (data == null || data.isEmpty()) {
            g2.setColor(textMuted);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("No data this week", padLeft + 8, padTop + chartHeight / 2);
            g2.dispose();
            return;
        }

        double maxMinutes = data.stream().mapToDouble(StatsService.DailyListeningStat::totalMinutes).max().orElse(1.0);
        if (maxMinutes <= 0) {
            maxMinutes = 1.0;
        }

        int barCount = data.size();
        int totalGap = chartWidth / 6;
        int barWidth = Math.max(8, (chartWidth - totalGap) / Math.max(1, barCount));
        int gap = Math.max(4, totalGap / (barCount + 1));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        for (int i = 0; i < barCount; i++) {
            StatsService.DailyListeningStat stat = data.get(i);
            int barHeight = (int) ((stat.totalMinutes() / maxMinutes) * chartHeight);
            int x = padLeft + gap + i * (barWidth + gap);
            int y = padTop + chartHeight - barHeight;

            g2.setColor(barColor);
            g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

            String dayLabel = stat.day().length() >= 10 ? stat.day().substring(5) : stat.day();
            g2.setColor(textMuted);
            FontMetrics fm = g2.getFontMetrics();
            int labelX = x + (barWidth - fm.stringWidth(dayLabel)) / 2;
            g2.drawString(dayLabel, labelX, padTop + chartHeight + 18);

            if (stat.totalMinutes() > 0) {
                String val = stat.totalMinutes() + "m";
                int valX = x + (barWidth - fm.stringWidth(val)) / 2;
                g2.setColor(textPrimary);
                g2.drawString(val, valX, y - 4);
            }
        }

        g2.setColor(textPrimary);
        g2.drawLine(padLeft, padTop + chartHeight, padLeft + chartWidth, padTop + chartHeight);
        g2.dispose();
    }
}
