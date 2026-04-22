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
import java.util.function.Consumer;
import java.util.function.Function;

public final class PlaylistsScreenView {
    private PlaylistsScreenView() {
    }

    public static JPanel build(
            JTable playlistScreenListTable,
            JTable playlistScreenSongsTable,
            JLabel playlistScreenTitleLabel,
            JLabel playlistScreenMetaLabel,
            Function<String, RoundedButton> createPrimaryButton,
            Function<String, RoundedButton> createSecondaryButton,
            Consumer<RoundedButton> applySymbolButtonFont,
            Consumer<JScrollPane> styleScrollPane,
            Runnable onCreatePlaylist,
            Runnable onPlaySelectedPlaylist,
            Runnable onAddSongsToSelectedPlaylist,
            Runnable onRenameSelectedPlaylist,
            Runnable onDeleteSelectedPlaylist,
            Runnable onMoveSongUp,
            Runnable onMoveSongDown,
            Runnable onRemoveSong,
            Runnable onRefreshPlaylistScreenDetail
    ) {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(Theme.BG_DEEP);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JScrollPane leftListScroll = new JScrollPane(playlistScreenListTable);
        styleScrollPane.accept(leftListScroll);

        RoundedPanel leftPanel = new RoundedPanel(22, Theme.BG_SURFACE);
        leftPanel.setBorderConfig(Theme.BG_BORDER, 1);
        leftPanel.setLayout(new BorderLayout(0, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel leftHeader = new JPanel(new BorderLayout(8, 0));
        leftHeader.setOpaque(false);
        JLabel playlistsLabel = new JLabel("Playlists");
        playlistsLabel.setForeground(Theme.TEXT_PRIMARY);
        playlistsLabel.setFont(Theme.heading(18f));
        RoundedButton createButton = createPrimaryButton.apply("+ New");
        applySymbolButtonFont.accept(createButton);
        createButton.setPreferredSize(new Dimension(80, 32));
        createButton.addActionListener(e -> onCreatePlaylist.run());
        leftHeader.add(playlistsLabel, BorderLayout.WEST);
        leftHeader.add(createButton, BorderLayout.EAST);
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        leftPanel.add(leftListScroll, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(260, 0));

        RoundedPanel right = new RoundedPanel(22, Theme.BG_SURFACE);
        right.setBorderConfig(Theme.BG_BORDER, 1);
        right.setLayout(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        playlistScreenTitleLabel.setForeground(Theme.TEXT_PRIMARY);
        playlistScreenTitleLabel.setFont(Theme.heading(20f));
        playlistScreenMetaLabel.setForeground(new java.awt.Color(0xA2A89A));
        playlistScreenMetaLabel.setFont(Theme.body(12f));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        RoundedButton playButton = createPrimaryButton.apply("▶ Play");
        RoundedButton addSongButton = createSecondaryButton.apply("+ Add Songs");
        RoundedButton renameButton = createSecondaryButton.apply("✏ Rename");
        RoundedButton deleteButton = createSecondaryButton.apply("🗑 Delete");
        applySymbolButtonFont.accept(playButton);
        applySymbolButtonFont.accept(addSongButton);
        applySymbolButtonFont.accept(renameButton);
        applySymbolButtonFont.accept(deleteButton);
        playButton.addActionListener(e -> onPlaySelectedPlaylist.run());
        addSongButton.addActionListener(e -> onAddSongsToSelectedPlaylist.run());
        renameButton.addActionListener(e -> onRenameSelectedPlaylist.run());
        deleteButton.addActionListener(e -> onDeleteSelectedPlaylist.run());
        actions.add(playButton);
        actions.add(addSongButton);
        actions.add(renameButton);
        actions.add(deleteButton);
        header.add(playlistScreenTitleLabel, BorderLayout.NORTH);
        header.add(playlistScreenMetaLabel, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        JScrollPane songsScroll = new JScrollPane(playlistScreenSongsTable);
        styleScrollPane.accept(songsScroll);

        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rowActions.setOpaque(false);
        RoundedButton moveUpButton = createSecondaryButton.apply("↑");
        RoundedButton moveDownButton = createSecondaryButton.apply("↓");
        RoundedButton removeButton = createSecondaryButton.apply("Remove from Playlist");
        applySymbolButtonFont.accept(moveUpButton);
        applySymbolButtonFont.accept(moveDownButton);
        moveUpButton.addActionListener(e -> onMoveSongUp.run());
        moveDownButton.addActionListener(e -> onMoveSongDown.run());
        removeButton.addActionListener(e -> onRemoveSong.run());
        rowActions.add(moveUpButton);
        rowActions.add(moveDownButton);
        rowActions.add(removeButton);

        right.add(header, BorderLayout.NORTH);
        right.add(songsScroll, BorderLayout.CENTER);
        right.add(rowActions, BorderLayout.SOUTH);

        playlistScreenListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onRefreshPlaylistScreenDetail.run();
            }
        });

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        return panel;
    }
}
