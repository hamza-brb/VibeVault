package com.vibevault.ui;

import com.vibevault.model.Playlist;
import com.vibevault.model.Song;
import com.vibevault.model.User;
import com.vibevault.service.AuthService;
import com.vibevault.service.LibraryScanService;
import com.vibevault.service.LibraryService;
import com.vibevault.service.PlayerService;
import com.vibevault.service.PlaylistService;
import com.vibevault.service.StatsService;
import com.vibevault.ui.components.AccentSliderUI;
import com.vibevault.ui.components.CircleAvatarLabel;
import com.vibevault.ui.components.DarkScrollBarUI;
import com.vibevault.ui.components.RoundedButton;
import com.vibevault.ui.components.RoundedBorder;
import com.vibevault.ui.components.RoundedPanel;
import com.vibevault.ui.components.SidebarButton;
import com.vibevault.ui.components.Theme;
import com.vibevault.ui.components.ThreeDPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.DefaultListModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridLayout;
import java.io.File;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;

public class VibeVaultFrame extends JFrame {
    private static final Color ABYSS = Theme.BG_DEEP;
    private static final Color DEEP_NAVY = Theme.BG_SURFACE;
    private static final Color STEEL_BLUE = Theme.BG_BORDER;
    private static final Color MUTED_BLUE = Theme.ACCENT;
    private static final Color CREAM = Theme.TEXT_PRIMARY;

    private static final String CARD_AUTH = "auth";
    private static final String CARD_DASHBOARD = "dashboard";
    private static final String CONTENT_LIBRARY = "content-library";
    private static final String CONTENT_BROWSE = "content-browse";
    private static final String CONTENT_PLAYLISTS = "content-playlists";
    private static final String CONTENT_STATS = "content-stats";
    private static final String CONTENT_SEARCH = "content-search";
    private static final String BROWSE_GRID = "browse-grid";
    private static final String BROWSE_DETAIL = "browse-detail";

    private final AuthService authService;
    private final LibraryService libraryService;
    private final PlaylistService playlistService;
    private final PlayerService playerService;
    private final StatsService statsService;
    private final LibraryScanService libraryScanService;

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
    private final JLabel nowPlayingLabel = new JLabel("Nothing playing");
    private final JLabel nowPlayingArtistLabel = new JLabel("Choose a song from your library");
    private final JLabel elapsedTimeLabel = new JLabel("0:00");
    private final JLabel totalTimeLabel = new JLabel("0:00");
    private final JLabel librarySongCountLabel = new JLabel("0 songs");
    private final RoundedButton playButton = createIconButton("▶");
    private final RoundedButton repeatButton = createIconButton("🔁");
    private final RoundedButton shuffleToggle = createIconButton("🔀");
    private final JSlider seekSlider = new JSlider(0, 1000, 0);
    private final JSlider volumeSlider = new JSlider(0, 100, 70);
    private final CircleAvatarLabel nowPlayingAvatar = new CircleAvatarLabel("VibeVault");
    private final JLabel statsTotalPlaysValueLabel = new JLabel("0");
    private final JLabel statsListeningValueLabel = new JLabel("0.0 min");
    private final JLabel statsTopArtistValueLabel = new JLabel("N/A");

    private final JTextField titleField = new JTextField();
    private final JTextField artistField = new JTextField();
    private final JTextField sourceField = new JTextField();
    private final JTextField globalSearchField = new JTextField();
    private final JTextField librarySearchField = new JTextField();
    private final JTextField playlistNameField = new JTextField();

