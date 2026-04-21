package com.vibevault.ui;

import com.vibevault.model.Playlist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import com.vibevault.service.AuthService;
import com.vibevault.service.LibraryService;
import com.vibevault.service.PlayerService;
import com.vibevault.service.PlaylistService;
import com.vibevault.service.StatsService;
import com.vibevault.ui.components.DarkScrollBarUI;
import com.vibevault.ui.components.RoundedButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class VibeVaultFrame extends JFrame {
    private static final Color ABYSS = new Color(0x0D1321);
    private static final Color DEEP_NAVY = new Color(0x1A2A3A);
    private static final Color STEEL_BLUE = new Color(0x2E5077);
    private static final Color MUTED_BLUE = new Color(0x6B8FA8);
    private static final Color CREAM = new Color(0xEDE8D0);

    private static final String CARD_AUTH = "auth";
    private static final String CARD_DASHBOARD = "dashboard";
    private static final String CONTENT_LIBRARY = "content-library";
    private static final String CONTENT_BROWSE = "content-browse";
    private static final String CONTENT_PLAYLISTS = "content-playlists";
    private static final String CONTENT_STATS = "content-stats";
    private static final String BROWSE_GRID = "browse-grid";
    private static final String BROWSE_DETAIL = "browse-detail";

    private final AuthService authService;
    private final LibraryService libraryService;
    private final PlaylistService playlistService;
    private final PlayerService playerService;
    private final StatsService statsService;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);
    private final CardLayout contentCardLayout = new CardLayout();
    private final JPanel contentCardPanel = new JPanel(contentCardLayout);
    private final CardLayout browseCardLayout = new CardLayout();
    private final JPanel browseCardPanel = new JPanel(browseCardLayout);

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JLabel authErrorLabel = new JLabel(" ");
    private final JLabel authModeHintLabel = new JLabel("Login to continue");
    private RoundedButton authPrimaryButton;
    private RoundedButton authSecondaryButton;
    private boolean registerMode;

    private final JLabel welcomeLabel = new JLabel("Welcome");
    private final JLabel summaryLabel = new JLabel("No data yet.");
    private final JLabel nowPlayingLabel = new JLabel("Now playing: -");
    private final JLabel statsTotalPlaysValueLabel = new JLabel("0");
    private final JLabel statsListeningValueLabel = new JLabel("0.0 min");
    private final JLabel statsTopArtistValueLabel = new JLabel("N/A");

    private final JTextField titleField = new JTextField();
    private final JTextField artistField = new JTextField();
    private final JTextField albumField = new JTextField();
    private final JTextField genreField = new JTextField();
    private final JTextField sourceField = new JTextField();
    private final JTextField librarySearchField = new JTextField();
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
    private final TableRowSorter<DefaultTableModel> librarySorter = new TableRowSorter<>(libraryTableModel);
    private final JPanel browseGridPanel = new JPanel(new GridLayout(0, 4, 12, 12));
    private final JLabel browseHeaderLabel = new JLabel("Artists");
    private final JLabel browseDetailLabel = new JLabel("Detail");
    private final JPanel browseDetailAlbumsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final DefaultTableModel browseSongsTableModel = new DefaultTableModel(
            new Object[]{"Song ID", "Title", "Artist ID", "Album ID", "Duration"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable browseSongsTable = new JTable(browseSongsTableModel);
    private boolean browseAlbumsMode;
    private final DefaultTableModel playlistScreenListModel = new DefaultTableModel(new Object[]{"Playlist ID", "Name"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable playlistScreenListTable = new JTable(playlistScreenListModel);
    private final DefaultTableModel playlistScreenSongsModel = new DefaultTableModel(new Object[]{"#", "Song ID", "Title", "Source"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable playlistScreenSongsTable = new JTable(playlistScreenSongsModel);
    private final JLabel playlistScreenTitleLabel = new JLabel("Select a playlist");
    private final JLabel playlistScreenMetaLabel = new JLabel("0 songs");

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
        setMinimumSize(new Dimension(1000, 650));
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(rootPanel);
        rootPanel.setBackground(ABYSS);

        rootPanel.add(buildAuthPanel(), CARD_AUTH);
        rootPanel.add(buildDashboardPanel(), CARD_DASHBOARD);
        configureLibraryScreenInteractions();
        cardLayout.show(rootPanel, CARD_AUTH);
    }

    private JPanel buildAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ABYSS);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel card = new JPanel();
        card.setBackground(DEEP_NAVY);
        card.setPreferredSize(new Dimension(360, 380));
        card.setMaximumSize(new Dimension(360, 380));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x2E5077), 1, true),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("♫  VibeVault");
        title.setForeground(CREAM);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setAlignmentX(CENTER_ALIGNMENT);

        authModeHintLabel.setForeground(new Color(0xEDE8D0));
        authModeHintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authModeHintLabel.setAlignmentX(CENTER_ALIGNMENT);

        styleInputField(usernameField);
        styleInputField(passwordField);
        styleInputField(confirmPasswordField);
        applyPlaceholder(usernameField, "Enter username");
        applyPasswordPlaceholder(passwordField, "Enter password");
        applyPasswordPlaceholder(confirmPasswordField, "Confirm password");
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setMaximumSize(new Dimension(320, 38));
        confirmPasswordField.setPreferredSize(new Dimension(320, 38));

        usernameField.setMaximumSize(new Dimension(320, 38));
        passwordField.setMaximumSize(new Dimension(320, 38));

        authPrimaryButton = new RoundedButton("Login", 20, STEEL_BLUE, MUTED_BLUE, new Color(0x24415E));
        authPrimaryButton.setForeground(CREAM);
        authPrimaryButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        authPrimaryButton.setAlignmentX(CENTER_ALIGNMENT);
        authPrimaryButton.setPreferredSize(new Dimension(320, 38));
        authPrimaryButton.setMaximumSize(new Dimension(320, 38));
        authPrimaryButton.addActionListener(e -> handlePrimaryAuthAction());

        authSecondaryButton = new RoundedButton("Register", 20, ABYSS, DEEP_NAVY, ABYSS.darker());
        authSecondaryButton.setForeground(CREAM);
        authSecondaryButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        authSecondaryButton.setBorder(BorderFactory.createLineBorder(MUTED_BLUE, 1, true));
        authSecondaryButton.setAlignmentX(CENTER_ALIGNMENT);
        authSecondaryButton.setPreferredSize(new Dimension(320, 38));
        authSecondaryButton.setMaximumSize(new Dimension(320, 38));
        authSecondaryButton.addActionListener(e -> toggleRegisterMode());

        authErrorLabel.setForeground(new Color(0xC86B6B));
        authErrorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authErrorLabel.setAlignmentX(CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(authModeHintLabel);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(usernameField);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(confirmPasswordField);
        card.add(Box.createRigidArea(new Dimension(0, 16)));
        card.add(authPrimaryButton);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(authSecondaryButton);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(authErrorLabel);

        JPanel center = new JPanel();
        center.setBackground(ABYSS);
        center.add(card);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDashboardPanel() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(ABYSS);
        shell.add(buildTopBar(), BorderLayout.NORTH);
        shell.add(buildSidebar(), BorderLayout.WEST);
        contentCardPanel.setBackground(ABYSS);
        contentCardPanel.add(buildLibraryWorkspacePanel(), CONTENT_LIBRARY);
        contentCardPanel.add(buildBrowsePanel(), CONTENT_BROWSE);
        contentCardPanel.add(buildPlaylistScreenPanel(), CONTENT_PLAYLISTS);
        contentCardPanel.add(buildStatsScreenPanel(), CONTENT_STATS);
        shell.add(contentCardPanel, BorderLayout.CENTER);
        shell.add(buildNowPlayingBar(), BorderLayout.SOUTH);
        return shell;
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(ABYSS);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel leftNav = new JPanel();
        leftNav.setOpaque(false);
        leftNav.add(new JButton("←"));
        leftNav.add(new JButton("→"));

        JTextField globalSearch = new JTextField();
        styleInputField(globalSearch);
        globalSearch.setText("Search your library...");
        globalSearch.setForeground(new Color(0xA2A89A));
        globalSearch.setPreferredSize(new Dimension(400, 32));

        JButton statsButton = new JButton("Stats");
        statsButton.addActionListener(e -> {
            contentCardLayout.show(contentCardPanel, CONTENT_STATS);
            User currentUser = requireCurrentUser();
            refreshLibraryAndStats(currentUser.getUserId());
        });
        JButton userButton = new JButton("👤 User ▾");
        userButton.addActionListener(e -> showUserMenu(userButton));

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(statsButton);
        right.add(userButton);

        topBar.add(leftNav, BorderLayout.WEST);
        topBar.add(globalSearch, BorderLayout.CENTER);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(ABYSS);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 16)),
                BorderFactory.createEmptyBorder(14, 10, 14, 10)
        ));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JButton songsButton = new JButton("☰ Songs");
        songsButton.addActionListener(e -> contentCardLayout.show(contentCardPanel, CONTENT_LIBRARY));
        JButton artistsButton = new JButton("👤 Artists");
        artistsButton.addActionListener(e -> showBrowseArtists());
        JButton albumsButton = new JButton("💿 Albums");
        albumsButton.addActionListener(e -> showBrowseAlbums());
        JButton playlistsButton = new JButton("♪ Playlists");
        playlistsButton.addActionListener(e -> contentCardLayout.show(contentCardPanel, CONTENT_PLAYLISTS));
        JButton statsButton = new JButton("📊 Stats");
        statsButton.addActionListener(e -> contentCardLayout.show(contentCardPanel, CONTENT_STATS));

        sidebar.add(songsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(artistsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(albumsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(playlistsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(statsButton);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildNowPlayingBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(ABYSS);
        bar.setPreferredSize(new Dimension(0, 90));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 16)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel centerControls = new JPanel(new GridLayout(1, 4, 8, 0));
        centerControls.setOpaque(false);
        JButton previousButton = new JButton("⏮");
        JButton playButton = new JButton("⏯");
        JButton nextButton = new JButton("⏭");
        JButton shuffleToggle = new JButton("🔀");
        previousButton.addActionListener(e -> playPrevious());
        playButton.addActionListener(e -> {
            if (playerService.isPlaying()) {
                playerService.pause();
            } else {
                playerService.play();
            }
            updateNowPlayingLabel();
        });
        nextButton.addActionListener(e -> playNext());
        shuffleToggle.addActionListener(e -> toggleShuffle());
        centerControls.add(previousButton);
        centerControls.add(playButton);
        centerControls.add(nextButton);
        centerControls.add(shuffleToggle);

        bar.add(nowPlayingLabel, BorderLayout.WEST);
        bar.add(centerControls, BorderLayout.CENTER);
        return bar;
    }

    private JPanel buildPlaceholderPanel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DEEP_NAVY);
        JLabel label = new JLabel(text);
        label.setForeground(CREAM);
        label.setHorizontalAlignment(JLabel.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBrowsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DEEP_NAVY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        browseHeaderLabel.setForeground(CREAM);
        browseHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JButton artistsToggle = new JButton("Artists");
        JButton albumsToggle = new JButton("Albums");
        artistsToggle.addActionListener(e -> showBrowseArtists());
        albumsToggle.addActionListener(e -> showBrowseAlbums());
        JPanel togglePanel = new JPanel();
        togglePanel.setOpaque(false);
        togglePanel.add(artistsToggle);
        togglePanel.add(albumsToggle);
        header.add(browseHeaderLabel, BorderLayout.WEST);
        header.add(togglePanel, BorderLayout.EAST);

        browseGridPanel.setBackground(DEEP_NAVY);
        JScrollPane gridScroll = new JScrollPane(browseGridPanel);
        styleScrollPane(gridScroll);
        browseCardPanel.add(gridScroll, BROWSE_GRID);

        JPanel detail = new JPanel(new BorderLayout(8, 8));
        detail.setBackground(DEEP_NAVY);
        JButton backButton = new JButton("← Back");
        backButton.addActionListener(e -> browseCardLayout.show(browseCardPanel, BROWSE_GRID));
        JPanel detailTop = new JPanel(new BorderLayout());
        detailTop.setOpaque(false);
        browseDetailLabel.setForeground(CREAM);
        browseDetailLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        detailTop.add(backButton, BorderLayout.WEST);
        detailTop.add(browseDetailLabel, BorderLayout.CENTER);

        browseDetailAlbumsPanel.setBackground(DEEP_NAVY);
        JScrollPane albumsStripScroll = new JScrollPane(browseDetailAlbumsPanel);
        albumsStripScroll.setPreferredSize(new Dimension(0, 90));
        styleScrollPane(albumsStripScroll);

        JScrollPane songsScroll = new JScrollPane(browseSongsTable);
        styleScrollPane(songsScroll);

        detail.add(detailTop, BorderLayout.NORTH);
        detail.add(albumsStripScroll, BorderLayout.CENTER);
        detail.add(songsScroll, BorderLayout.SOUTH);
        browseCardPanel.add(detail, BROWSE_DETAIL);

        panel.add(header, BorderLayout.NORTH);
        panel.add(browseCardPanel, BorderLayout.CENTER);
        return panel;
    }

    private void showBrowseArtists() {
        browseAlbumsMode = false;
        browseHeaderLabel.setText("Artists");
        refreshBrowseGrid();
        contentCardLayout.show(contentCardPanel, CONTENT_BROWSE);
        browseCardLayout.show(browseCardPanel, BROWSE_GRID);
    }

    private void showBrowseAlbums() {
        browseAlbumsMode = true;
        browseHeaderLabel.setText("Albums");
        refreshBrowseGrid();
        contentCardLayout.show(contentCardPanel, CONTENT_BROWSE);
        browseCardLayout.show(browseCardPanel, BROWSE_GRID);
    }

    private void refreshBrowseGrid() {
        browseGridPanel.removeAll();
        User user = requireCurrentUser();
        if (!browseAlbumsMode) {
            List<LibraryService.ArtistLibrarySummary> artists = libraryService.getArtistBrowseSummaries(user.getUserId());
            for (LibraryService.ArtistLibrarySummary artist : artists) {
                JButton card = new JButton("<html><b>" + artist.artistName() + "</b><br/>" + artist.songCount() + " songs</html>");
                card.setPreferredSize(new Dimension(160, 120));
                card.addActionListener(e -> openArtistBrowseDetail(artist.artistId(), artist.artistName()));
                browseGridPanel.add(card);
            }
        } else {
            List<AlbumBrowseCard> albums = new ArrayList<>();
            List<LibraryService.ArtistLibrarySummary> artists = libraryService.getArtistBrowseSummaries(user.getUserId());
            for (LibraryService.ArtistLibrarySummary artist : artists) {
                for (LibraryService.AlbumLibrarySummary album : libraryService.getAlbumBrowseSummaries(user.getUserId(), artist.artistId())) {
                    albums.add(new AlbumBrowseCard(album.albumId(), album.albumTitle(), artist.artistName()));
                }
            }
            for (AlbumBrowseCard album : albums) {
                JButton card = new JButton("<html><b>" + album.albumTitle + "</b><br/>" + album.artistName + "</html>");
                card.setPreferredSize(new Dimension(160, 120));
                card.addActionListener(e -> openAlbumBrowseDetail(album.albumId, album.albumTitle, album.artistName));
                browseGridPanel.add(card);
            }
        }
        browseGridPanel.revalidate();
        browseGridPanel.repaint();
    }

    private void openArtistBrowseDetail(int artistId, String artistName) {
        User user = requireCurrentUser();
        browseDetailLabel.setText(artistName);
        browseDetailAlbumsPanel.removeAll();
        List<LibraryService.AlbumLibrarySummary> albums = libraryService.getAlbumBrowseSummaries(user.getUserId(), artistId);
        for (LibraryService.AlbumLibrarySummary album : albums) {
            JButton button = new JButton(album.albumTitle());
            button.addActionListener(e -> fillBrowseSongsTable(libraryService.getSongsByAlbumInUserLibrary(user.getUserId(), album.albumId())));
            browseDetailAlbumsPanel.add(button);
        }
        fillBrowseSongsTable(libraryService.getSongsByArtistInUserLibrary(user.getUserId(), artistId));
        browseDetailAlbumsPanel.revalidate();
        browseDetailAlbumsPanel.repaint();
        browseCardLayout.show(browseCardPanel, BROWSE_DETAIL);
    }

    private void openAlbumBrowseDetail(int albumId, String albumTitle, String artistName) {
        User user = requireCurrentUser();
        browseDetailLabel.setText(albumTitle + " — " + artistName);
        browseDetailAlbumsPanel.removeAll();
        fillBrowseSongsTable(libraryService.getSongsByAlbumInUserLibrary(user.getUserId(), albumId));
        browseDetailAlbumsPanel.revalidate();
        browseDetailAlbumsPanel.repaint();
        browseCardLayout.show(browseCardPanel, BROWSE_DETAIL);
    }

    private void fillBrowseSongsTable(List<Song> songs) {
        browseSongsTableModel.setRowCount(0);
        for (Song song : songs) {
            browseSongsTableModel.addRow(new Object[]{
                    song.getSongId(),
                    song.getTitle(),
                    song.getArtistId(),
                    song.getAlbumId(),
                    song.getDurationSeconds()
            });
        }
    }

    private JPanel buildPlaylistScreenPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DEEP_NAVY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane leftListScroll = new JScrollPane(playlistScreenListTable);
        leftListScroll.setPreferredSize(new Dimension(260, 0));
        styleScrollPane(leftListScroll);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        playlistScreenTitleLabel.setForeground(CREAM);
        playlistScreenTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        playlistScreenMetaLabel.setForeground(new Color(0xA2A89A));

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton playButton = new JButton("▶ Play");
        JButton renameButton = new JButton("✏ Rename");
        JButton deleteButton = new JButton("🗑 Delete");
        playButton.addActionListener(e -> playSelectedPlaylistScreen());
        renameButton.addActionListener(e -> renameSelectedPlaylistScreen());
        deleteButton.addActionListener(e -> deleteSelectedPlaylistScreen());
        actions.add(playButton);
        actions.add(renameButton);
        actions.add(deleteButton);
        header.add(playlistScreenTitleLabel, BorderLayout.NORTH);
        header.add(playlistScreenMetaLabel, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        JScrollPane songsScroll = new JScrollPane(playlistScreenSongsTable);
        styleScrollPane(songsScroll);

        JPanel rowActions = new JPanel();
        rowActions.setOpaque(false);
        JButton moveUpButton = new JButton("Move Up");
        JButton moveDownButton = new JButton("Move Down");
        JButton removeButton = new JButton("Remove from Playlist");
        moveUpButton.addActionListener(e -> moveSelectedPlaylistSong(-1));
        moveDownButton.addActionListener(e -> moveSelectedPlaylistSong(1));
        removeButton.addActionListener(e -> removeSelectedPlaylistSong());
        rowActions.add(moveUpButton);
        rowActions.add(moveDownButton);
        rowActions.add(removeButton);

        right.add(header, BorderLayout.NORTH);
        right.add(songsScroll, BorderLayout.CENTER);
        right.add(rowActions, BorderLayout.SOUTH);

        playlistScreenListTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshPlaylistScreenDetail();
            }
        });

        panel.add(leftListScroll, BorderLayout.WEST);
        panel.add(right, BorderLayout.CENTER);
        return panel;
    }

    private Integer getSelectedPlaylistScreenId() {
        int row = playlistScreenListTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return Integer.parseInt(playlistScreenListModel.getValueAt(row, 0).toString());
    }

    private void refreshPlaylistScreenList(List<Playlist> playlists) {
        playlistScreenListModel.setRowCount(0);
        for (Playlist playlist : playlists) {
            playlistScreenListModel.addRow(new Object[]{playlist.getPlaylistId(), playlist.getName()});
        }
        refreshPlaylistScreenDetail();
    }

    private void refreshPlaylistScreenDetail() {
        Integer playlistId = getSelectedPlaylistScreenId();
        playlistScreenSongsModel.setRowCount(0);
        if (playlistId == null) {
            playlistScreenTitleLabel.setText("Select a playlist");
            playlistScreenMetaLabel.setText("0 songs");
            return;
        }

        String playlistName = playlistScreenListModel.getValueAt(playlistScreenListTable.getSelectedRow(), 1).toString();
        List<Song> songs = playlistService.getPlaylistSongs(playlistId);
        playlistScreenTitleLabel.setText(playlistName);
        playlistScreenMetaLabel.setText(songs.size() + " songs");
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            playlistScreenSongsModel.addRow(new Object[]{i + 1, song.getSongId(), song.getTitle(), song.getFilePath()});
        }
    }

    private void playSelectedPlaylistScreen() {
        Integer playlistId = getSelectedPlaylistScreenId();
        if (playlistId == null) {
            return;
        }
        List<Song> songs = playlistService.getPlaylistSongs(playlistId);
        playerService.setQueue(songs);
        if (!songs.isEmpty()) {
            playerService.playAt(0);
        }
        refreshQueueTable();
        updateNowPlayingLabel();
    }

    private void renameSelectedPlaylistScreen() {
        Integer playlistId = getSelectedPlaylistScreenId();
        if (playlistId == null) {
            return;
        }
        String currentName = playlistScreenListModel.getValueAt(playlistScreenListTable.getSelectedRow(), 1).toString();
        String newName = JOptionPane.showInputDialog(this, "Rename playlist", currentName);
        if (newName == null) {
            return;
        }
        playlistService.renamePlaylist(playlistId, newName);
        refreshPlaylists(requireCurrentUser().getUserId());
    }

    private void deleteSelectedPlaylistScreen() {
        Integer playlistId = getSelectedPlaylistScreenId();
        if (playlistId == null) {
            return;
        }
        String playlistName = playlistScreenListModel.getValueAt(playlistScreenListTable.getSelectedRow(), 1).toString();
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete playlist '" + playlistName + "'? This cannot be undone.",
                "Delete Playlist",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        playlistService.deletePlaylist(playlistId);
        refreshPlaylists(requireCurrentUser().getUserId());
    }

    private void moveSelectedPlaylistSong(int delta) {
        Integer playlistId = getSelectedPlaylistScreenId();
        int row = playlistScreenSongsTable.getSelectedRow();
        if (playlistId == null || row < 0) {
            return;
        }
        int currentPosition = Integer.parseInt(playlistScreenSongsModel.getValueAt(row, 0).toString());
        int targetPosition = currentPosition + delta;
        if (targetPosition < 1 || targetPosition > playlistScreenSongsModel.getRowCount()) {
            return;
        }
        int songId = Integer.parseInt(playlistScreenSongsModel.getValueAt(row, 1).toString());
        playlistService.moveSong(playlistId, songId, targetPosition);
        refreshPlaylistScreenDetail();
    }

    private void removeSelectedPlaylistSong() {
        Integer playlistId = getSelectedPlaylistScreenId();
        int row = playlistScreenSongsTable.getSelectedRow();
        if (playlistId == null || row < 0) {
            return;
        }
        int songId = Integer.parseInt(playlistScreenSongsModel.getValueAt(row, 1).toString());
        playlistService.removeSong(playlistId, songId);
        refreshPlaylistScreenDetail();
    }

    private JPanel buildStatsScreenPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DEEP_NAVY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topHeader = new JPanel(new BorderLayout(8, 0));
        topHeader.setOpaque(false);
        JLabel title = new JLabel("Your Stats");
        title.setForeground(CREAM);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JComboBox<String> filter = new JComboBox<>(new String[]{"This Week", "This Month", "All Time"});
        topHeader.add(title, BorderLayout.WEST);
        topHeader.add(filter, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 10));
        cards.setOpaque(false);
        cards.add(buildStatCard("Total Plays", statsTotalPlaysValueLabel));
        cards.add(buildStatCard("Listening Time", statsListeningValueLabel));
        cards.add(buildStatCard("Top Artist", statsTopArtistValueLabel));

        JPanel weeklySection = new JPanel(new BorderLayout());
        weeklySection.setOpaque(false);
        JLabel weeklyTitle = new JLabel("Weekly Activity");
        weeklyTitle.setForeground(CREAM);
        weeklySection.add(weeklyTitle, BorderLayout.NORTH);
        JTable weeklyTable = new JTable(weeklyActivityTableModel);
        JScrollPane weeklyScroll = new JScrollPane(weeklyTable);
        styleScrollPane(weeklyScroll);
        weeklySection.add(weeklyScroll, BorderLayout.CENTER);

        JTable topSongsStatsTable = new JTable(topSongsTableModel);
        JTable topArtistsStatsTable = new JTable(topArtistsTableModel);
        JTable recentStatsTable = new JTable(recentPlaysTableModel);
        JScrollPane topSongsScroll = new JScrollPane(topSongsStatsTable);
        JScrollPane topArtistsScroll = new JScrollPane(topArtistsStatsTable);
        JScrollPane recentScroll = new JScrollPane(recentStatsTable);
        styleScrollPane(topSongsScroll);
        styleScrollPane(topArtistsScroll);
        styleScrollPane(recentScroll);
        topSongsScroll.setBorder(BorderFactory.createTitledBorder("Top 5 Songs"));
        topArtistsScroll.setBorder(BorderFactory.createTitledBorder("Top 5 Artists"));
        recentScroll.setBorder(BorderFactory.createTitledBorder("Recently Played"));

        JPanel lists = new JPanel(new GridLayout(1, 2, 10, 10));
        lists.setOpaque(false);
        lists.add(topSongsScroll);
        lists.add(topArtistsScroll);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(weeklySection, BorderLayout.CENTER);
        center.add(lists, BorderLayout.SOUTH);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(recentScroll, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildStatCard(String labelText, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(DEEP_NAVY);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STEEL_BLUE, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(0xA2A89A));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        valueLabel.setForeground(CREAM);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        card.add(label, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLibraryWorkspacePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(DEEP_NAVY);

        JPanel libraryHeader = new JPanel(new BorderLayout(8, 0));
        libraryHeader.setOpaque(false);
        JLabel songsTitle = new JLabel("Songs");
        songsTitle.setForeground(CREAM);
        songsTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        styleInputField(librarySearchField);
        librarySearchField.setPreferredSize(new Dimension(260, 34));
        libraryHeader.add(songsTitle, BorderLayout.WEST);
        libraryHeader.add(librarySearchField, BorderLayout.EAST);

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
        JButton importFromFileButton = new JButton("Import from Audio File");
        importButton.addActionListener(e -> importSong());
        importFromFileButton.addActionListener(e -> importSongFromAudioFile());

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
        leftColumn.add(importFromFileButton);
        leftColumn.add(playlistActions);
        leftColumn.add(summaryLabel);

        JScrollPane libraryPane = new JScrollPane(libraryTable);
        libraryPane.setBorder(BorderFactory.createTitledBorder("My Library"));
        JScrollPane playlistsPane = new JScrollPane(playlistsTable);
        playlistsPane.setBorder(BorderFactory.createTitledBorder("My Playlists"));
        JScrollPane playlistSongsPane = new JScrollPane(playlistSongsTable);
        playlistSongsPane.setBorder(BorderFactory.createTitledBorder("Selected Playlist Songs"));
        styleScrollPane(libraryPane);
        styleScrollPane(playlistsPane);
        styleScrollPane(playlistSongsPane);

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
        styleScrollPane(queuePane);

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
        styleScrollPane(topSongsPane);
        styleScrollPane(topArtistsPane);
        styleScrollPane(weeklyPane);
        styleScrollPane(recentPane);
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

        panel.add(libraryHeader, BorderLayout.NORTH);
        panel.add(leftColumn, BorderLayout.WEST);
        panel.add(mainCenterSplit, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void register() {
        try {
            String password = readPasswordInput(passwordField);
            String confirmPassword = readPasswordInput(confirmPasswordField);
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            authService.register(readTextInput(usernameField), password);
            authErrorLabel.setText("Registration successful. Please login.");
            registerMode = false;
            updateAuthMode();
            resetAuthPlaceholders();
        } catch (RuntimeException ex) {
            authErrorLabel.setText(ex.getMessage());
        }
    }

    private void login() {
        try {
            authService.login(readTextInput(usernameField), readPasswordInput(passwordField))
                    .ifPresentOrElse(user -> {
                        authErrorLabel.setText(" ");
                        openDashboard(user);
                    }, () -> authErrorLabel.setText("Invalid username or password"));
        } catch (RuntimeException ex) {
            authErrorLabel.setText(ex.getMessage());
        }
    }

    private void handlePrimaryAuthAction() {
        if (registerMode) {
            register();
        } else {
            login();
        }
    }

    private void toggleRegisterMode() {
        registerMode = !registerMode;
        updateAuthMode();
        authErrorLabel.setText(" ");
        resetAuthPlaceholders();
    }

    private void updateAuthMode() {
        confirmPasswordField.setVisible(registerMode);
        authPrimaryButton.setText(registerMode ? "Create Account" : "Login");
        authSecondaryButton.setText(registerMode ? "Back to Login" : "Register");
        authModeHintLabel.setText(registerMode ? "Create your account" : "Login to continue");
        revalidate();
        repaint();
    }

    private void logout() {
        authService.logout();
        playerService.setActiveUserId(null);
        registerMode = false;
        updateAuthMode();
        authErrorLabel.setText(" ");
        resetAuthPlaceholders();
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
        playerService.setActiveUserId(user.getUserId());
        repeatModeCombo.setSelectedItem(PlayerService.RepeatMode.OFF);
        playerService.setRepeatMode(PlayerService.RepeatMode.OFF);
        contentCardLayout.show(contentCardPanel, CONTENT_LIBRARY);
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

    private void importSongFromAudioFile() {
        User currentUser = requireCurrentUser();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Audio File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Audio Files (*.mp3, *.wav)", "mp3", "wav"));
        int selection = fileChooser.showOpenDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
            showError("No file selected");
            return;
        }

        try {
            libraryService.importSongFromFile(currentUser.getUserId(), selectedFile.toPath());
            refreshLibraryAndStats(currentUser.getUserId());
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
        statsTotalPlaysValueLabel.setText(String.valueOf(statsService.getTotalPlayCount(userId)));
        statsListeningValueLabel.setText(statsService.getTotalListeningMinutes(userId) + " min");
        String topArtist = statsService.getTopArtists(userId, 1).stream()
                .findFirst()
                .map(StatsService.ArtistPlayStat::artistName)
                .orElse("N/A");
        statsTopArtistValueLabel.setText(topArtist);

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
        refreshPlaylistScreenList(playlists);
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
        int modelRow = libraryTable.convertRowIndexToModel(row);
        return Integer.parseInt(libraryTableModel.getValueAt(modelRow, 0).toString());
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

    private void styleInputField(JTextField field) {
        field.setBackground(DEEP_NAVY);
        field.setForeground(CREAM);
        field.setCaretColor(MUTED_BLUE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STEEL_BLUE, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void applyPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty("placeholder", placeholder);
        restorePlaceholderIfEmpty(field);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isPlaceholderActive(field)) {
                    field.setText("");
                    field.setForeground(CREAM);
                    field.putClientProperty("placeholder-active", false);
                    if (field instanceof JPasswordField passwordField) {
                        Character defaultEcho = (Character) passwordField.getClientProperty("default-echo-char");
                        if (defaultEcho != null) {
                            passwordField.setEchoChar(defaultEcho);
                        }
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                restorePlaceholderIfEmpty(field);
            }
        });
    }

    private void applyPasswordPlaceholder(JPasswordField field, String placeholder) {
        field.putClientProperty("default-echo-char", field.getEchoChar());
        applyPlaceholder(field, placeholder);
    }

    private void restorePlaceholderIfEmpty(JTextField field) {
        String text = field.getText();
        if (text != null && !text.isBlank()) {
            return;
        }
        String placeholder = (String) field.getClientProperty("placeholder");
        if (placeholder == null) {
            return;
        }
        field.setText(placeholder);
        field.setForeground(new Color(0xA2A89A));
        field.putClientProperty("placeholder-active", true);
        if (field instanceof JPasswordField passwordField) {
            passwordField.setEchoChar((char) 0);
        }
    }

    private boolean isPlaceholderActive(JTextField field) {
        return Boolean.TRUE.equals(field.getClientProperty("placeholder-active"));
    }

    private String readTextInput(JTextField field) {
        return isPlaceholderActive(field) ? "" : field.getText();
    }

    private String readPasswordInput(JPasswordField field) {
        return isPlaceholderActive(field) ? "" : new String(field.getPassword());
    }

    private void resetAuthPlaceholders() {
        usernameField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        restorePlaceholderIfEmpty(usernameField);
        restorePlaceholderIfEmpty(passwordField);
        restorePlaceholderIfEmpty(confirmPasswordField);
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.getViewport().setBackground(ABYSS);
        scrollPane.setBackground(ABYSS);
        DarkScrollBarUI.apply(scrollPane.getVerticalScrollBar());
    }

    private void configureLibraryScreenInteractions() {
        libraryTable.setRowSorter(librarySorter);
        librarySearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyLibrarySearchFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyLibrarySearchFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyLibrarySearchFilter();
            }
        });

        libraryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    Integer songId = getSelectedLibrarySongId();
                    if (songId != null) {
                        playSongFromLibrary(songId);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowLibraryPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowLibraryPopup(e);
            }
        });
    }

    private void applyLibrarySearchFilter() {
        String query = librarySearchField.getText() == null ? "" : librarySearchField.getText().trim();
        if (query.isEmpty()) {
            librarySorter.setRowFilter(null);
            return;
        }
        librarySorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 1, 2, 4, 5));
    }

    private void maybeShowLibraryPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = libraryTable.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        libraryTable.setRowSelectionInterval(row, row);
        Integer songId = getSelectedLibrarySongId();
        if (songId == null) {
            return;
        }

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem playItem = new JMenuItem("Play");
        playItem.addActionListener(evt -> playSongFromLibrary(songId));
        popupMenu.add(playItem);

        JMenu addToPlaylistMenu = new JMenu("Add to Playlist");
        User currentUser = requireCurrentUser();
        List<Playlist> playlists = playlistService.getUserPlaylists(currentUser.getUserId());
        for (Playlist playlist : playlists) {
            JMenuItem item = new JMenuItem(playlist.getName());
            item.addActionListener(evt -> {
                playlistService.addSong(playlist.getPlaylistId(), songId);
                refreshSelectedPlaylistSongs();
            });
            addToPlaylistMenu.add(item);
        }
        popupMenu.add(addToPlaylistMenu);

        JMenuItem removeItem = new JMenuItem("Remove from Library");
        removeItem.addActionListener(evt -> {
            libraryService.removeSongFromUserLibrary(currentUser.getUserId(), songId);
            refreshLibraryAndStats(currentUser.getUserId());
        });
        popupMenu.add(removeItem);

        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void playSongFromLibrary(int songId) {
        User currentUser = requireCurrentUser();
        List<Song> songs = libraryService.getUserLibrarySongs(currentUser.getUserId());
        if (songs.isEmpty()) {
            return;
        }
        playerService.setQueue(songs);
        int queueIndex = IntStream.range(0, songs.size())
                .filter(i -> songs.get(i).getSongId() == songId)
                .findFirst()
                .orElse(0);
        playerService.playAt(queueIndex);
        refreshQueueTable();
        updateNowPlayingLabel();
    }

    private void showUserMenu(JButton anchor) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem header = new JMenuItem("👤 " + requireCurrentUser().getUsername());
        header.setEnabled(false);
        JMenuItem preferences = new JMenuItem("⚙ Preferences");
        preferences.addActionListener(e -> JOptionPane.showMessageDialog(this, "Preferences are not implemented in MVP."));
        JMenuItem logout = new JMenuItem("🚪 Log Out");
        logout.addActionListener(e -> logout());
        menu.add(header);
        menu.addSeparator();
        menu.add(preferences);
        menu.add(logout);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class AlbumBrowseCard {
        private final int albumId;
        private final String albumTitle;
        private final String artistName;

        private AlbumBrowseCard(int albumId, String albumTitle, String artistName) {
            this.albumId = albumId;
            this.albumTitle = albumTitle;
            this.artistName = artistName;
        }
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
