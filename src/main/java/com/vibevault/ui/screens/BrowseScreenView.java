package com.vibevault.ui.screens;

import com.vibevault.ui.components.RoundedButton;
import com.vibevault.ui.components.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BrowseScreenView {
    private BrowseScreenView() {
    }

    public static JPanel build(
            JPanel browseCardPanel,
            CardLayout browseCardLayout,
            JPanel browseGridPanel,
            JTable browseSongsTable,
            JLabel browseHeaderLabel,
            JLabel browseDetailLabel,
            String browseGridCard,
            String browseDetailCard,
            Function<String, RoundedButton> createPrimaryButton,
            Function<String, RoundedButton> createSecondaryButton,
            Consumer<JScrollPane> styleScrollPane,
            Runnable onPlayCurrentBrowseSongs
    ) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.BG_DEEP);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        browseHeaderLabel.setForeground(Theme.TEXT_PRIMARY);
        browseHeaderLabel.setFont(Theme.heading(22f));
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        togglePanel.setOpaque(false);
        JLabel browseHint = new JLabel("Artists in your library");
        browseHint.setForeground(Theme.TEXT_MUTED);
        browseHint.setFont(Theme.body(12f));
        togglePanel.add(browseHint);
        header.add(browseHeaderLabel, BorderLayout.WEST);
        header.add(togglePanel, BorderLayout.EAST);

        browseGridPanel.setBackground(Theme.BG_DEEP);
        JScrollPane gridScroll = new JScrollPane(browseGridPanel);
        styleScrollPane.accept(gridScroll);
        gridScroll.setBorder(BorderFactory.createLineBorder(Theme.BG_BORDER, 1, true));
        browseCardPanel.add(gridScroll, browseGridCard);

        JPanel detail = new JPanel(new BorderLayout(8, 8));
        detail.setBackground(Theme.BG_DEEP);
        RoundedButton backButton = createSecondaryButton.apply("← Back");
        backButton.addActionListener(e -> browseCardLayout.show(browseCardPanel, browseGridCard));
        RoundedButton playAllButton = createPrimaryButton.apply("▶ Play All");
        playAllButton.addActionListener(e -> onPlayCurrentBrowseSongs.run());
        JPanel detailTop = new JPanel(new BorderLayout());
        detailTop.setOpaque(false);
        browseDetailLabel.setForeground(Theme.TEXT_PRIMARY);
        browseDetailLabel.setFont(Theme.heading(16f));
        detailTop.add(backButton, BorderLayout.WEST);
        detailTop.add(browseDetailLabel, BorderLayout.CENTER);
        detailTop.add(playAllButton, BorderLayout.EAST);

        JScrollPane songsScroll = new JScrollPane(browseSongsTable);
        styleScrollPane.accept(songsScroll);

        detail.add(detailTop, BorderLayout.NORTH);
        detail.add(songsScroll, BorderLayout.CENTER);
        browseCardPanel.add(detail, browseDetailCard);

        panel.add(header, BorderLayout.NORTH);
        panel.add(browseCardPanel, BorderLayout.CENTER);
        return panel;
    }

    public static RoundedButton createArtistCard(
            String artistName,
            int songCount,
            Function<String, RoundedButton> createCardButton
    ) {
        int safeSongCount = Math.max(0, songCount);
        String tracksText = safeSongCount == 1 ? "1 track" : safeSongCount + " tracks";
        RoundedButton card = createCardButton.apply(
                "<html><div style='text-align:left;padding:10px 8px 8px 10px;'>"
                        + "<div style='font-size:17px;color:#1ED760;'>♫ " + initials(artistName) + "</div>"
                        + "<div style='font-size:13px;font-weight:700;color:#F5F5F5;'>"
                        + escapeHtml(artistName)
                        + "</div>"
                        + "<div style='font-size:10px;color:#B3B3B3;margin-top:2px;'>"
                        + tracksText
                        + "</div></div></html>"
        );
        card.setForeground(Theme.TEXT_PRIMARY);
        card.setFont(Theme.body(12f));
        card.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        card.setPreferredSize(new Dimension(180, 118));
        return card;
    }

    private static String initials(String text) {
        if (text == null || text.isBlank()) {
            return "AR";
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
