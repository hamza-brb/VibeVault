package com.vibevault.ui.components;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class AccentSliderUI extends BasicSliderUI {
    private final Color accent;
    private final Color track;

    public AccentSliderUI(JSlider slider, Color accent, Color track) {
        super(slider);
        this.accent = accent;
        this.track = track;
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(12, 12);
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int trackHeight = 4;
        int y = trackRect.y + (trackRect.height - trackHeight) / 2;
        float arc = trackHeight;
        RoundRectangle2D fullTrack = new RoundRectangle2D.Float(trackRect.x, y, trackRect.width, trackHeight, arc, arc);
        g2.setColor(track);
        g2.fill(fullTrack);

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int fillWidth = thumbRect.x + (thumbRect.width / 2) - trackRect.x;
            fillWidth = Math.max(0, Math.min(fillWidth, trackRect.width));
            if (fillWidth > 0) {
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(trackRect.x, y, fillWidth, trackHeight, arc, arc));
            }
        }
        g2.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Ellipse2D thumb = new Ellipse2D.Float(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        g2.setColor(accent);
        g2.fill(thumb);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(thumb);
        g2.dispose();
    }

    @Override
    public void paintFocus(Graphics g) {
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.getBackground());
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        super.paint(g2, c);
        g2.dispose();
    }

    @Override
    public void setThumbLocation(int x, int y) {
        super.setThumbLocation(x, y);
        slider.repaint();
    }
}
