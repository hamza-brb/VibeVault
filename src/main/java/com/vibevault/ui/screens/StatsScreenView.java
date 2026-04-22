package com.vibevault.ui.screens;

import com.vibevault.ui.components.Theme;
import com.vibevault.ui.components.WeeklyBarChart;
import com.vibevault.ui.components.RoundedPanel;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;

public final class StatsScreenView {
    private StatsScreenView() {
    }

    public static JPanel build(
            JLabel statsTotalPlaysValueLabel,
            JLabel statsListeningValueLabel,
            JLabel statsTopArtistValueLabel,
            DefaultTableModel topSongsTableModel,
            DefaultTableModel topArtistsTableModel,
            DefaultTableModel recentPlaysTableModel,
            WeeklyBarChart weeklyBarChart,
            Consumer<JScrollPane> styleScrollPane,
            Consumer<JTable> styleTable
    ) {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(Theme.BG_DEEP);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topHeader = new JPanel(new BorderLayout(8, 0));
        topHeader.setOpaque(false);
        JLabel title = new JLabel("Your Stats");
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setFont(Theme.heading(22f));
        JComboBox<String> filter = new JComboBox<>(new String[]{"This Week", "This Month", "All Time"});
        topHeader.add(title, BorderLayout.WEST);
        topHeader.add(filter, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 80));
        cards.add(buildStatCard("Total Plays", statsTotalPlaysValueLabel));
        cards.add(buildStatCard("Listening Time", statsListeningValueLabel));
        cards.add(buildStatCard("Top Artist", statsTopArtistValueLabel));

        JPanel lists = new JPanel(new GridLayout(1, 2, 12, 12));
        lists.setOpaque(false);

        JTable topSongsStatsTable = new JTable(topSongsTableModel);
        JTable topArtistsStatsTable = new JTable(topArtistsTableModel);
        topSongsStatsTable.getTableHeader().setReorderingAllowed(false);
        topArtistsStatsTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane topSongsScroll = new JScrollPane(topSongsStatsTable);
        JScrollPane topArtistsScroll = new JScrollPane(topArtistsStatsTable);
        styleScrollPane.accept(topSongsScroll);
        styleScrollPane.accept(topArtistsScroll);
        topSongsScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BG_BORDER, 1, true),
                "Top 5 Songs"
        ));
        topArtistsScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.BG_BORDER, 1, true),
                "Top 5 Artists"
        ));
        topSongsStatsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        topSongsStatsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        topSongsStatsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        topArtistsStatsTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        topArtistsStatsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        topArtistsStatsTable.getColumnModel().getColumn(2).setPreferredWidth(70);

        lists.add(topSongsScroll);
        lists.add(topArtistsScroll);

        RoundedPanel weeklySection = new RoundedPanel(18, Theme.BG_SURFACE);
        weeklySection.setBorderConfig(Theme.BG_BORDER, 1);
        weeklySection.setLayout(new BorderLayout());
        weeklySection.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel weeklyTitle = new JLabel("Weekly Activity");
        weeklyTitle.setForeground(Theme.TEXT_PRIMARY);
        weeklyTitle.setFont(Theme.body(13f).deriveFont(java.awt.Font.BOLD));
        weeklySection.add(weeklyTitle, BorderLayout.NORTH);
        weeklyBarChart.setPreferredSize(new Dimension(0, 160));
        weeklySection.add(weeklyBarChart, BorderLayout.CENTER);

        RoundedPanel recentSection = new RoundedPanel(18, Theme.BG_SURFACE);
        recentSection.setBorderConfig(Theme.BG_BORDER, 1);
        recentSection.setLayout(new BorderLayout());
        recentSection.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel recentTitle = new JLabel("Recently Played");
        recentTitle.setForeground(Theme.TEXT_PRIMARY);
        recentTitle.setFont(Theme.heading(14f));
        recentSection.add(recentTitle, BorderLayout.NORTH);
        JTable recentStatsTable = new JTable(recentPlaysTableModel);
        styleTable.accept(recentStatsTable);
        JScrollPane recentScroll = new JScrollPane(recentStatsTable);
        recentScroll.setPreferredSize(new Dimension(0, 140));
        styleScrollPane.accept(recentScroll);
        recentSection.add(recentScroll, BorderLayout.CENTER);

        JPanel lowerSection = new JPanel(new GridLayout(2, 1, 0, 12));
        lowerSection.setOpaque(false);
        lowerSection.add(weeklySection);
        lowerSection.add(recentSection);

        RoundedPanel center = new RoundedPanel(22, Theme.BG_SURFACE);
        center.setBorderConfig(Theme.BG_BORDER, 1);
        center.setLayout(new BorderLayout(12, 12));
        center.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(lists, BorderLayout.CENTER);
        center.add(lowerSection, BorderLayout.SOUTH);

        JScrollPane mainScroll = new JScrollPane(center);
        styleScrollPane.accept(mainScroll);
        mainScroll.setBorder(null);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(mainScroll, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildStatCard(String labelText, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(Theme.BG_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(0xA2A89A));
        label.setFont(Theme.body(11f));
        valueLabel.setForeground(Theme.TEXT_PRIMARY);
        valueLabel.setFont(Theme.heading(18f));
        card.add(label, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }
}
