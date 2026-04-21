package com.vibevault.ui.components;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedPanel extends JPanel {
    private final int arc;
    private final Color backgroundColor;
    private Color borderColor;
    private int borderThickness;

    public RoundedPanel(int arc, Color backgroundColor) {
        this.arc = arc;
        this.backgroundColor = backgroundColor;
        setOpaque(false);
    }

    public void setBorderConfig(Color color, int thickness) {
        this.borderColor = color;
        this.borderThickness = thickness;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        
        // Draw border if configured
        if (borderColor != null && borderThickness > 0) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(borderThickness));
            // Adjust for stroke width so border isn't clipped
            g2.drawRoundRect(borderThickness / 2, borderThickness / 2, 
                             getWidth() - borderThickness, getHeight() - borderThickness, 
                             arc, arc);
        }
        
        g2.dispose();
        super.paintComponent(g);
    }
}
