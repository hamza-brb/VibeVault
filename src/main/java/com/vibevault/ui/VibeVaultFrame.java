package com.vibevault.ui;

import com.vibevault.model.Playlist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import com.vibevault.service.AuthService;
import com.vibevault.service.LibraryService;
import com.vibevault.service.PlayerService;
import com.vibevault.service.PlaylistService;
import com.vibevault.service.StatsService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.List;

public class VibeVaultFrame extends JFrame {
    private static final String CARD_AUTH = "auth";
    private static final String CARD_DASHBOARD = "dashboard";

    private final AuthService authService;
    private final LibraryService libraryService;
    private final PlaylistService playlistService;
    private final PlayerService playerService;
    private final StatsService statsService;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    private final JLabel welcomeLabel = new JLabel("Welcome");
    private final JLabel summaryLabel = new JLabel("No data yet.");
    private final JLabel nowPlayingLabel = new JLabel("Now playing: -");

    private final JTextField titleField = new JTextField();
    private final JTextField artistField = new JTextField();
    private final JTextField albumField = new JTextField();
    private final JTextField genreField = new JTextField();
    private final JTextField sourceField = new JTextField();
    private final JTextField playlistNameField = new JTextField();
    private final JTextField listenedSecondsField = new JTextField();

    private final DefaultTableModel libraryTableModel = new DefaultTableModel(
            new Object[]{"Song ID", "Title", "Artist ID", "Album ID", "Genre", "Source"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel playlistsTableModel = new DefaultTableModel(
            new Object[]{"Playlist ID", "Name"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel playlistSongsTableModel = new DefaultTableModel(
            new Object[]{"Song ID", "Title", "Source"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel queueTableModel = new DefaultTableModel(
            new Object[]{"Queue Index", "Song ID", "Title", "Source"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel topSongsTableModel = new DefaultTableModel(
            new Object[]{"Song", "Plays", "Seconds"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel topArtistsTableModel = new DefaultTableModel(
            new Object[]{"Artist", "Plays", "Seconds"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel weeklyActivityTableModel = new DefaultTableModel(
            new Object[]{"Day", "Minutes"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel recentPlaysTableModel = new DefaultTableModel(
            new Object[]{"Played At", "Song", "Seconds"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable libraryTable = new JTable(libraryTableModel);
    private final JTable playlistsTable = new JTable(playlistsTableModel);
    private final JTable playlistSongsTable = new JTable(playlistSongsTableModel);
    private final JTable queueTable = new JTable(queueTableModel);
    private final JTable topSongsTable = new JTable(topSongsTableModel);
    private final JTable topArtistsTable = new JTable(topArtistsTableModel);
    private final JTable weeklyActivityTable = new JTable(weeklyActivityTableModel);
    private final JTable recentPlaysTable = new JTable(recentPlaysTableModel);

    private final JComboBox<PlayerService.RepeatMode> repeatModeCombo = new JComboBox<>(PlayerService.RepeatMode.values());
    private final JButton shuffleButton = new JButton("Shuffle: OFF");

    public VibeVaultFrame(
            AuthService authService,
            LibraryService libraryService,
            PlaylistService playlistService,
            PlayerService playerService,
            StatsService statsService
    ) {
        this.authService = authService;
        this.libraryService = libraryService;
        this.playlistService = playlistService;
        this.playerService = playerService;
        this.statsService = statsService;

        setTitle("VibeVault");
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(rootPanel);

        rootPanel.add(buildAuthPanel(), CARD_AUTH);
        rootPanel.add(buildDashboardPanel(), CARD_DASHBOARD);
        cardLayout.show(rootPanel, CARD_AUTH);
    }

    private JPanel buildAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 10));
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        actions.add(loginButton);
        actions.add(registerButton);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        panel.add(new JLabel("VibeVault Authentication"), BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());
        topBar.add(welcomeLabel, BorderLayout.WEST);
        topBar.add(logoutButton, BorderLayout.EAST);

        JPanel importForm = new JPanel(new GridLayout(0, 2, 8, 8));
        importForm.setBorder(BorderFactory.createTitledBorder("Import Song (Metadata First)"));
        importForm.add(new JLabel("Title"));
        importForm.add(titleField);
        importForm.add(new JLabel("Artist"));
        importForm.add(artistField);
        importForm.add(new JLabel("Album (optional)"));
        importForm.add(albumField);
        importForm.add(new JLabel("Genre (optional)"));
        importForm.add(genreField);
        importForm.add(new JLabel("Media Source URI"));
        importForm.add(sourceField);

        JButton importButton = new JButton("Import to Library");
        importButton.addActionListener(e -> importSong());

        JPanel playlistActions = new JPanel(new GridLayout(0, 1, 8, 8));
        playlistActions.setBorder(BorderFactory.createTitledBorder("Playlist Actions"));
        JButton createPlaylistButton = new JButton("Create Playlist");
        JButton addSongToPlaylistButton = new JButton("Add Selected Library Song");
        JButton loadPlaylistButton = new JButton("Load Selected Playlist to Queue");
        createPlaylistButton.addActionListener(e -> createPlaylist());
        addSongToPlaylistButton.addActionListener(e -> addSelectedLibrarySongToSelectedPlaylist());
        loadPlaylistButton.addActionListener(e -> loadSelectedPlaylistToQueue());
        playlistActions.add(new JLabel("Playlist Name"));
        playlistActions.add(playlistNameField);
        playlistActions.add(createPlaylistButton);
        playlistActions.add(addSongToPlaylistButton);
        playlistActions.add(loadPlaylistButton);

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(importForm);
        leftColumn.add(importButton);
        leftColumn.add(playlistActions);
        leftColumn.add(summaryLabel);

        JScrollPane libraryPane = new JScrollPane(libraryTable);
        libraryPane.setBorder(BorderFactory.createTitledBorder("My Library"));
        JScrollPane playlistsPane = new JScrollPane(playlistsTable);
        playlistsPane.setBorder(BorderFactory.createTitledBorder("My Playlists"));
        JScrollPane playlistSongsPane = new JScrollPane(playlistSongsTable);
        playlistSongsPane.setBorder(BorderFactory.createTitledBorder("Selected Playlist Songs"));

        JSplitPane playlistsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, playlistsPane, playlistSongsPane);
        playlistsSplit.setResizeWeight(0.45);

        JSplitPane mainCenterSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, libraryPane, playlistsSplit);
        mainCenterSplit.setResizeWeight(0.62);

        JPanel playerControls = new JPanel(new GridLayout(0, 2, 8, 8));
        playerControls.setBorder(BorderFactory.createTitledBorder("Player Controls"));
        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        JButton playSelectedQueueButton = new JButton("Play Selected Queue Row");
        JButton moveQueueUpButton = new JButton("Move Queue Row Up");
        JButton moveQueueDownButton = new JButton("Move Queue Row Down");
        JButton removeQueueRowButton = new JButton("Remove Queue Row");
        JButton logPlaybackButton = new JButton("Log Playback Seconds");
        previousButton.addActionListener(e -> playPrevious());
        nextButton.addActionListener(e -> playNext());
        playSelectedQueueButton.addActionListener(e -> playSelectedQueueRow());
        moveQueueUpButton.addActionListener(e -> moveSelectedQueueRow(-1));
        moveQueueDownButton.addActionListener(e -> moveSelectedQueueRow(1));
        removeQueueRowButton.addActionListener(e -> removeSelectedQueueRow());
        logPlaybackButton.addActionListener(e -> logPlayback());
        shuffleButton.addActionListener(e -> toggleShuffle());
        repeatModeCombo.addActionListener(e -> updateRepeatMode());
        listenedSecondsField.setText("30");

        playerControls.add(previousButton);
        playerControls.add(nextButton);
        playerControls.add(playSelectedQueueButton);
        playerControls.add(moveQueueUpButton);
        playerControls.add(moveQueueDownButton);
        playerControls.add(removeQueueRowButton);
        playerControls.add(shuffleButton);
        playerControls.add(new JLabel("Repeat Mode"));
        playerControls.add(repeatModeCombo);
        playerControls.add(new JLabel("Listened Seconds"));
        playerControls.add(listenedSecondsField);
        playerControls.add(logPlaybackButton);
        playerControls.add(nowPlayingLabel);

        JScrollPane queuePane = new JScrollPane(queueTable);
        queuePane.setBorder(BorderFactory.createTitledBorder("Playback Queue"));

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Stats Dashboard"));
        JScrollPane topSongsPane = new JScrollPane(topSongsTable);
        topSongsPane.setBorder(BorderFactory.createTitledBorder("Top Songs"));
        JScrollPane topArtistsPane = new JScrollPane(topArtistsTable);
        topArtistsPane.setBorder(BorderFactory.createTitledBorder("Top Artists"));
        JScrollPane weeklyPane = new JScrollPane(weeklyActivityTable);
        weeklyPane.setBorder(BorderFactory.createTitledBorder("Weekly Activity"));
        JScrollPane recentPane = new JScrollPane(recentPlaysTable);
        recentPane.setBorder(BorderFactory.createTitledBorder("Recently Played"));
        statsPanel.add(topSongsPane);
        statsPanel.add(topArtistsPane);
        statsPanel.add(weeklyPane);
        statsPanel.add(recentPane);

        JButton refreshStatsButton = new JButton("Refresh Stats");
        refreshStatsButton.addActionListener(e -> refreshLibraryAndStats(requireCurrentUser().getUserId()));

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.add(statsPanel, BorderLayout.WEST);
        bottomPanel.add(queuePane, BorderLayout.CENTER);
        bottomPanel.add(playerControls, BorderLayout.EAST);
        bottomPanel.add(refreshStatsButton, BorderLayout.SOUTH);

        playlistsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshSelectedPlaylistSongs();
            }
        });

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(leftColumn, BorderLayout.WEST);
        panel.add(mainCenterSplit, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void register() {
        try {
            authService.register(usernameField.getText(), new String(passwordField.getPassword()));
            JOptionPane.showMessageDialog(this, "Registration successful. You can now log in.");
            passwordField.setText("");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void login() {
        try {
            authService.login(usernameField.getText(), new String(passwordField.getPassword()))
                    .ifPresentOrElse(this::openDashboard, () -> showError("Invalid username or password"));
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void logout() {
        authService.logout();
        usernameField.setText("");
        passwordField.setText("");
        libraryTableModel.setRowCount(0);
        playlistsTableModel.setRowCount(0);
        playlistSongsTableModel.setRowCount(0);
        queueTableModel.setRowCount(0);
        topSongsTableModel.setRowCount(0);
        topArtistsTableModel.setRowCount(0);
        weeklyActivityTableModel.setRowCount(0);
        recentPlaysTableModel.setRowCount(0);
        summaryLabel.setText("No data yet.");
        nowPlayingLabel.setText("Now playing: -");
        playerService.clearQueue();
        cardLayout.show(rootPanel, CARD_AUTH);
    }

    private void openDashboard(User user) {
        welcomeLabel.setText("Welcome, " + user.getUsername());
        repeatModeCombo.setSelectedItem(PlayerService.RepeatMode.OFF);
        playerService.setRepeatMode(PlayerService.RepeatMode.OFF);
        refreshLibraryAndStats(user.getUserId());
        refreshPlaylists(user.getUserId());
        refreshQueueTable();
        cardLayout.show(rootPanel, CARD_DASHBOARD);
    }

    private void importSong() {
        User currentUser = requireCurrentUser();
        try {
            libraryService.importSong(currentUser.getUserId(), new LibraryService.SongImportRequest(
                    titleField.getText(),
                    artistField.getText(),
                    albumField.getText(),
                    genreField.getText(),
                    null,
                    sourceField.getText(),
                    null,
                    null
            ));
            clearImportFields();
            refreshLibraryAndStats(currentUser.getUserId());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void createPlaylist() {
        User currentUser = requireCurrentUser();
        try {
            playlistService.createPlaylist(currentUser.getUserId(), playlistNameField.getText());
            playlistNameField.setText("");
            refreshPlaylists(currentUser.getUserId());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void addSelectedLibrarySongToSelectedPlaylist() {
        User currentUser = requireCurrentUser();
        Integer playlistId = getSelectedPlaylistId();
        Integer songId = getSelectedLibrarySongId();
        if (playlistId == null || songId == null) {
            showError("Select both a playlist and a library song first");
            return;
        }
        try {
            playlistService.addSong(playlistId, songId);
            refreshSelectedPlaylistSongs();
            refreshLibraryAndStats(currentUser.getUserId());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadSelectedPlaylistToQueue() {
        Integer playlistId = getSelectedPlaylistId();
        if (playlistId == null) {
            showError("Select a playlist first");
            return;
        }
        try {
            List<Song> songs = playlistService.getPlaylistSongs(playlistId);
            playerService.setQueue(songs);
            refreshQueueTable();
            updateNowPlayingLabel();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void playSelectedQueueRow() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a queue row first");
            return;
        }
        int queueIndex = Integer.parseInt(queueTableModel.getValueAt(selectedRow, 0).toString());
        try {
            playerService.playAt(queueIndex);
            updateNowPlayingLabel();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void moveSelectedQueueRow(int delta) {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a queue row first");
            return;
        }
        int fromIndex = Integer.parseInt(queueTableModel.getValueAt(selectedRow, 0).toString());
        int toIndex = fromIndex + delta;
        if (toIndex < 0 || toIndex >= queueTableModel.getRowCount()) {
            return;
        }
        try {
            playerService.moveQueueItem(fromIndex, toIndex);
            refreshQueueTable();
            updateNowPlayingLabel();
            queueTable.setRowSelectionInterval(toIndex, toIndex);
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void removeSelectedQueueRow() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select a queue row first");
            return;
        }
        int queueIndex = Integer.parseInt(queueTableModel.getValueAt(selectedRow, 0).toString());
        try {
            playerService.removeQueueItem(queueIndex);
            refreshQueueTable();
            updateNowPlayingLabel();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void playPrevious() {
        playerService.previous();
        updateNowPlayingLabel();
    }

    private void playNext() {
        playerService.next();
        updateNowPlayingLabel();
    }

    private void toggleShuffle() {
        playerService.setShuffleEnabled(!playerService.isShuffleEnabled());
        shuffleButton.setText(playerService.isShuffleEnabled() ? "Shuffle: ON" : "Shuffle: OFF");
    }

    private void updateRepeatMode() {
        PlayerService.RepeatMode selected = (PlayerService.RepeatMode) repeatModeCombo.getSelectedItem();
        if (selected != null) {
            playerService.setRepeatMode(selected);
        }
    }

    private void logPlayback() {
        User currentUser = requireCurrentUser();
        try {
            int listenedSeconds = Integer.parseInt(listenedSecondsField.getText().trim());
            playerService.logCurrentSongPlayback(currentUser.getUserId(), listenedSeconds);
            refreshLibraryAndStats(currentUser.getUserId());
        } catch (NumberFormatException ex) {
            showError("Listened seconds must be a valid number");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshLibraryAndStats(int userId) {
        List<Song> songs = libraryService.getUserLibrarySongs(userId);
        libraryTableModel.setRowCount(0);
        for (Song song : songs) {
            libraryTableModel.addRow(new Object[]{
                    song.getSongId(),
                    song.getTitle(),
                    song.getArtistId(),
                    song.getAlbumId(),
                    song.getGenre(),
                    song.getFilePath()
            });
        }

        double totalMinutes = statsService.getTotalListeningMinutes(userId);
        String topSong = statsService.getTopSongs(userId, 1).stream()
                .findFirst()
                .map(StatsService.SongPlayStat::songTitle)
                .orElse("N/A");
        String favoriteAlbum = statsService.getFavoriteAlbum(userId)
                .map(StatsService.AlbumPlayStat::albumTitle)
                .orElse("N/A");
        summaryLabel.setText(
                "Songs: " + songs.size() +
                        " | Total listening minutes: " + totalMinutes +
                        " | Top song: " + topSong +
                        " | Favorite album: " + favoriteAlbum
        );

        refreshStatsTables(userId);
    }

    private void refreshStatsTables(int userId) {
        topSongsTableModel.setRowCount(0);
        for (StatsService.SongPlayStat stat : statsService.getTopSongs(userId, 5)) {
            topSongsTableModel.addRow(new Object[]{stat.songTitle(), stat.playCount(), stat.totalSeconds()});
        }

        topArtistsTableModel.setRowCount(0);
        for (StatsService.ArtistPlayStat stat : statsService.getTopArtists(userId, 5)) {
            topArtistsTableModel.addRow(new Object[]{stat.artistName(), stat.playCount(), stat.totalSeconds()});
        }

        weeklyActivityTableModel.setRowCount(0);
        for (StatsService.DailyListeningStat stat : statsService.getWeeklyActivity(userId)) {
            weeklyActivityTableModel.addRow(new Object[]{stat.day(), stat.totalMinutes()});
        }

        recentPlaysTableModel.setRowCount(0);
        for (StatsService.RecentPlay play : statsService.getRecentlyPlayed(userId, 10)) {
            recentPlaysTableModel.addRow(new Object[]{play.playedAt(), play.songTitle(), play.durationListened()});
        }
    }

    private void refreshPlaylists(int userId) {
        List<Playlist> playlists = playlistService.getUserPlaylists(userId);
        playlistsTableModel.setRowCount(0);
        for (Playlist playlist : playlists) {
            playlistsTableModel.addRow(new Object[]{playlist.getPlaylistId(), playlist.getName()});
        }
        playlistSongsTableModel.setRowCount(0);
    }

    private void refreshSelectedPlaylistSongs() {
        Integer playlistId = getSelectedPlaylistId();
        playlistSongsTableModel.setRowCount(0);
        if (playlistId == null) {
            return;
        }
        List<Song> songs = playlistService.getPlaylistSongs(playlistId);
        for (Song song : songs) {
            playlistSongsTableModel.addRow(new Object[]{song.getSongId(), song.getTitle(), song.getFilePath()});
        }
    }

    private void refreshQueueTable() {
        List<Song> queue = playerService.getQueueSnapshot();
        queueTableModel.setRowCount(0);
        for (int i = 0; i < queue.size(); i++) {
            Song song = queue.get(i);
            queueTableModel.addRow(new Object[]{i, song.getSongId(), song.getTitle(), song.getFilePath()});
        }
    }

    private void updateNowPlayingLabel() {
        String nowPlaying = playerService.getCurrentSong()
                .map(song -> song.getTitle() + " (" + song.getFilePath() + ")")
                .orElse("-");
        nowPlayingLabel.setText("Now playing: " + nowPlaying);
    }

    private Integer getSelectedPlaylistId() {
        int row = playlistsTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return Integer.parseInt(playlistsTableModel.getValueAt(row, 0).toString());
    }

    private Integer getSelectedLibrarySongId() {
        int row = libraryTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return Integer.parseInt(libraryTableModel.getValueAt(row, 0).toString());
    }

    private User requireCurrentUser() {
        return authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    private void clearImportFields() {
        titleField.setText("");
        artistField.setText("");
        albumField.setText("");
        genreField.setText("");
        sourceField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void launch(
            AuthService authService,
            LibraryService libraryService,
            PlaylistService playlistService,
            PlayerService playerService,
            StatsService statsService
    ) {
        SwingUtilities.invokeLater(() -> new VibeVaultFrame(
                authService,
                libraryService,
                playlistService,
                playerService,
                statsService
        ).setVisible(true));
    }
}
