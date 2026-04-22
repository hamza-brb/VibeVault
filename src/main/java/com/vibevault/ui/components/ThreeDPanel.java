package com.vibevault.ui.components;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BorderLayout;

public class ThreeDPanel extends JPanel {
    private final int arc;
    private final int depth = 20; // extra margin so the glow has room to breathe

    public ThreeDPanel(int arc, JPanel content) {
        this.arc = arc;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(content, BorderLayout.CENTER);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(depth, depth, depth, depth));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        int fx = depth;
        int fy = depth;
        int fw = w - depth * 2;
        int fh = h - depth * 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,     RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int glowRadius = depth;
        int glowSteps  = 30;
        for (int i = 1; i <= glowSteps; i++) {
            float t       = (float) i / glowSteps;
            float alpha_f = (float) Math.exp(-t * t * 5.7f);
            int   alpha   = (int) (alpha_f * 72);
            int   expand  = (int) (t * glowRadius);

            // Accent-themed green glow.
            g2.setColor(new Color(30, 215, 96, Math.max(1, alpha)));
            g2.fillRoundRect(
                    fx - expand,      fy - expand,
                    fw + expand * 2,  fh + expand * 2,
                    arc + expand * 2, arc + expand * 2);
        }

        Color faceLight = new Color(28, 28, 28, 240);
        Color faceDark  = new Color(14, 14, 14, 248);
        java.awt.LinearGradientPaint facePaint = new java.awt.LinearGradientPaint(
                fx, fy, fx, fy + fh,
                new float[]{ 0f, 1f },
                new Color[]{ faceLight, faceDark });
        g2.setPaint(facePaint);
        g2.fillRoundRect(fx, fy, fw, fh, arc, arc);

        java.awt.RadialGradientPaint spotPaint = new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(fx + fw * 0.85f, fy + fh * 0.55f),
                fw * 0.45f,
                new float[]{ 0f, 1f },
                new Color[]{ new Color(30, 215, 96, 38),
                        new Color(30, 215, 96, 0) });
        java.awt.Shape faceShape = new java.awt.geom.RoundRectangle2D.Float(fx, fy, fw, fh, arc, arc);
        g2.setClip(faceShape);
        g2.setPaint(spotPaint);
        g2.fillRoundRect(fx, fy, fw, fh, arc, arc);
        g2.setClip(null);

        int sheenH = fh / 4;
        java.awt.LinearGradientPaint sheenPaint = new java.awt.LinearGradientPaint(
                0, fy,           0, fy + sheenH,
                new float[]{ 0f, 0.6f, 1f },
                new Color[]{ new Color(255, 255, 255, 70),
                        new Color(255, 255, 255, 18),
                        new Color(255, 255, 255, 0) });
        g2.setClip(faceShape);
        g2.setPaint(sheenPaint);
        g2.fillRoundRect(fx + 2, fy + 2, fw - 4, sheenH, arc - 2, arc - 2);
        g2.setClip(null);

        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(46, 46, 46, 170));
        g2.drawRoundRect(fx, fy, fw, fh, arc, arc);

        g2.setStroke(new BasicStroke(0.9f));
        g2.setColor(new Color(30, 215, 96, 30));
        g2.drawRoundRect(fx + 1, fy + 1, fw - 2, fh - 2, arc - 1, arc - 1);

        g2.dispose();
        super.paintComponent(g);
    }
}
