package com.vibevault.ui.components;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SidebarButton extends JPanel {
    private final JLabel iconLabel;
    private final JLabel textLabel;
    private boolean active;

    public SidebarButton(String icon, String label, boolean active) {
        this.active = active;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 14, 12));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        setPreferredSize(new Dimension(220, 46));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));

        textLabel = new JLabel(label);
        add(Box.createHorizontalStrut(2));
        add(iconLabel);
        add(textLabel);
        setActive(active);
    }

    public void setActive(boolean active) {
        this.active = active;
        textLabel.setFont(Theme.body(active ? 14f : 13f).deriveFont(active ? Font.BOLD : Font.PLAIN));
        textLabel.setForeground(active ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
        iconLabel.setForeground(active ? Theme.ACCENT : Theme.TEXT_MUTED);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(active ? Theme.BG_HOVER : Theme.BG_DEEP);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        if (active) {
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(0, 8, 4, Math.max(12, getHeight() - 16), 4, 4);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
