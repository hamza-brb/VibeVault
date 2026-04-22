package com.vibevault.ui.screens;

import com.vibevault.ui.components.RoundedButton;
import com.vibevault.ui.components.RoundedPanel;
import com.vibevault.ui.components.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LibraryScreenView {
    private LibraryScreenView() {
    }

    public static JPanel build(
            JTable libraryTable,
            JTable playlistsTable,
            JLabel librarySongCountLabel,
            Function<String, RoundedButton> createPrimaryButton,
            Function<String, RoundedButton> createSecondaryButton,
            Consumer<JScrollPane> styleScrollPane,
            Runnable onShowAddSongsDialog,
            Runnable onShowScanDialog,
            Runnable onRefreshSelectedPlaylistSongs
    ) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.setBackground(Theme.BG_DEEP);

        JPanel libraryHeader = new JPanel(new BorderLayout(8, 0));
        libraryHeader.setOpaque(false);
        JLabel songsTitle = new JLabel("Songs");
        songsTitle.setForeground(Theme.TEXT_PRIMARY);
        songsTitle.setFont(Theme.heading(22f));
        librarySongCountLabel.setForeground(Theme.TEXT_MUTED);
        librarySongCountLabel.setFont(Theme.body(12f));
        JPanel titlePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(songsTitle);
        titlePanel.add(librarySongCountLabel);

        RoundedButton addSongsButton = createPrimaryButton.apply("Add Songs");
        addSongsButton.setPreferredSize(new Dimension(110, 34));
        addSongsButton.addActionListener(e -> onShowAddSongsDialog.run());

        RoundedButton scanButton = createSecondaryButton.apply("Scan");
        scanButton.setPreferredSize(new Dimension(80, 34));
        scanButton.addActionListener(e -> onShowScanDialog.run());

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(addSongsButton);
        headerActions.add(scanButton);
        libraryHeader.add(titlePanel, BorderLayout.WEST);
        libraryHeader.add(headerActions, BorderLayout.EAST);

        JScrollPane libraryPane = new JScrollPane(libraryTable);
        styleScrollPane.accept(libraryPane);
        libraryPane.setBorder(null);

        RoundedPanel contentSurface = new RoundedPanel(28, Theme.BG_SURFACE);
        contentSurface.setBorderConfig(Theme.BG_BORDER, 1);
        contentSurface.setLayout(new BorderLayout(12, 12));
        contentSurface.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contentSurface.add(libraryHeader, BorderLayout.NORTH);
        contentSurface.add(libraryPane, BorderLayout.CENTER);

        playlistsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRefreshSelectedPlaylistSongs.run();
            }
        });

        panel.add(contentSurface, BorderLayout.CENTER);
        return panel;
    }
}