    private final DefaultTableModel libraryTableModel = new DefaultTableModel(
            new Object[]{"#", "Title", "Artist", "Duration"}, 0
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
    private final DefaultTableModel searchSongsTableModel = new DefaultTableModel(
            new Object[]{"Song ID", "Title", "Artist", "Duration"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel searchArtistsTableModel = new DefaultTableModel(
            new Object[]{"Artist ID", "Artist", "Songs"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel searchPlaylistsTableModel = new DefaultTableModel(
            new Object[]{"Playlist ID", "Playlist", "Songs"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable searchSongsTable = new JTable(searchSongsTableModel);
    private final JTable searchArtistsTable = new JTable(searchArtistsTableModel);
    private final JTable searchPlaylistsTable = new JTable(searchPlaylistsTableModel);
    private final JLabel searchResultsLabel = new JLabel("Search results");
    private final JPanel browseGridPanel = new JPanel(new GridLayout(0, 5, 10, 10));
    private final JLabel browseHeaderLabel = new JLabel("Artists");
    private final JLabel browseDetailLabel = new JLabel("Detail");
    private final DefaultTableModel browseSongsTableModel = new DefaultTableModel(
            new Object[]{"#", "Title", "Artist", "Duration"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable browseSongsTable = new JTable(browseSongsTableModel);
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
    private final WeeklyBarChart weeklyBarChart = new WeeklyBarChart();
    private final Map<String, SidebarButton> sidebarButtons = new HashMap<>();
    private final Map<Integer, String> artistNameCache = new HashMap<>();
    private final List<Integer> browseSongIds = new ArrayList<>();
    private List<Song> currentBrowseSongs = List.of();
    private String activeContentCard = CONTENT_LIBRARY;
    private String lastNonSearchContentCard = CONTENT_LIBRARY;
    private final javax.swing.Timer playbackUiTimer;
    private boolean seekSliderInternalUpdate;

    public VibeVaultFrame(
            AuthService authService,
            LibraryService libraryService,
            PlaylistService playlistService,
            PlayerService playerService,
            StatsService statsService,
            LibraryScanService libraryScanService
    ) {
        this.authService = authService;
        this.libraryService = libraryService;
        this.playlistService = playlistService;
        this.playerService = playerService;
        this.statsService = statsService;
        this.libraryScanService = libraryScanService;

        setTitle("VibeVault");
        setMinimumSize(new Dimension(1100, 680));
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(rootPanel);
        rootPanel.setBackground(ABYSS);

        styleAllTables();
        stylePlaybackControls();
        rootPanel.add(buildAuthPanel(), CARD_AUTH);
        rootPanel.add(buildDashboardPanel(), CARD_DASHBOARD);
        configureLibraryScreenInteractions();
        configureBrowseScreenInteractions();
        configurePlaylistScreenInteractions();
        configureSearchScreenInteractions();
        configureGlobalSearch();
        configureKeyboardShortcuts();
        playbackUiTimer = new javax.swing.Timer(200, e -> updatePlaybackProgressUi());
        playbackUiTimer.start();

        this.playerService.addPropertyChangeListener(evt -> {
            if ("playing".equals(evt.getPropertyName())) {
                boolean nowPlaying = (Boolean) evt.getNewValue();
                boolean wasPlaying = (Boolean) evt.getOldValue();
                if (wasPlaying && !nowPlaying) {
                    if (playerService.isNaturallyEnded()) {
                        SwingUtilities.invokeLater(() -> {
                            playerService.handleTrackCompletion();
                            updateNowPlayingLabel();
                            refreshQueueTable();
                            libraryTable.repaint();
                        });
                    } else {
                        SwingUtilities.invokeLater(this::updateNowPlayingLabel);
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        updateNowPlayingLabel();
                        libraryTable.repaint();
                    });
                }
            }
            if ("currentSong".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    updateNowPlayingLabel();
                    refreshQueueTable();
                    libraryTable.repaint();
                });
            }
            if ("playbackError".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() ->
                        showToast("Cannot play: " + evt.getNewValue()));
            }
        });

        cardLayout.show(rootPanel, CARD_AUTH);
    }

    private void stylePlaybackControls() {
        Font symbolFont = new Font("Segoe UI Symbol", Font.BOLD, 15);
        shuffleToggle.setForeground(Theme.TEXT_PRIMARY);
        shuffleToggle.setFont(symbolFont);
        shuffleToggle.setToolTipText("Shuffle");
        repeatButton.setForeground(Theme.TEXT_PRIMARY);
        repeatButton.setFont(symbolFont);
        repeatButton.setToolTipText("Repeat");
        playButton.setFont(symbolFont.deriveFont(17f));
        playButton.setPreferredSize(new Dimension(44, 36));
        playButton.setBackground(Theme.ACCENT_SOFT);
        seekSlider.setOpaque(true);
        seekSlider.setBackground(Theme.BG_SURFACE);
        seekSlider.setForeground(Theme.ACCENT);
        seekSlider.setUI(new AccentSliderUI(seekSlider, Theme.ACCENT, Theme.BG_BORDER));
        volumeSlider.setOpaque(true);
        volumeSlider.setBackground(Theme.BG_SURFACE);
        volumeSlider.setForeground(Theme.ACCENT);
        volumeSlider.setUI(new AccentSliderUI(volumeSlider, Theme.ACCENT, Theme.BG_BORDER));
        volumeSlider.setValue(playerService.getVolumePercent());
    }

    private RoundedButton createPrimaryButton(String text) {
        RoundedButton btn = new RoundedButton(text, 16, Theme.ACCENT_SOFT, new Color(0x1A4A2B), new Color(0x10331E));
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFont(Theme.body(13f).deriveFont(Font.BOLD));
        btn.setMargin(new java.awt.Insets(4, 12, 4, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private RoundedButton createSecondaryButton(String text) {
        RoundedButton btn = new RoundedButton(text, 16, Theme.BG_SURFACE, Theme.BG_HOVER, Theme.BG_DEEP);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFont(Theme.body(13f));
        btn.setMargin(new java.awt.Insets(4, 12, 4, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private RoundedButton createIconButton(String text) {
        RoundedButton btn = new RoundedButton(text, 14, Theme.BG_DEEP, Theme.BG_HOVER, Theme.BG_SURFACE);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        btn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel buildAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ABYSS);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        RoundedPanel card = new RoundedPanel(50, DEEP_NAVY);
        card.setPreferredSize(new Dimension(360, 380));
        card.setMaximumSize(new Dimension(360, 380));
        card.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("♫  VibeVault");
        title.setForeground(CREAM);
        title.setFont(new Font("Segoe UI Symbol", Font.BOLD, 28));
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
        
        // Enter key navigation
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> {
            if (registerMode) {
                confirmPasswordField.requestFocusInWindow();
            } else {
                handlePrimaryAuthAction();
            }
        });
        confirmPasswordField.addActionListener(e -> handlePrimaryAuthAction());

        confirmPasswordField.setVisible(false);
        confirmPasswordField.setMaximumSize(new Dimension(320, 38));
        confirmPasswordField.setPreferredSize(new Dimension(320, 38));

        usernameField.setMaximumSize(new Dimension(320, 38));
        passwordField.setMaximumSize(new Dimension(320, 38));

        authPrimaryButton = new RoundedButton("Login", 20, STEEL_BLUE, MUTED_BLUE, new Color(0x165229));
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

        authErrorLabel.setForeground(Theme.DANGER);
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

        JPanel center = new JPanel(new java.awt.GridBagLayout());
        center.setBackground(ABYSS);
        center.add(new ThreeDPanel(50, card), new java.awt.GridBagConstraints());
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
        contentCardPanel.add(buildSearchResultsPanel(), CONTENT_SEARCH);
        shell.add(contentCardPanel, BorderLayout.CENTER);
        shell.add(buildNowPlayingBar(), BorderLayout.SOUTH);
        return shell;
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setBackground(Theme.BG_DEEP);
        topBar.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        JPanel leftNav = new JPanel(new GridLayout(0, 1, 0, 2));
        leftNav.setOpaque(false);
        JLabel brandLabel = new JLabel("VibeVault");
        brandLabel.setForeground(Theme.TEXT_PRIMARY);
        brandLabel.setFont(Theme.heading(22f));
        JLabel brandMeta = new JLabel("Deep Space Audio");
        brandMeta.setForeground(Theme.TEXT_MUTED);
        brandMeta.setFont(Theme.body(12f));
        leftNav.add(brandLabel);
        leftNav.add(brandMeta);

        styleInputField(globalSearchField);
        applyPlaceholder(globalSearchField, "Search songs, artists, and playlists...");
        globalSearchField.setPreferredSize(new Dimension(340, 36));
        globalSearchField.setMinimumSize(new Dimension(160, 36));
        globalSearchField.setMaximumSize(new Dimension(500, 36));

        RoundedButton statsButton = createSecondaryButton("Stats");
        statsButton.setPreferredSize(new Dimension(86, 36));
        statsButton.addActionListener(e -> {
            showContentCard(CONTENT_STATS);
            User currentUser = requireCurrentUser();
            refreshLibraryAndStats(currentUser.getUserId());
        });
        RoundedButton userButton = createSecondaryButton("Account");
        userButton.setPreferredSize(new Dimension(110, 36));
        userButton.addActionListener(e -> showUserMenu(userButton));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(statsButton);
        right.add(userButton);

        topBar.add(leftNav, BorderLayout.WEST);
        topBar.add(globalSearchField, BorderLayout.CENTER);
        topBar.add(right, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Theme.BG_DEEP);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BG_BORDER),
                BorderFactory.createEmptyBorder(18, 12, 18, 12)
        ));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel sectionLabel = new JLabel("Library");
        sectionLabel.setForeground(Theme.TEXT_MUTED);
        sectionLabel.setFont(Theme.body(11f).deriveFont(Font.BOLD));
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 10, 0));

        SidebarButton songsButton = createSidebarButton("♫", "Songs", CONTENT_LIBRARY);
        SidebarButton artistsButton = createSidebarButton("◎", "Artists", CONTENT_BROWSE);
        SidebarButton playlistsButton = createSidebarButton("≡", "Playlists", CONTENT_PLAYLISTS);
        SidebarButton statsButton = createSidebarButton("◫", "Stats", CONTENT_STATS);

        songsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showContentCard(CONTENT_LIBRARY);
            }
        });
        artistsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showBrowseArtists();
            }
        });
        playlistsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showContentCard(CONTENT_PLAYLISTS);
            }
        });
        statsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showContentCard(CONTENT_STATS);
            }
        });

        sidebar.add(sectionLabel);
        sidebar.add(songsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(artistsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(playlistsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(statsButton);
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private SidebarButton createSidebarButton(String icon, String text, String section) {
        SidebarButton btn = new SidebarButton(icon, text, false);
        btn.setAlignmentX(CENTER_ALIGNMENT);
        sidebarButtons.put(section, btn);
        return btn;
    }

    private void setActiveSection(String section) {
        sidebarButtons.forEach((key, button) -> button.setActive(key.equals(section)));
    }

    private void showContentCard(String card) {
        if (!CONTENT_SEARCH.equals(card)) {
            activeContentCard = card;
            lastNonSearchContentCard = card;
            setActiveSection(card);
        }
        contentCardLayout.show(contentCardPanel, card);
    }

    private JPanel buildNowPlayingBar() {
        JPanel bar = new JPanel(new GridLayout(1, 3, 0, 0));
        bar.setBackground(Theme.BG_SURFACE);
        bar.setPreferredSize(new Dimension(0, 104));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BG_BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);
        nowPlayingAvatar.setPreferredSize(new Dimension(48, 48));
        JPanel songMetaPanel = new JPanel();
        songMetaPanel.setOpaque(false);
        songMetaPanel.setLayout(new BoxLayout(songMetaPanel, BoxLayout.Y_AXIS));
        nowPlayingLabel.setForeground(Theme.TEXT_PRIMARY);
        nowPlayingLabel.setFont(Theme.body(14f).deriveFont(Font.BOLD));
        nowPlayingArtistLabel.setForeground(Theme.TEXT_MUTED);
        nowPlayingArtistLabel.setFont(Theme.body(12f));
        songMetaPanel.add(nowPlayingLabel);
        songMetaPanel.add(Box.createVerticalStrut(4));
        songMetaPanel.add(nowPlayingArtistLabel);
        leftPanel.add(nowPlayingAvatar);
        leftPanel.add(songMetaPanel);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setAlignmentX(CENTER_ALIGNMENT);

        JPanel seekRow = new JPanel(new BorderLayout(8, 0));
        seekRow.setOpaque(false);
        elapsedTimeLabel.setForeground(Theme.TEXT_MUTED);
        elapsedTimeLabel.setFont(Theme.body(11f));
        totalTimeLabel.setForeground(Theme.TEXT_MUTED);
        totalTimeLabel.setFont(Theme.body(11f));
        seekRow.add(elapsedTimeLabel, BorderLayout.WEST);
        seekRow.add(seekSlider, BorderLayout.CENTER);
        seekRow.add(totalTimeLabel, BorderLayout.EAST);

        JPanel centerControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerControls.setOpaque(false);
        RoundedButton previousButton = createIconButton("⏮");
        RoundedButton nextButton = createIconButton("⏭");
        previousButton.setPreferredSize(new Dimension(40, 34));
        nextButton.setPreferredSize(new Dimension(40, 34));
        shuffleToggle.setPreferredSize(new Dimension(40, 34));
        repeatButton.setPreferredSize(new Dimension(40, 34));
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
        centerControls.add(repeatButton);
        repeatButton.addActionListener(e -> cycleRepeatMode());
        centerPanel.add(seekRow);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(centerControls);

        ChangeListener seekListener = e -> {
            if (seekSliderInternalUpdate || seekSlider.getValueIsAdjusting()) {
                return;
            }
            Song current = playerService.getCurrentSong().orElse(null);
            if (current == null || current.getDurationSeconds() == null || current.getDurationSeconds() <= 0) {
                return;
            }
            int targetSecond = (int) Math.round(seekSlider.getValue() / 1000.0 * current.getDurationSeconds());
            playerService.seekToSecond(Math.min(targetSecond, current.getDurationSeconds()));
            updateNowPlayingLabel();
        };
        seekSlider.addChangeListener(seekListener);

        volumeSlider.addChangeListener(e -> playerService.setVolumePercent(volumeSlider.getValue()));
        JPanel rightControls = new JPanel(new BorderLayout(8, 4));
        rightControls.setOpaque(false);

        JPanel volumePanel = new JPanel(new BorderLayout(4, 0));
        volumePanel.setOpaque(false);
        JLabel volumeLabel = new JLabel("Volume");
        volumeLabel.setForeground(Theme.TEXT_MUTED);
        volumeLabel.setFont(Theme.body(12f));
        volumePanel.add(volumeLabel, BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);
        summaryLabel.setForeground(Theme.TEXT_SUBTLE);
        summaryLabel.setFont(Theme.body(12f));
        rightControls.add(summaryLabel, BorderLayout.NORTH);
        rightControls.add(volumePanel, BorderLayout.SOUTH);

        bar.add(leftPanel);
        bar.add(centerPanel);
        bar.add(rightControls);
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
        browseHeaderLabel.setFont(Theme.heading(20f));
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        togglePanel.setOpaque(false);
        JLabel browseHint = new JLabel("Artists in your library");
        browseHint.setForeground(Theme.TEXT_MUTED);
        browseHint.setFont(Theme.body(12f));
        togglePanel.add(browseHint);
        header.add(browseHeaderLabel, BorderLayout.WEST);
        header.add(togglePanel, BorderLayout.EAST);

        browseGridPanel.setBackground(DEEP_NAVY);
        JScrollPane gridScroll = new JScrollPane(browseGridPanel);
        styleScrollPane(gridScroll);
        browseCardPanel.add(gridScroll, BROWSE_GRID);

        JPanel detail = new JPanel(new BorderLayout(8, 8));
        detail.setBackground(DEEP_NAVY);
        RoundedButton backButton = createSecondaryButton("← Back");
        backButton.addActionListener(e -> browseCardLayout.show(browseCardPanel, BROWSE_GRID));
        RoundedButton playAllButton = createPrimaryButton("▶ Play All");
        playAllButton.addActionListener(e -> playCurrentBrowseSongs());
        JPanel detailTop = new JPanel(new BorderLayout());
        detailTop.setOpaque(false);
        browseDetailLabel.setForeground(CREAM);
        browseDetailLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        detailTop.add(backButton, BorderLayout.WEST);
        detailTop.add(browseDetailLabel, BorderLayout.CENTER);
        detailTop.add(playAllButton, BorderLayout.EAST);

        JScrollPane songsScroll = new JScrollPane(browseSongsTable);
        styleScrollPane(songsScroll);

        detail.add(detailTop, BorderLayout.NORTH);
        detail.add(songsScroll, BorderLayout.CENTER);
        browseCardPanel.add(detail, BROWSE_DETAIL);

        panel.add(header, BorderLayout.NORTH);
        panel.add(browseCardPanel, BorderLayout.CENTER);
        return panel;
    }

    private void showBrowseArtists() {
        browseHeaderLabel.setText("Artists");
        refreshBrowseGrid();
        showContentCard(CONTENT_BROWSE);
        browseCardLayout.show(browseCardPanel, BROWSE_GRID);
    }

    private void refreshBrowseGrid() {
        browseGridPanel.removeAll();
        User user = requireCurrentUser();
        List<LibraryService.ArtistLibrarySummary> artists = libraryService.getArtistBrowseSummaries(user.getUserId());
        for (LibraryService.ArtistLibrarySummary artist : artists) {
            RoundedButton card = new RoundedButton(
                    "<html><div style='text-align:center;'><b><font size='3'>" + escapeHtml(artist.artistName()) +
                            "</font></b><br/><span style='font-size:10px;'>" + artist.songCount() + " songs</span></div></html>",
                    15, DEEP_NAVY, STEEL_BLUE, ABYSS
            );
            card.setForeground(CREAM);
            card.setFont(Theme.body(11f));
            card.setPreferredSize(new Dimension(100, 100));
            card.addActionListener(e -> openArtistBrowseDetail(artist.artistId(), artist.artistName()));
            browseGridPanel.add(card);
        }
        browseGridPanel.revalidate();
        browseGridPanel.repaint();
    }

    private void openArtistBrowseDetail(int artistId, String artistName) {
        User user = requireCurrentUser();
        browseDetailLabel.setText(artistName);
        currentBrowseSongs = libraryService.getSongsByArtistInUserLibrary(user.getUserId(), artistId);
        fillBrowseSongsTable(currentBrowseSongs);
        browseCardLayout.show(browseCardPanel, BROWSE_DETAIL);
    }

    private void fillBrowseSongsTable(List<Song> songs) {
        browseSongsTableModel.setRowCount(0);
        browseSongIds.clear();
        currentBrowseSongs = List.copyOf(songs);
        int rowNum = 1;
        for (Song song : songs) {
            browseSongIds.add(song.getSongId());
            browseSongsTableModel.addRow(new Object[]{
                    rowNum++,
                    song.getTitle(),
                    lookupArtistName(song.getArtistId()),
                    formatDuration(song.getDurationSeconds())
            });
        }
    }

    private JPanel buildSearchResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(Theme.BG_DEEP);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        RoundedPanel surface = new RoundedPanel(28, Theme.BG_SURFACE);
        surface.setBorderConfig(Theme.BG_BORDER, 1);
        surface.setLayout(new BorderLayout(12, 12));
        surface.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        searchResultsLabel.setForeground(Theme.TEXT_PRIMARY);
        searchResultsLabel.setFont(Theme.heading(20f));
        surface.add(searchResultsLabel, BorderLayout.NORTH);

        JPanel sections = new JPanel(new GridLayout(3, 1, 0, 12));
        sections.setOpaque(false);
        sections.add(buildSearchSection("Songs", searchSongsTable));
        sections.add(buildSearchSection("Artists", searchArtistsTable));
        sections.add(buildSearchSection("Playlists", searchPlaylistsTable));
        surface.add(sections, BorderLayout.CENTER);

        panel.add(surface, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSearchSection(String title, JTable table) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setForeground(Theme.TEXT_MUTED);
        label.setFont(Theme.body(12f).deriveFont(Font.BOLD));
        section.add(label, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel buildPlaylistScreenPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(DEEP_NAVY);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane leftListScroll = new JScrollPane(playlistScreenListTable);
        styleScrollPane(leftListScroll);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setOpaque(false);
        JPanel leftHeader = new JPanel(new BorderLayout(8, 0));
        leftHeader.setOpaque(false);
        JLabel playlistsLabel = new JLabel("Playlists");
        playlistsLabel.setForeground(CREAM);
        playlistsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        RoundedButton createButton = createPrimaryButton("+ New");
        createButton.setPreferredSize(new Dimension(80, 32));
        createButton.addActionListener(e -> showCreatePlaylistDialog());
        leftHeader.add(playlistsLabel, BorderLayout.WEST);
        leftHeader.add(createButton, BorderLayout.EAST);
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        leftPanel.add(leftListScroll, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(260, 0));

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        playlistScreenTitleLabel.setForeground(CREAM);
        playlistScreenTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        playlistScreenMetaLabel.setForeground(new Color(0xA2A89A));

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        RoundedButton playButton = createPrimaryButton("▶ Play");
        RoundedButton addSongButton = createSecondaryButton("+ Add Songs");
        RoundedButton renameButton = createSecondaryButton("✏ Rename");
        RoundedButton deleteButton = createSecondaryButton("🗑 Delete");
        playButton.addActionListener(e -> playSelectedPlaylistScreen());
        addSongButton.addActionListener(e -> showAddSongsToPlaylistDialog(getSelectedPlaylistScreenId()));
        renameButton.addActionListener(e -> renameSelectedPlaylistScreen());
        deleteButton.addActionListener(e -> deleteSelectedPlaylistScreen());
        actions.add(playButton);
        actions.add(addSongButton);
        actions.add(renameButton);
        actions.add(deleteButton);
        header.add(playlistScreenTitleLabel, BorderLayout.NORTH);
        header.add(playlistScreenMetaLabel, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);

        JScrollPane songsScroll = new JScrollPane(playlistScreenSongsTable);
        styleScrollPane(songsScroll);

        JPanel rowActions = new JPanel();
        rowActions.setOpaque(false);
        RoundedButton moveUpButton = createSecondaryButton("Move Up");
        RoundedButton moveDownButton = createSecondaryButton("Move Down");
        RoundedButton removeButton = createSecondaryButton("Remove from Playlist");
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

        panel.add(leftPanel, BorderLayout.WEST);
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

    private void selectPlaylistScreenRow(int playlistId) {
        for (int i = 0; i < playlistScreenListModel.getRowCount(); i++) {
            if (Integer.parseInt(playlistScreenListModel.getValueAt(i, 0).toString()) == playlistId) {
                playlistScreenListTable.setRowSelectionInterval(i, i);
                refreshPlaylistScreenDetail();
                return;
            }
        }
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

        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 0));
        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(0, 80)); // Constrained height for cards
        cards.add(buildStatCard("Total Plays", statsTotalPlaysValueLabel));
        cards.add(buildStatCard("Listening Time", statsListeningValueLabel));
        cards.add(buildStatCard("Top Artist", statsTopArtistValueLabel));

        JPanel lists = new JPanel(new GridLayout(1, 2, 10, 10));
        lists.setOpaque(false);

        JTable topSongsStatsTable = new JTable(topSongsTableModel);
        JTable topArtistsStatsTable = new JTable(topArtistsTableModel);
        JScrollPane topSongsScroll = new JScrollPane(topSongsStatsTable);
        JScrollPane topArtistsScroll = new JScrollPane(topArtistsStatsTable);
        styleScrollPane(topSongsScroll);
        styleScrollPane(topArtistsScroll);
        topSongsScroll.setBorder(BorderFactory.createTitledBorder("Top 5 Songs"));
        topArtistsScroll.setBorder(BorderFactory.createTitledBorder("Top 5 Artists"));
        topSongsStatsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        topSongsStatsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        topSongsStatsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        topArtistsStatsTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        topArtistsStatsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        topArtistsStatsTable.getColumnModel().getColumn(2).setPreferredWidth(70);

        lists.add(topSongsScroll);
        lists.add(topArtistsScroll);

        JPanel weeklySection = new JPanel(new BorderLayout());
        weeklySection.setOpaque(false);
        JLabel weeklyTitle = new JLabel("Weekly Activity");
        weeklyTitle.setForeground(CREAM);
        weeklySection.add(weeklyTitle, BorderLayout.NORTH);
        weeklyBarChart.setPreferredSize(new Dimension(0, 160));
        weeklySection.add(weeklyBarChart, BorderLayout.CENTER);

        JPanel recentSection = new JPanel(new BorderLayout());
        recentSection.setOpaque(false);
        JLabel recentTitle = new JLabel("Recently Played");
        recentTitle.setForeground(CREAM);
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        recentSection.add(recentTitle, BorderLayout.NORTH);
        JTable recentStatsTable = new JTable(recentPlaysTableModel);
        styleTable(recentStatsTable);
        JScrollPane recentScroll = new JScrollPane(recentStatsTable);
        recentScroll.setPreferredSize(new Dimension(0, 140));
        styleScrollPane(recentScroll);
        recentSection.add(recentScroll, BorderLayout.CENTER);

        JPanel lowerSection = new JPanel(new GridLayout(2, 1, 0, 10));
        lowerSection.setOpaque(false);
        lowerSection.add(weeklySection);
        lowerSection.add(recentSection);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(lists, BorderLayout.CENTER);
        center.add(lowerSection, BorderLayout.SOUTH);

        JScrollPane mainScroll = new JScrollPane(center);
        styleScrollPane(mainScroll);
        mainScroll.setBorder(null);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(mainScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatCard(String labelText, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(2, 2));
        card.setBackground(DEEP_NAVY);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STEEL_BLUE, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(0xA2A89A));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        valueLabel.setForeground(CREAM);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        card.add(label, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }


    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(CREAM);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    private JPanel buildLibraryWorkspacePanel() {
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

        RoundedButton addSongsButton = createPrimaryButton("Add Songs");
        addSongsButton.setPreferredSize(new Dimension(110, 34));
        addSongsButton.addActionListener(e -> showAddSongsDialog());

        RoundedButton scanButton = createSecondaryButton("Scan");
        scanButton.setPreferredSize(new Dimension(80, 34));
        scanButton.addActionListener(e -> showScanDialog());

        styleInputField(librarySearchField);
        applyPlaceholder(librarySearchField, "Search your library...");
        librarySearchField.setPreferredSize(new Dimension(260, 34));

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(addSongsButton);
        headerActions.add(scanButton);
        headerActions.add(librarySearchField);
        libraryHeader.add(titlePanel, BorderLayout.WEST);
        libraryHeader.add(headerActions, BorderLayout.EAST);
        JScrollPane libraryPane = new JScrollPane(libraryTable);
        styleScrollPane(libraryPane);
        libraryPane.setBorder(null);

        RoundedPanel contentSurface = new RoundedPanel(28, Theme.BG_SURFACE);
        contentSurface.setBorderConfig(Theme.BG_BORDER, 1);
        contentSurface.setLayout(new BorderLayout(12, 12));
        contentSurface.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contentSurface.add(libraryHeader, BorderLayout.NORTH);
        contentSurface.add(libraryPane, BorderLayout.CENTER);

        playlistsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshSelectedPlaylistSongs();
            }
        });

        panel.add(contentSurface, BorderLayout.CENTER);
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
        globalSearchField.setText("");
        restorePlaceholderIfEmpty(globalSearchField);
        clearSearchResults();
        libraryTableModel.setRowCount(0);
        playlistsTableModel.setRowCount(0);
        playlistSongsTableModel.setRowCount(0);
        queueTableModel.setRowCount(0);
        topSongsTableModel.setRowCount(0);
        topArtistsTableModel.setRowCount(0);
        weeklyActivityTableModel.setRowCount(0);
        recentPlaysTableModel.setRowCount(0);
        summaryLabel.setText("No data yet.");
        nowPlayingLabel.setText("Nothing playing");
        nowPlayingArtistLabel.setText("Choose a song from your library");
        elapsedTimeLabel.setText("0:00");
        totalTimeLabel.setText("0:00");
        librarySongCountLabel.setText("0 songs");
        playerService.clearQueue();
        setActiveSection(CONTENT_LIBRARY);
        cardLayout.show(rootPanel, CARD_AUTH);
    }

    private void openDashboard(User user) {
        welcomeLabel.setText("Welcome, " + user.getUsername());
        playerService.setActiveUserId(user.getUserId());
        playerService.setRepeatMode(PlayerService.RepeatMode.OFF);
        playerService.setShuffleEnabled(false);
        repeatButton.setText("🔁");
        syncShuffleButtonState();
        clearSearchResults();
        showContentCard(CONTENT_LIBRARY);

        // Step 7: Dead file cleanup on login
        int removed = libraryScanService.validateAndCleanLibrary(user.getUserId());
        if (removed > 0) {
            showToast(removed + " missing files removed from library.");
        }

        refreshLibraryAndStats(user.getUserId());
        refreshPlaylists(user.getUserId());
        refreshQueueTable();
        cardLayout.show(rootPanel, CARD_DASHBOARD);
    }

    private void showScanDialog() {
        User currentUser = requireCurrentUser();
        JDialog dialog = new JDialog(this, "Library Auto-Scan", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(DEEP_NAVY);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Watched Folders");
        title.setForeground(CREAM);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        DefaultListModel<String> folderModel = new DefaultListModel<>();
        folderModel.addAll(libraryScanService.getWatchedFolders(currentUser.getUserId()));
        JList<String> folderList = new JList<>(folderModel);
        folderList.setBackground(ABYSS);
        folderList.setForeground(CREAM);
        JScrollPane scroll = new JScrollPane(folderList);
        styleScrollPane(scroll);

        RoundedButton addButton = createSecondaryButton("+ Add Folder");
        RoundedButton removeButton = createSecondaryButton("- Remove");
        RoundedButton scanNowButton = createPrimaryButton("Scan Now");

        addButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Folder to Watch");
            chooser.setAcceptAllFileFilterUsed(false);
            int result = chooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File folder = chooser.getSelectedFile();
                if (folder.exists() && folder.isDirectory()) {
                    String path = folder.getAbsolutePath();
                    try {
                        libraryScanService.addWatchedFolder(currentUser.getUserId(), path);
                        folderModel.clear();
                        folderModel.addAll(libraryScanService.getWatchedFolders(currentUser.getUserId()));
                    } catch (IllegalArgumentException ex) {
                        showError(ex.getMessage());
                    }
                }
            }
        });

        removeButton.addActionListener(e -> {
            String selected = folderList.getSelectedValue();
            if (selected != null) {
                libraryScanService.removeWatchedFolder(currentUser.getUserId(), selected);
                folderModel.removeElement(selected);
            }
        });

        JProgressBar progress = new JProgressBar();
        progress.setVisible(false);
        progress.setBackground(ABYSS);
        progress.setForeground(STEEL_BLUE);
        progress.setStringPainted(true);

        scanNowButton.addActionListener(e -> {
            if (folderModel.isEmpty()) {
                showError("Add at least one folder to scan");
                return;
            }
            scanNowButton.setEnabled(false);
            progress.setVisible(true);
            SwingWorker<LibraryScanService.ScanResult, Integer> worker = new SwingWorker<>() {
                @Override
                protected LibraryScanService.ScanResult doInBackground() {
                    return libraryScanService.scanWatchedFolders(currentUser.getUserId(), (cur, total) -> {
                        progress.setMaximum(total);
                        publish(cur);
                    });
                }
                @Override
                protected void process(List<Integer> chunks) {
                    progress.setValue(chunks.get(chunks.size() - 1));
                }
                @Override
                protected void done() {
                    try {
                        LibraryScanService.ScanResult res = get();
                        refreshLibraryAndStats(currentUser.getUserId());
                        showToast(res.toSummaryString());
                        dialog.dispose();
                    } catch (Exception ex) {
                        showError("Scan failed: " + ex.getMessage());
                        scanNowButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setOpaque(false);
        btns.add(addButton);
        btns.add(removeButton);
        btns.add(scanNowButton);

        root.add(title, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(progress, BorderLayout.NORTH);
        bottom.add(btns, BorderLayout.SOUTH);
        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private void importSong() {
        User currentUser = requireCurrentUser();
        try {
            libraryService.importSong(currentUser.getUserId(), new LibraryService.SongImportRequest(
                    readTextInput(titleField),
                    readTextInput(artistField),
                    null,
                    readTextInput(sourceField)
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

    private void showAddSongsDialog() {
        User currentUser = requireCurrentUser();

        JDialog dialog = new JDialog(this, "Add Songs", true);
        dialog.setSize(700, 460);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(DEEP_NAVY);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Import MP3 Songs");
        title.setForeground(CREAM);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel subtitle = new JLabel("Choose one or more MP3 files and import them into your library.");
        subtitle.setForeground(new Color(0xA2A89A));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JPanel titlePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        DefaultListModel<File> selectedFilesModel = new DefaultListModel<>();
        JList<File> selectedFilesList = new JList<>(selectedFilesModel);
        selectedFilesList.setBackground(ABYSS);
        selectedFilesList.setForeground(CREAM);
        selectedFilesList.setSelectionBackground(STEEL_BLUE);
        selectedFilesList.setSelectionForeground(CREAM);
        selectedFilesList.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        selectedFilesList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getName());
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(STEEL_BLUE);
                label.setForeground(CREAM);
            } else {
                label.setBackground(ABYSS);
                label.setForeground(CREAM);
            }
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            label.setToolTipText(value.getAbsolutePath());
            return label;
        });
        JScrollPane filesScroll = new JScrollPane(selectedFilesList);
        styleScrollPane(filesScroll);
        filesScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(STEEL_BLUE),
                "Selected Files",
                0,
                0,
                new Font("Segoe UI", Font.PLAIN, 12),
                CREAM
        ));

        JLabel infoLabel = new JLabel("No files selected.");
        infoLabel.setForeground(new Color(0xA2A89A));

        RoundedButton browseButton = createSecondaryButton("Browse MP3 Files");
        RoundedButton clearButton = createSecondaryButton("Clear");
        RoundedButton importButton = createPrimaryButton("Import");
        RoundedButton cancelButton = createSecondaryButton("Cancel");
        importButton.setEnabled(false);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setVisible(false);
        progressBar.setBackground(ABYSS);
        progressBar.setForeground(MUTED_BLUE);
        progressBar.setStringPainted(true);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setDialogTitle("Select MP3 Files");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 Files", "mp3"));
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = chooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    boolean exists = false;
                    for (int i = 0; i < selectedFilesModel.size(); i++) {
                        if (selectedFilesModel.get(i).equals(file)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        selectedFilesModel.addElement(file);
                    }
                }
                int count = selectedFilesModel.size();
                infoLabel.setText(count + " file(s) selected.");
                importButton.setEnabled(count > 0);
            }
        });

        clearButton.addActionListener(e -> {
            selectedFilesModel.clear();
            infoLabel.setText("No files selected.");
            importButton.setEnabled(false);
            progressBar.setVisible(false);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        importButton.addActionListener(e -> {
            if (selectedFilesModel.isEmpty()) {
                showError("Select at least one MP3 file");
                return;
            }

            List<java.nio.file.Path> paths = new ArrayList<>();
            for (int i = 0; i < selectedFilesModel.size(); i++) {
                paths.add(selectedFilesModel.get(i).toPath());
            }

            browseButton.setEnabled(false);
            clearButton.setEnabled(false);
            importButton.setEnabled(false);
            cancelButton.setEnabled(false);
            progressBar.setMinimum(0);
            progressBar.setMaximum(paths.size());
            progressBar.setValue(0);
            progressBar.setString("0 / " + paths.size());
            progressBar.setVisible(true);

            SwingWorker<Integer, Integer> worker = new SwingWorker<>() {
                @Override
                protected Integer doInBackground() {
                    return libraryService.importSongs(currentUser.getUserId(), paths, this::publish);
                }

                @Override
                protected void process(List<Integer> chunks) {
                    int latest = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latest);
                    progressBar.setString(latest + " / " + paths.size());
                }

                @Override
                protected void done() {
                    try {
                        int importedCount = get();
                        refreshLibraryAndStats(currentUser.getUserId());
                        dialog.dispose();
                        showToast("Successfully imported " + importedCount + " songs.");
                    } catch (Exception ex) {
                        browseButton.setEnabled(true);
                        clearButton.setEnabled(true);
                        cancelButton.setEnabled(true);
                        importButton.setEnabled(!selectedFilesModel.isEmpty());
                        showError("Failed to import songs: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(browseButton);
        buttons.add(clearButton);
        buttons.add(importButton);
        buttons.add(cancelButton);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.add(infoLabel, BorderLayout.WEST);
        bottom.add(progressBar, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        root.add(top, BorderLayout.NORTH);
        root.add(filesScroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
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
        if (playerService.previous().isPresent() && !playerService.isPlaying()) {
            playerService.play();
        }
        SwingUtilities.invokeLater(this::updateNowPlayingLabel);
    }

    private void playNext() {
        if (playerService.next().isPresent() && !playerService.isPlaying()) {
            playerService.play();
        }
        SwingUtilities.invokeLater(this::updateNowPlayingLabel);
    }

    private void toggleShuffle() {
        boolean enabled = !playerService.isShuffleEnabled();
        playerService.setShuffleEnabled(enabled);
        syncShuffleButtonState();
        showToast("Shuffle: " + (enabled ? "ON" : "OFF"));
    }

    private void showToast(String message) {
        JLabel toast = new JLabel(message, JLabel.CENTER);
        toast.setFont(new Font("Segoe UI", Font.BOLD, 14));
        toast.setForeground(CREAM);
        toast.setBackground(STEEL_BLUE);
        toast.setOpaque(true);
        // Significantly increased horizontal padding
        toast.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));

        JPanel glass = (JPanel) getGlassPane();
        glass.setLayout(null);
        glass.setVisible(true);
        glass.removeAll();

        // Calculate size and add a safety margin
        Dimension prefSize = toast.getPreferredSize();
        int width = prefSize.width + 20; 
        int height = prefSize.height;
        toast.setSize(width, height);
        
        int x = (getWidth() - width) / 2;
        int y = 60; 
        toast.setLocation(x, y);
        glass.add(toast);
        glass.repaint();

        new javax.swing.Timer(2500, e -> {
            glass.remove(toast);
            glass.repaint();
            if (glass.getComponentCount() == 0) {
                glass.setVisible(false);
            }
        }).start();
    }

    private void cycleRepeatMode() {
        PlayerService.RepeatMode current = playerService.getRepeatMode();
        PlayerService.RepeatMode next = switch (current) {
            case OFF -> PlayerService.RepeatMode.ONE;
            case ONE -> PlayerService.RepeatMode.ALL;
            case ALL -> PlayerService.RepeatMode.OFF;
        };
        playerService.setRepeatMode(next);
        repeatButton.setText(switch (next) {
            case OFF -> "🔁";
            case ONE -> "🔂";
            case ALL -> "🔁●";
        });
    }

    private void refreshLibraryAndStats(int userId) {
        List<Song> songs = libraryService.getUserLibrarySongs(userId);
        artistNameCache.clear();
        List<LibraryService.ArtistLibrarySummary> artistSummaries = libraryService.getArtistBrowseSummaries(userId);
        artistNameCache.putAll(artistSummaries.stream().collect(Collectors.toMap(
                LibraryService.ArtistLibrarySummary::artistId,
                LibraryService.ArtistLibrarySummary::artistName
        )));

        libraryTableModel.setRowCount(0);
        for (Song song : songs) {
            libraryTableModel.addRow(new Object[]{
                    song.getSongId(),
                    song.getTitle(),
                    lookupArtistName(song.getArtistId()),
                    formatDuration(song.getDurationSeconds())
            });
        }
        librarySongCountLabel.setText(songs.size() + " songs");

        double totalMinutes = statsService.getTotalListeningMinutes(userId);
        String topSong = statsService.getTopSongs(userId, 1).stream()
                .findFirst()
                .map(StatsService.SongPlayStat::songTitle)
                .orElse("N/A");
        summaryLabel.setText(
                "Songs: " + songs.size() +
                        " | Total listening minutes: " + totalMinutes +
                        " | Top song: " + topSong
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
        List<StatsService.DailyListeningStat> weeklyStats = statsService.getWeeklyActivity(userId);
        for (StatsService.DailyListeningStat stat : weeklyStats) {
            weeklyActivityTableModel.addRow(new Object[]{stat.day(), stat.totalMinutes()});
        }
        weeklyBarChart.setData(weeklyStats);

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
        ensureArtistCacheLoaded();
        playerService.getCurrentSong().ifPresentOrElse(song -> {
            String artistName = lookupArtistName(song.getArtistId());
            nowPlayingLabel.setText(song.getTitle());
            nowPlayingArtistLabel.setText(artistName);
            nowPlayingAvatar.setSeedText(artistName.isBlank() ? song.getTitle() : artistName);
            playButton.setText(playerService.isPlaying() ? "⏸" : "▶");
        }, () -> {
            nowPlayingLabel.setText("Nothing playing");
            nowPlayingArtistLabel.setText("Choose a song from your library");
            nowPlayingAvatar.setSeedText("VibeVault");
            playButton.setText("▶");
        });
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
        if (modelRow < 0) {
            return null;
        }
        Object value = libraryTableModel.getValueAt(modelRow, 0);
        return value instanceof Integer ? (Integer) value : Integer.parseInt(value.toString());
    }

    private User requireCurrentUser() {
        return authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    private void clearImportFields() {
        titleField.setText("");
        artistField.setText("");
        sourceField.setText("");
    }

    private String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds <= 0) {
            return "--:--";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String lookupArtistName(int artistId) {
        return artistNameCache.getOrDefault(artistId, "Unknown Artist");
    }

    private void ensureArtistCacheLoaded() {
        if (!artistNameCache.isEmpty()) {
            return;
        }
        authService.getCurrentUser().ifPresent(user ->
                libraryService.getArtistBrowseSummaries(user.getUserId())
                        .forEach(artist -> artistNameCache.put(artist.artistId(), artist.artistName()))
        );
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void styleInputField(JTextField field) {
        field.setBackground(Theme.BG_SURFACE);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.ACCENT);
        field.setFont(Theme.body(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.BG_BORDER, 16, 1),
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
        field.setForeground(Theme.TEXT_MUTED);
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
        scrollPane.getViewport().setBackground(Theme.BG_SURFACE);
        scrollPane.setBackground(Theme.BG_SURFACE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        DarkScrollBarUI.apply(scrollPane.getVerticalScrollBar());
    }

    private void configureGlobalSearch() {
        globalSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyGlobalSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyGlobalSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyGlobalSearch();
            }
        });
    }

    private void applyGlobalSearch() {
        String query = readTextInput(globalSearchField).trim();
        if (query.isEmpty()) {
            clearSearchResults();
            librarySorter.setRowFilter(null);
            showContentCard(lastNonSearchContentCard);
            return;
        }

        lastNonSearchContentCard = CONTENT_SEARCH.equals(activeContentCard) ? lastNonSearchContentCard : activeContentCard;
        populateSearchResults(query);
        contentCardLayout.show(contentCardPanel, CONTENT_SEARCH);
    }

    private void populateSearchResults(String query) {
        String lowered = query.toLowerCase();
        searchResultsLabel.setText("Search results for \"" + query + "\"");
        ensureArtistCacheLoaded();

        List<Song> songs = authService.getCurrentUser()
                .map(user -> libraryService.getUserLibrarySongs(user.getUserId()))
                .orElse(List.of());
        List<Song> matchedSongs = songs.stream()
                .filter(song -> song.getTitle().toLowerCase().contains(lowered)
                        || lookupArtistName(song.getArtistId()).toLowerCase().contains(lowered))
                .toList();

        searchSongsTableModel.setRowCount(0);
        for (Song song : matchedSongs) {
            searchSongsTableModel.addRow(new Object[]{
                    song.getSongId(),
                    song.getTitle(),
                    lookupArtistName(song.getArtistId()),
                    formatDuration(song.getDurationSeconds())
            });
        }

        List<LibraryService.ArtistLibrarySummary> artists = authService.getCurrentUser()
                .map(user -> libraryService.getArtistBrowseSummaries(user.getUserId()))
                .orElse(List.of());
        searchArtistsTableModel.setRowCount(0);
        for (LibraryService.ArtistLibrarySummary artist : artists) {
            if (artist.artistName().toLowerCase().contains(lowered)) {
                searchArtistsTableModel.addRow(new Object[]{
                        artist.artistId(),
                        artist.artistName(),
                        artist.songCount()
                });
            }
        }

        List<Playlist> playlists = authService.getCurrentUser()
                .map(user -> playlistService.getUserPlaylists(user.getUserId()))
                .orElse(List.of());
        searchPlaylistsTableModel.setRowCount(0);
        for (Playlist playlist : playlists) {
            if (playlist.getName().toLowerCase().contains(lowered)) {
                int songCount = playlistService.getPlaylistSongs(playlist.getPlaylistId()).size();
                searchPlaylistsTableModel.addRow(new Object[]{
                        playlist.getPlaylistId(),
                        playlist.getName(),
                        songCount
                });
            }
        }
    }

    private void clearSearchResults() {
        searchResultsLabel.setText("Search results");
        searchSongsTableModel.setRowCount(0);
        searchArtistsTableModel.setRowCount(0);
        searchPlaylistsTableModel.setRowCount(0);
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
        String query = readTextInput(librarySearchField).trim();
        if (query.isEmpty()) {
            librarySorter.setRowFilter(null);
            return;
        }
        librarySorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 1, 2));
    }

    private void configureBrowseScreenInteractions() {
        browseSongsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    playSelectedBrowseSong();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowBrowseSongsPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowBrowseSongsPopup(e);
            }
        });
    }

    private void configurePlaylistScreenInteractions() {
        playlistScreenSongsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    playSelectedPlaylistScreenSong();
                }
            }
        });
    }

    private void configureSearchScreenInteractions() {
        searchSongsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = searchSongsTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = searchSongsTable.convertRowIndexToModel(row);
                        int songId = Integer.parseInt(searchSongsTableModel.getValueAt(modelRow, 0).toString());
                        playSongFromLibrary(songId);
                    }
                }
            }
        });

        searchArtistsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = searchArtistsTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = searchArtistsTable.convertRowIndexToModel(row);
                        int artistId = Integer.parseInt(searchArtistsTableModel.getValueAt(modelRow, 0).toString());
                        String artistName = searchArtistsTableModel.getValueAt(modelRow, 1).toString();
                        showBrowseArtists();
                        openArtistBrowseDetail(artistId, artistName);
                    }
                }
            }
        });

        searchPlaylistsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    int row = searchPlaylistsTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = searchPlaylistsTable.convertRowIndexToModel(row);
                        int playlistId = Integer.parseInt(searchPlaylistsTableModel.getValueAt(modelRow, 0).toString());
                        showContentCard(CONTENT_PLAYLISTS);
                        selectPlaylistScreenRow(playlistId);
                    }
                }
            }
        });
    }

    private void configureKeyboardShortcuts() {
        JComponent target = getRootPane();
        target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle-playback");
        target.getActionMap().put("toggle-playback", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getFocusOwner() instanceof JTextField) {
                    return;
                }
                if (playerService.isPlaying()) {
                    playerService.pause();
                } else {
                    playerService.play();
                }
                updateNowPlayingLabel();
            }
        });
        bindShortcut("queue-next", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), this::playNext);
        bindShortcut("queue-prev", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), this::playPrevious);
        bindShortcut("toggle-shuffle", KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), this::toggleShuffle);
        bindShortcut("focus-library-search", KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
                () -> librarySearchField.requestFocusInWindow());
    }

    private void bindShortcut(String actionKey, KeyStroke keyStroke, Runnable action) {
        JComponent target = getRootPane();
        target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        target.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private Integer getSelectedBrowseSongId() {
        int row = browseSongsTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        if (row < 0 || row >= browseSongIds.size()) {
            return null;
        }
        return browseSongIds.get(row);
    }

    private void playSelectedBrowseSong() {
        Integer songId = getSelectedBrowseSongId();
        if (songId == null) {
            return;
        }
        playSongFromLibrary(songId);
    }

    private void playSelectedPlaylistScreenSong() {
        Integer playlistId = getSelectedPlaylistScreenId();
        int row = playlistScreenSongsTable.getSelectedRow();
        if (playlistId == null || row < 0) {
            return;
        }

        int songId = Integer.parseInt(playlistScreenSongsModel.getValueAt(row, 1).toString());
        List<Song> songs = playlistService.getPlaylistSongs(playlistId);
        playerService.setQueue(songs);
        int queueIndex = IntStream.range(0, songs.size())
                .filter(i -> songs.get(i).getSongId() == songId)
                .findFirst()
                .orElse(0);
        playerService.playAt(queueIndex);
        refreshQueueTable();
        updateNowPlayingLabel();
    }

    private void maybeShowBrowseSongsPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int row = browseSongsTable.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        browseSongsTable.setRowSelectionInterval(row, row);
        Integer songId = getSelectedBrowseSongId();
        if (songId == null) {
            return;
        }

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem playItem = new JMenuItem("Play");
        playItem.addActionListener(evt -> playSongFromLibrary(songId));
        popupMenu.add(playItem);
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
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

    private void showUserMenu(RoundedButton anchor) {
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

    private void showCreatePlaylistDialog() {
        User currentUser = requireCurrentUser();
        String name = JOptionPane.showInputDialog(this, "Playlist name:");
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            playlistService.createPlaylist(currentUser.getUserId(), name.trim());
            refreshPlaylists(currentUser.getUserId());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void showAddSongsToPlaylistDialog(Integer playlistId) {
        if (playlistId == null) {
            showError("Select a playlist first");
            return;
        }
        User currentUser = requireCurrentUser();
        List<Song> songs = libraryService.getUserLibrarySongs(currentUser.getUserId());
        if (songs.isEmpty()) {
            showError("No songs in library");
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Title", "Artist", "Duration"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        List<Integer> songIds = new ArrayList<>();
        for (Song song : songs) {
            songIds.add(song.getSongId());
            model.addRow(new Object[]{
                    song.getTitle(),
                    lookupArtistName(song.getArtistId()),
                    formatDuration(song.getDurationSeconds())
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(650, 340));

        int choice = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Add Songs to Playlist",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }
        for (int selectedRow : selectedRows) {
            playlistService.addSong(playlistId, songIds.get(selectedRow));
        }
        refreshPlaylistScreenDetail();
    }

    private void playCurrentBrowseSongs() {
        if (currentBrowseSongs == null || currentBrowseSongs.isEmpty()) {
            return;
        }
        playerService.setQueue(currentBrowseSongs);
        playerService.playAt(0);
        refreshQueueTable();
        updateNowPlayingLabel();
    }

    private void styleAllTables() {
        styleTable(libraryTable);
        styleTable(playlistsTable);
        styleTable(playlistSongsTable);
        styleTable(queueTable);
        styleTable(topSongsTable);
        styleTable(topArtistsTable);
        styleTable(weeklyActivityTable);
        styleTable(recentPlaysTable);
        styleTable(browseSongsTable);
        styleTable(playlistScreenListTable);
        styleTable(playlistScreenSongsTable);
        styleTable(searchSongsTable);
        styleTable(searchArtistsTable);
        styleTable(searchPlaylistsTable);

        // Step 10: Hide raw ID columns
        hideColumn(libraryTable, 0); // "#" column often confusing if sorted
        hideColumn(playlistsTable, 0);
        hideColumn(playlistSongsTable, 0);
        hideColumn(queueTable, 0);
        hideColumn(queueTable, 1);
        hideColumn(playlistScreenListTable, 0);
        hideColumn(playlistScreenSongsModel, playlistScreenSongsTable, 1);
        hideColumn(searchSongsTable, 0);
        hideColumn(searchArtistsTable, 0);
        hideColumn(searchPlaylistsTable, 0);
    }

    private void hideColumn(JTable table, int columnIndex) {
        table.getColumnModel().getColumn(columnIndex).setMinWidth(0);
        table.getColumnModel().getColumn(columnIndex).setMaxWidth(0);
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(0);
    }

    private void hideColumn(DefaultTableModel model, JTable table, int columnIndex) {
        hideColumn(table, columnIndex);
    }

    private void styleTable(JTable table) {
        table.setBackground(Theme.BG_DEEP);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setSelectionBackground(Theme.BG_HOVER);
        table.setSelectionForeground(Theme.TEXT_PRIMARY);
        table.setGridColor(new Color(255, 255, 255, 6));
        table.setRowHeight(40);
        table.setFont(Theme.body(13f));
        table.getTableHeader().setBackground(Theme.BG_SURFACE);
        table.getTableHeader().setForeground(Theme.TEXT_MUTED);
        table.getTableHeader().setFont(Theme.body(11f).deriveFont(Font.BOLD));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        if (table == libraryTable) {
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
                ) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    int modelRow = table.convertRowIndexToModel(row);
                    Object songIdValue = libraryTableModel.getValueAt(modelRow, 0);
                    Integer currentSongId = playerService.getCurrentSong().map(Song::getSongId).orElse(null);
                    boolean isCurrentSong = currentSongId != null && currentSongId.equals(songIdValue);
                    if (isSelected) {
                        label.setBackground(Theme.BG_HOVER);
                        label.setForeground(Theme.TEXT_PRIMARY);
                        label.setFont(Theme.body(13f));
                    } else if (isCurrentSong) {
                        label.setBackground(Theme.ACCENT_SOFT);
                        label.setForeground(Theme.ACCENT);
                        label.setFont(Theme.body(13f).deriveFont(Font.BOLD));
                    } else {
                        label.setBackground(row % 2 == 0 ? Theme.BG_DEEP : Theme.BG_SURFACE);
                        label.setForeground(Theme.TEXT_PRIMARY);
                        label.setFont(Theme.body(13f));
                    }
                    return label;
                }
            });
        }
    }

    private void syncShuffleButtonState() {
        boolean enabled = playerService.isShuffleEnabled();
        shuffleToggle.setForeground(enabled ? Theme.ACCENT : Theme.TEXT_PRIMARY);
        shuffleToggle.setText(enabled ? "⇌" : "🔀");
    }

    private void updatePlaybackProgressUi() {
        if (seekSlider.getValueIsAdjusting()) {
            return;
        }
        Song current = playerService.getCurrentSong().orElse(null);
        int currentSecond = 0;
        int durationSeconds = 0;
        int nextValue = 0;
        if (current != null && current.getDurationSeconds() != null && current.getDurationSeconds() > 0) {
            currentSecond = playerService.getCurrentSecond();
            durationSeconds = current.getDurationSeconds();
            nextValue = (int) Math.min(1000,
                    Math.round(currentSecond * 1000.0 / durationSeconds));
        }
        seekSliderInternalUpdate = true;
        seekSlider.setValue(nextValue);
        seekSliderInternalUpdate = false;
        elapsedTimeLabel.setText(formatDuration(currentSecond));
        totalTimeLabel.setText(formatDuration(durationSeconds));
    }

    private static class WeeklyBarChart extends JPanel {
        private List<StatsService.DailyListeningStat> data = new ArrayList<>();

        public void setData(List<StatsService.DailyListeningStat> data) {
            this.data = data == null ? List.of() : List.copyOf(data);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padLeft = 40;
            int padRight = 16;
            int padTop = 16;
            int padBottom = 36;
            int chartWidth = Math.max(1, w - padLeft - padRight);
            int chartHeight = Math.max(1, h - padTop - padBottom);

            g2.setColor(DEEP_NAVY);
            g2.fillRect(0, 0, w, h);

            if (data == null || data.isEmpty()) {
                g2.setColor(new Color(0xA2A89A));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.drawString("No data this week", padLeft + 8, padTop + chartHeight / 2);
                g2.dispose();
                return;
            }

            double maxMinutes = data.stream().mapToDouble(StatsService.DailyListeningStat::totalMinutes).max().orElse(1.0);
            if (maxMinutes <= 0) {
                maxMinutes = 1.0;
            }

            int barCount = data.size();
            int totalGap = chartWidth / 6;
            int barWidth = Math.max(8, (chartWidth - totalGap) / Math.max(1, barCount));
            int gap = Math.max(4, totalGap / (barCount + 1));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));

            for (int i = 0; i < barCount; i++) {
                StatsService.DailyListeningStat stat = data.get(i);
                int barHeight = (int) ((stat.totalMinutes() / maxMinutes) * chartHeight);
                int x = padLeft + gap + i * (barWidth + gap);
                int y = padTop + chartHeight - barHeight;

                g2.setColor(STEEL_BLUE);
                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);

                String dayLabel = stat.day().length() >= 10 ? stat.day().substring(5) : stat.day();
                g2.setColor(new Color(0xA2A89A));
                FontMetrics fm = g2.getFontMetrics();
                int labelX = x + (barWidth - fm.stringWidth(dayLabel)) / 2;
                g2.drawString(dayLabel, labelX, padTop + chartHeight + 18);

                if (stat.totalMinutes() > 0) {
                    String val = stat.totalMinutes() + "m";
                    int valX = x + (barWidth - fm.stringWidth(val)) / 2;
                    g2.setColor(CREAM);
                    g2.drawString(val, valX, y - 4);
                }
            }

            g2.setColor(new Color(0xEDE8D0));
            g2.drawLine(padLeft, padTop + chartHeight, padLeft + chartWidth, padTop + chartHeight);
            g2.dispose();
        }
    }

    public static void launch(
            AuthService authService,
            LibraryService libraryService,
            PlaylistService playlistService,
            PlayerService playerService,
            StatsService statsService,
            LibraryScanService libraryScanService
    ) {
        SwingUtilities.invokeLater(() -> new VibeVaultFrame(
                authService,
                libraryService,
                playlistService,
                playerService,
                statsService,
                libraryScanService
        ).setVisible(true));
    }
}
