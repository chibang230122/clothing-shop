package com.clothingshop.client.swing;

import com.clothingshop.client.lib.ShopSocketConnection;
import com.clothingshop.client.lib.ShopSocketConnection.LogLevel;
import com.clothingshop.client.lib.ShopSocketConnection.ResponseEnvelope;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ShopManagerSwingApp extends JFrame {

    private static final String PREF_HOST = "socket.host";
    private static final String PREF_PORT = "socket.port";
    private static final int MAX_LOG_LINES = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Preferences prefs = Preferences.userNodeForPackage(ShopManagerSwingApp.class);
    private final ShopSocketConnection connection = new ShopSocketConnection();

    private final JTextField hostField = new JTextField(12);
    private final JTextField portField = new JTextField(6);
    private final JButton connectBtn = new JButton("K\u1ebft n\u1ed1i");
    private final JButton disconnectBtn = new JButton("Ng\u1eaft");
    private final JLabel connDotLabel = new JLabel("\u25cf Ch\u01b0a k\u1ebft n\u1ed1i");

    private final JTextField emailField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(12);
    private final JButton loginBtn = new JButton("\u0110\u0103ng nh\u1eadp");
    private static final String PREF_SAVED_ACCOUNTS = "socket.saved.accounts";
    private final DefaultComboBoxModel<SavedAccountItem> savedAccountModel = new DefaultComboBoxModel<>();
    private final JComboBox<SavedAccountItem> savedAccountCombo = new JComboBox<>(savedAccountModel);
    private final JButton saveAccountBtn = new JButton("L\u01b0u TK");
    private final JButton delAccountBtn = new JButton("X\u00f3a TK");

    private static class SavedAccountItem {
        final String label;
        final String email;
        final String password;

        SavedAccountItem(String label, String email, String password) {
            this.label = label;
            this.email = email;
            this.password = password;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    private final JLabel welcomeUserLabel = new JLabel("Ch\u01b0a \u0111\u0103ng nh\u1eadp");

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel adminBannedTabPanel;

    private final JTextField prodSearchField = new JTextField(18);
    private final JButton prodSearchBtn = new JButton("T\u00ecm");
    private final JButton prodReloadBtn = new JButton("T\u1ea3i l\u1ea1i");
    private DefaultTableModel productTableModel;
    private JTable productTable;
    private final List cachedProducts = new ArrayList<>();

    private final JTextField detailNameField = new JTextField(20);
    private final JTextField detailPriceField = new JTextField(12);
    private final JComboBox<String> detailCategoryCombo = new JComboBox<>(new String[]{
        "\u00c1o thun", "\u00c1o s\u01a1 mi", "Qu\u1ea7n jean", "\u00c1o kho\u00e1c", "Ph\u1ee5 ki\u1ec7n"
    });
    private final JTextArea detailDescArea = new JTextArea(3, 20);
    private final JCheckBox detailActiveCheck = new JCheckBox("\u0110ang b\u00e1n", true);
    private final JLabel detailVersionLabel = new JLabel("Phi\u00ean b\u1ea3n: - (ch\u1ec9 \u0111\u1ecdc)");
    private final JButton prodSaveBtn = new JButton("L\u01b0u thay \u0111\u1ed5i");
    private final JButton prodRevertBtn = new JButton("Ho\u00e0n t\u00e1c");
    private Map selectedProductSnapshot = null;

    private DefaultTableModel stockTableModel;
    private JTable stockTable;
    private final JTextField stockSkuField = new JTextField(14);
    private final JSpinner stockDeltaSpinner = new JSpinner(new SpinnerNumberModel(1, -1000, 1000, 1));
    private final JTextField stockReasonField = new JTextField(20);
    private final JButton stockAdjustBtn = new JButton("\u0110i\u1ec1u ch\u1ec9nh t\u1ed3n kho");
    private final JButton stockReloadBtn = new JButton("T\u1ea3i l\u1ea1i");
    private DefaultTableModel stockHistoryTableModel;
    private JTable stockHistoryTable;

    private final DefaultListModel<ChatRoomItem> conversationListModel = new DefaultListModel<>();
    private final JList<ChatRoomItem> conversationList = new JList<>(conversationListModel);
    private final JButton claimChatBtn = new JButton("Nh\u1eadn h\u1ed7 tr\u1ee3");
    private final JButton refreshChatListBtn = new JButton("L\u00e0m m\u1edbi");
    private final JButton createInternalBtn = new JButton("T\u1ea1o k\u00eanh n\u1ed9i b\u1ed9");
    private final JButton addStaffBtn = new JButton("Th\u00eam nh\u00e2n vi\u00ean");
    private final JButton closeChatBtn = new JButton("\u0110\u00f3ng h\u1ed9i tho\u1ea1i");
    private final JButton deleteChatBtn = new JButton("X\u00f3a h\u1ed9i tho\u1ea1i");
    private final JPanel rightChatPanel = new JPanel(new BorderLayout(6, 6));
    private TitledBorder rightChatBorder;
    private final JLabel roomMembersLabel = new JLabel(" ");
    private final JTextArea chatHistoryArea = new JTextArea();
    private final JTextField chatMessageField = new JTextField();
    private final JButton chatSendBtn = new JButton("G\u1eedi");
    private final JLabel filterNoticeLabel = new JLabel("B\u1ed9 l\u1ecdc t\u1eeb c\u1ea5m: \u0110ang ho\u1ea1t \u0111\u1ed9ng (MASK / BLOCK)");
    private Long currentChatId = null;

    private DefaultTableModel bannedTableModel;
    private JTable bannedTable;
    private final JTextField newWordField = new JTextField(16);
    private final JComboBox<String> wordActionCombo = new JComboBox<>(new String[]{"MASK", "BLOCK"});
    private final JButton addWordBtn = new JButton("Th\u00eam t\u1eeb c\u1ea5m");
    private final JButton delWordBtn = new JButton("X\u00f3a t\u1eeb \u0111\u00e3 ch\u1ecdn");

    private final JTextPane logTextPane = new JTextPane();
    private final JLabel statusBarState = new JLabel("Tr\u1ea1ng th\u00e1i: S\u1eb5n s\u00e0ng");
    private final JLabel statusBarThread = new JLabel("Lu\u1ed3ng ph\u1ee5c v\u1ee5: -");
    private final JLabel statusBarLatency = new JLabel("Ph\u1ea3n h\u1ed3i: - ms");

    private boolean authenticated = false;
    private String currentRole = "";
    private Integer currentUserId = null;

    private static class ChatRoomItem {
        final long id;
        final String label;
        final boolean assigned;
        final boolean internal;
        final String status;
        int unread;

        ChatRoomItem(long id, String label, boolean assigned, boolean internal, String status, int unread) {
            this.id = id;
            this.label = label;
            this.assigned = assigned;
            this.internal = internal;
            this.status = status;
            this.unread = unread;
        }

        @Override
        public String toString() {
            if (internal) {
                String badge = unread > 0 ? "  (" + unread + " m\u1edbi)" : "";
                return "\u2500\u2500 N\u1ed9i b\u1ed9 \u2500\u2500 " + label + badge;
            }
            if ("CLOSED".equals(status)) {
                return "\u2715 " + label + "  [\u0110\u00e3 \u0111\u00f3ng]";
            }
            String dot = assigned ? "\u25cf " : "\u25cb ";
            String badge = unread > 0 ? "  (" + unread + " m\u1edbi)" : (assigned ? "" : "  [Ch\u01b0a nh\u1eadn]");
            return dot + label + badge;
        }
    }

    private static class StaffChoiceItem {
        final int userId;
        final String fullName;
        final String email;
        final String role;
        final boolean joined;

        StaffChoiceItem(int userId, String fullName, String email, String role, boolean joined) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.joined = joined;
        }

        @Override
        public String toString() {
            String tag = joined ? " [\u0110\u00e3 trong k\u00eanh]" : "";
            return fullName + " (" + email + " - " + role + ")" + tag;
        }
    }

    public ShopManagerSwingApp() {
        super("QU\u1ea2N L\u00dd SHOP QU\u1ea6N \u00c1O - CLIENT SOCKET");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 810);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        hostField.setText(prefs.get(PREF_HOST, "127.0.0.1"));
        portField.setText(prefs.get(PREF_PORT, "9100"));

        buildUI();
        loadSavedAccounts();
        wireEvents();
        updateControlStates();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 6, 8));

        JPanel connBox = new JPanel(new GridLayout(2, 1, 4, 4));
        connBox.setBorder(BorderFactory.createTitledBorder("C\u1ea5u h\u00ecnh k\u1ebft n\u1ed1i Socket Server"));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row1.add(new JLabel("Host:"));
        row1.add(hostField);
        row1.add(new JLabel("Port:"));
        row1.add(portField);
        row1.add(connectBtn);
        row1.add(disconnectBtn);
        connDotLabel.setForeground(new Color(180, 40, 40));
        connDotLabel.setFont(connDotLabel.getFont().deriveFont(Font.BOLD));
        row1.add(Box.createHorizontalStrut(12));
        row1.add(connDotLabel);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2.add(new JLabel("Email / T\u00e0i kho\u1ea3n:"));
        row2.add(emailField);
        row2.add(new JLabel("M\u1eadt kh\u1ea9u:"));
        row2.add(passwordField);
        row2.add(loginBtn);
        row2.add(new JLabel("T\u00e0i kho\u1ea3n l\u01b0u:"));
        savedAccountCombo.setPreferredSize(new Dimension(210, 26));
        row2.add(savedAccountCombo);
        row2.add(saveAccountBtn);
        row2.add(delAccountBtn);
        row2.add(Box.createHorizontalStrut(12));
        welcomeUserLabel.setFont(welcomeUserLabel.getFont().deriveFont(Font.BOLD));
        row2.add(welcomeUserLabel);

        connBox.add(row1);
        connBox.add(row2);
        root.add(connBox, BorderLayout.NORTH);

        tabbedPane.addTab("S\u1ea3n ph\u1ea9m", buildProductTab());
        tabbedPane.addTab("T\u1ed3n kho", buildStockTab());
        tabbedPane.addTab("Chat", buildChatTab());
        adminBannedTabPanel = buildAdminTab();
        tabbedPane.addTab("T\u1eeb c\u1ea5m (Admin)", adminBannedTabPanel);

        JPanel logPanel = buildPacketLogPanel();

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, logPanel);
        verticalSplit.setResizeWeight(0.72);
        verticalSplit.setDividerLocation(480);
        root.add(verticalSplit, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statusBar.add(statusBarState);
        statusBar.add(new JLabel("\u2502"));
        statusBar.add(statusBarThread);
        statusBar.add(new JLabel("\u2502"));
        statusBar.add(statusBarLatency);
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildProductTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        searchBar.add(new JLabel("T\u00ecm:"));
        searchBar.add(prodSearchField);
        searchBar.add(prodSearchBtn);
        searchBar.add(prodReloadBtn);
        panel.add(searchBar, BorderLayout.NORTH);

        productTableModel = new DefaultTableModel(new String[]{"M\u00e3", "T\u00ean s\u1ea3n ph\u1ea9m", "Gi\u00e1", "B\u00e1n"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(productTableModel);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(productTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Danh s\u00e1ch s\u1ea3n ph\u1ea9m"));

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder("Chi ti\u1ebft / ch\u1ec9nh s\u1eeda"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        detailPanel.add(new JLabel("T\u00ean:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        detailPanel.add(detailNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        detailPanel.add(new JLabel("Gi\u00e1:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        detailPanel.add(detailPriceField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        detailPanel.add(new JLabel("Danh m\u1ee5c:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        detailPanel.add(detailCategoryCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        detailPanel.add(new JLabel("M\u00f4 t\u1ea3:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 0.6; gbc.fill = GridBagConstraints.BOTH;
        detailDescArea.setLineWrap(true);
        detailDescArea.setWrapStyleWord(true);
        detailPanel.add(new JScrollPane(detailDescArea), gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(detailActiveCheck, gbc);
        gbc.gridx = 1;
        detailVersionLabel.setForeground(new Color(90, 90, 90));
        detailPanel.add(detailVersionLabel, gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        btnRow.add(prodSaveBtn);
        btnRow.add(prodRevertBtn);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        detailPanel.add(btnRow, gbc);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailPanel);
        split.setResizeWeight(0.55);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStockTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        stockTableModel = new DefaultTableModel(new String[]{"SKU", "S\u1ea3n ph\u1ea9m", "Size", "M\u00e0u", "T\u1ed3n hi\u1ec7n t\u1ea1i"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockTable = new JTable(stockTableModel);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(stockTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Danh s\u00e1ch bi\u1ebfn th\u1ec3 & t\u1ed3n kho"));
                stockHistoryTableModel = new DefaultTableModel(
            new Object[]{"Th\u1eddi gian", "SKU", "Tr\u01b0\u1edbc", "Thay \u0111\u1ed5i (+/-)", "Sau", "L\u00fd do", "Ng\u01b0\u1eddi s\u1eeda"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        stockHistoryTable = new JTable(stockHistoryTableModel);
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("L\u1ecbch s\u1eed \u0111i\u1ec1u ch\u1ec9nh t\u1ed3n kho"));
        historyPanel.add(new JScrollPane(stockHistoryTable), BorderLayout.CENTER);

        JSplitPane stockSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, historyPanel);
        stockSplit.setResizeWeight(0.58);
        panel.add(stockSplit, BorderLayout.CENTER);

        JPanel adjustBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        adjustBar.setBorder(BorderFactory.createTitledBorder("\u0110i\u1ec1u ch\u1ec9nh nh\u1eadp / xu\u1ea5t kho (Pessimistic Lock)"));
        adjustBar.add(new JLabel("SKU:"));
        stockSkuField.setEditable(false);
        adjustBar.add(stockSkuField);
        adjustBar.add(new JLabel("T\u0103ng / Gi\u1ea3m (+/-):"));
        adjustBar.add(stockDeltaSpinner);
        adjustBar.add(new JLabel("L\u00fd do:"));
        adjustBar.add(stockReasonField);
        adjustBar.add(stockAdjustBtn);
        adjustBar.add(stockReloadBtn);
        panel.add(adjustBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildChatTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        JPanel leftPanel = new JPanel(new BorderLayout(4, 4));
        leftPanel.setBorder(BorderFactory.createTitledBorder("H\u1ed9i tho\u1ea1i"));
        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(conversationList), BorderLayout.CENTER);

        JPanel leftBtns = new JPanel(new GridLayout(3, 2, 4, 4));
        createInternalBtn.setForeground(new Color(20, 100, 175));
        addStaffBtn.setForeground(new Color(25, 130, 60));
        deleteChatBtn.setForeground(new Color(175, 30, 30));
        leftBtns.add(claimChatBtn);
        leftBtns.add(refreshChatListBtn);
        leftBtns.add(createInternalBtn);
        leftBtns.add(addStaffBtn);
        leftBtns.add(closeChatBtn);
        leftBtns.add(deleteChatBtn);
        leftPanel.add(leftBtns, BorderLayout.SOUTH);

        rightChatBorder = BorderFactory.createTitledBorder("Ch\u01b0a ch\u1ecdn h\u1ed9i tho\u1ea1i");
        rightChatPanel.setBorder(rightChatBorder);
        roomMembersLabel.setForeground(new Color(45, 95, 145));
        roomMembersLabel.setFont(roomMembersLabel.getFont().deriveFont(Font.ITALIC, 12f));
        rightChatPanel.add(roomMembersLabel, BorderLayout.NORTH);

        chatHistoryArea.setEditable(false);
        chatHistoryArea.setLineWrap(true);
        chatHistoryArea.setWrapStyleWord(true);
        rightChatPanel.add(new JScrollPane(chatHistoryArea), BorderLayout.CENTER);

        JPanel chatBottom = new JPanel(new BorderLayout(4, 4));
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.add(chatMessageField, BorderLayout.CENTER);
        inputRow.add(chatSendBtn, BorderLayout.EAST);
        filterNoticeLabel.setForeground(new Color(140, 90, 20));
        chatBottom.add(inputRow, BorderLayout.NORTH);
        chatBottom.add(filterNoticeLabel, BorderLayout.SOUTH);
        rightChatPanel.add(chatBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightChatPanel);
        split.setResizeWeight(0.32);
        split.setDividerLocation(335);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));

        bannedTableModel = new DefaultTableModel(new String[]{"ID", "T\u1eeb c\u1ea5m", "D\u1ea1ng chu\u1ea9n h\u00f3a", "H\u00e0nh \u0111\u1ed9ng"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        bannedTable = new JTable(bannedTableModel);
        JScrollPane scroll = new JScrollPane(bannedTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Danh s\u00e1ch t\u1eeb c\u1ea5m h\u1ec7 th\u1ed1ng"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bottom.add(new JLabel("T\u1eeb m\u1edbi:"));
        bottom.add(newWordField);
        bottom.add(new JLabel("Ch\u1ebf \u0111\u1ed9:"));
        bottom.add(wordActionCombo);
        bottom.add(addWordBtn);
        bottom.add(delWordBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildPacketLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Nh\u1eadt k\u00fd g\u00f3i tin Client <-> Server"));

        logTextPane.setEditable(false);
        logTextPane.setBackground(new Color(28, 30, 34));
        logTextPane.setForeground(Color.WHITE);
        logTextPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton clearBtn = new JButton("X\u00f3a");
        JButton saveBtn = new JButton("L\u01b0u ra file");
        clearBtn.addActionListener(e -> logTextPane.setText(""));
        saveBtn.addActionListener(e -> exportLogToFile());
        topActions.add(clearBtn);
        topActions.add(saveBtn);

        panel.add(topActions, BorderLayout.NORTH);
        panel.add(new JScrollPane(logTextPane), BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        connection.setLogListener((level, msg, latencyMs) -> SwingUtilities.invokeLater(() -> appendColoredLog(level, msg, latencyMs)));
        connection.setDisconnectListener(reason -> SwingUtilities.invokeLater(() -> {
            authenticated = false;
            currentRole = "";
            connDotLabel.setText("\u25cf \u0110\u00e3 ng\u1eaft k\u1ebft n\u1ed1i");
            connDotLabel.setForeground(new Color(180, 40, 40));
            welcomeUserLabel.setText("Ch\u01b0a \u0111\u0103ng nh\u1eadp");
            statusBarState.setText("Tr\u1ea1ng th\u00e1i: M\u1ea5t k\u1ebft n\u1ed1i (" + reason + ")");
            updateControlStates();
        }));
        connection.setPushListener(frame -> SwingUtilities.invokeLater(() -> handlePushEvent(frame)));

        connectBtn.addActionListener(e -> doConnect());
        disconnectBtn.addActionListener(e -> {
            connection.close();
            statusBarState.setText("Tr\u1ea1ng th\u00e1i: \u0110\u00e3 ng\u1eaft k\u1ebft n\u1ed1i ch\u1ee7 \u0111\u1ed9ng");
        });
        loginBtn.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> { if (loginBtn.isEnabled()) doLogin(); });
        savedAccountCombo.addActionListener(e -> applySelectedSavedAccount());
        saveAccountBtn.addActionListener(e -> saveCurrentAccountToPrefs());
        delAccountBtn.addActionListener(e -> deleteSelectedSavedAccount());

        prodSearchBtn.addActionListener(e -> loadCatalogAsync(prodSearchField.getText()));
        prodReloadBtn.addActionListener(e -> {
            prodSearchField.setText("");
            loadCatalogAsync(null);
        });
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateSelectedProductDetail();
        });
        prodRevertBtn.addActionListener(e -> populateSelectedProductDetail());
        prodSaveBtn.addActionListener(e -> doSaveProduct());

        stockTable.getSelectionModel().addListSelectionListener(e -> {
            int row = stockTable.getSelectedRow();
            if (row >= 0) stockSkuField.setText(String.valueOf(stockTableModel.getValueAt(row, 0)));
        });
        stockReloadBtn.addActionListener(e -> { loadCatalogAsync(null); loadStockHistoryAsync(null); });
        stockAdjustBtn.addActionListener(e -> doAdjustStock());

        refreshChatListBtn.addActionListener(e -> loadConversationsAsync());
        conversationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) openSelectedConversation();
        });
        claimChatBtn.addActionListener(e -> doClaimSelectedChat());
        createInternalBtn.addActionListener(e -> doCreateInternalRoom());
        addStaffBtn.addActionListener(e -> doAddStaffToInternalRoom());
        closeChatBtn.addActionListener(e -> doCloseSelectedChat());
        deleteChatBtn.addActionListener(e -> doDeleteSelectedChat());
        chatSendBtn.addActionListener(e -> doSendChat());
        chatMessageField.addActionListener(e -> doSendChat());

        addWordBtn.addActionListener(e -> doAddBannedWord());
        delWordBtn.addActionListener(e -> doDeleteBannedWord());
    }

    private void loadSavedAccounts() {
        savedAccountModel.removeAllElements();
        String raw = prefs.get(PREF_SAVED_ACCOUNTS, "");
        Map<String, SavedAccountItem> map = new LinkedHashMap<>();

        // Mac dinh san 2 tai khoan test ADMIN & STAFF de doi nhanh
        map.put("admin@clothingshop.local",
            new SavedAccountItem("ADMIN - admin@clothingshop.local", "admin@clothingshop.local", "AdminShop123@"));
        map.put("staff@clothingshop.local",
            new SavedAccountItem("STAFF - staff@clothingshop.local", "staff@clothingshop.local", "AdminShop123@"));
        map.put("staff2@clothingshop.local",
            new SavedAccountItem("STAFF 2 - staff2@clothingshop.local", "staff2@clothingshop.local", "AdminShop123@"));
        map.put("staff3@clothingshop.local",
            new SavedAccountItem("STAFF 3 - staff3@clothingshop.local", "staff3@clothingshop.local", "AdminShop123@"));

        if (!raw.isBlank()) {
            for (String entry : raw.split("\\|\\|")) {
                String[] parts = entry.split("::", -1);
                if (parts.length >= 2 && !parts[1].isBlank()) {
                    String em = parts[1].trim();
                    String pw = parts.length >= 3 ? parts[2] : "AdminShop123@";
                    String lb = (parts.length >= 1 && !parts[0].isBlank()) ? parts[0].trim() : em;
                    map.put(em.toLowerCase(), new SavedAccountItem(lb, em, pw));
                }
            }
        }
        for (SavedAccountItem item : map.values()) {
            savedAccountModel.addElement(item);
        }
        if (savedAccountModel.getSize() > 0) {
            savedAccountCombo.setSelectedIndex(0);
            applySelectedSavedAccount();
        }
    }

    private void persistSavedAccounts() {
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < savedAccountModel.getSize(); i++) {
            SavedAccountItem item = savedAccountModel.getElementAt(i);
            String safeLabel = item.label.replace("::", " ").replace("||", " ");
            String safeEmail = item.email.replace("::", " ").replace("||", " ");
            String safePass = item.password.replace("::", " ").replace("||", " ");
            entries.add(safeLabel + "::" + safeEmail + "::" + safePass);
        }
        prefs.put(PREF_SAVED_ACCOUNTS, String.join("||", entries));
    }

    private void applySelectedSavedAccount() {
        Object sel = savedAccountCombo.getSelectedItem();
        if (sel instanceof SavedAccountItem item) {
            emailField.setText(item.email);
            passwordField.setText(item.password);
            if (connection.isConnected()) loginBtn.setEnabled(true);
        }
    }

    private void saveCurrentAccountToPrefs() {
        String em = emailField.getText().trim();
        String pw = new String(passwordField.getPassword());
        if (em.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng nh\u1eadp Email / T\u00e0i kho\u1ea3n tr\u01b0\u1edbc khi l\u01b0u.");
            return;
        }
        String roleTag = (!currentRole.isBlank() && em.equalsIgnoreCase(emailField.getText().trim()))
            ? (currentRole + " - ") : "";
        String lb = roleTag + em;

        int existingIdx = -1;
        for (int i = 0; i < savedAccountModel.getSize(); i++) {
            if (savedAccountModel.getElementAt(i).email.equalsIgnoreCase(em)) {
                existingIdx = i;
                break;
            }
        }
        SavedAccountItem newItem = new SavedAccountItem(lb, em, pw);
        if (existingIdx >= 0) {
            savedAccountModel.removeElementAt(existingIdx);
            savedAccountModel.insertElementAt(newItem, existingIdx);
            savedAccountCombo.setSelectedIndex(existingIdx);
        } else {
            savedAccountModel.addElement(newItem);
            savedAccountCombo.setSelectedItem(newItem);
        }
        persistSavedAccounts();
        statusBarState.setText("Tr\u1ea1ng th\u00e1i: \u0110\u00e3 l\u01b0u t\u00e0i kho\u1ea3n " + em);
    }

    private void deleteSelectedSavedAccount() {
        int idx = savedAccountCombo.getSelectedIndex();
        if (idx < 0) return;
        SavedAccountItem item = savedAccountModel.getElementAt(idx);
        savedAccountModel.removeElementAt(idx);
        persistSavedAccounts();
        statusBarState.setText("Tr\u1ea1ng th\u00e1i: \u0110\u00e3 x\u00f3a t\u00e0i kho\u1ea3n l\u01b0u " + item.email);
    }

    private void doConnect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "C\u1ed5ng (Port) ph\u1ea3i l\u00e0 s\u1ed1 nguy\u00ean h\u1ee3p l\u1ec7.", "L\u1ed7i nh\u1eadp li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            return;
        }
        prefs.put(PREF_HOST, host);
        prefs.put(PREF_PORT, String.valueOf(port));
        connectBtn.setEnabled(false);
        statusBarState.setText("Tr\u1ea1ng th\u00e1i: \u0110ang k\u1ebft n\u1ed1i...");

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                connection.connect(host, port);
                return connection.sendJsonCommand("PING", Map.of(), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    connDotLabel.setText("\u25cf \u0110\u00e3 k\u1ebft n\u1ed1i");
                    connDotLabel.setForeground(new Color(20, 140, 50));
                    updateStatusMetrics(env, "\u0110\u00e3 k\u1ebft n\u1ed1i Server");
                    updateControlStates();
                } catch (Exception ex) {
                    connDotLabel.setText("\u25cf K\u1ebft n\u1ed1i th\u1ea5t b\u1ea1i");
                    connDotLabel.setForeground(new Color(180, 40, 40));
                    statusBarState.setText("Tr\u1ea1ng th\u00e1i: L\u1ed7i k\u1ebft n\u1ed1i");
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                        "Kh\u00f4ng th\u1ec3 k\u1ebft n\u1ed1i t\u1edbi " + host + ":" + port + "\nChi ti\u1ebft: " + ex.getMessage(),
                        "L\u1ed7i k\u1ebft n\u1ed1i Socket", JOptionPane.ERROR_MESSAGE);
                    updateControlStates();
                }
            }
        }.execute();
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String pass = new String(passwordField.getPassword());
        if (email.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng nh\u1eadp \u0111\u1ea7y \u0111\u1ee7 Email/T\u00e0i kho\u1ea3n v\u00e0 M\u1eadt kh\u1ea9u.");
            return;
        }
        loginBtn.setEnabled(false);

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("AUTH", Map.of("username", email, "password", pass), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 x\u00e1c th\u1ef1c");
                    Map body = env.data();
                    if (Boolean.TRUE.equals(body.get("ok"))) {
                        Map payload = (Map) body.get("payload");
                        authenticated = true;
                        currentRole = String.valueOf(payload.get("role"));
                        currentUserId = Integer.parseInt(String.valueOf(payload.get("userId")));
                        String displayName = String.valueOf(payload.get("fullName"));
                        welcomeUserLabel.setText("Xin ch\u00e0o: " + displayName + " (" + currentRole + ")");
                        passwordField.setText("");

                        configureRoleTabs();
                        updateControlStates();
                        loadCatalogAsync(null);
                        loadConversationsAsync();
                        if ("ADMIN".equals(currentRole)) loadBannedWordsAsync();
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(body.get("error")), "\u0110\u0103ng nh\u1eadp th\u1ea5t b\u1ea1i", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i \u0111\u0103ng nh\u1eadp: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                } finally {
                    updateControlStates();
                }
            }
        }.execute();
    }

    private void configureRoleTabs() {
        int adminIdx = tabbedPane.indexOfComponent(adminBannedTabPanel);
        if ("ADMIN".equals(currentRole)) {
            if (adminIdx < 0) tabbedPane.addTab("T\u1eeb c\u1ea5m (Admin)", adminBannedTabPanel);
        } else {
            if (adminIdx >= 0) tabbedPane.removeTabAt(adminIdx);
        }
    }

    private void loadCatalogAsync(String keyword) {
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                Map p = keyword != null && !keyword.isBlank() ? Map.of("keyword", keyword.trim()) : Map.of();
                return connection.sendJsonCommand("PRODUCT_LIST", p, 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "S\u1eb5n s\u00e0ng");
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List items = (List) payload.get("items");

                    cachedProducts.clear();
                    productTableModel.setRowCount(0);
                    stockTableModel.setRowCount(0);
                    Set<Object> seenProdIds = new HashSet<>();

                    for (Object _obj_row : items) { Map row = (Map) _obj_row; 
                        Object pid = row.get("productId");
                        if (seenProdIds.add(pid)) {
                            cachedProducts.add(row);
                            boolean active = Boolean.TRUE.equals(row.get("active"));
                            productTableModel.addRow(new Object[]{
                                pid, row.get("productName"), row.get("basePrice"), active ? "C\u00f3" : "Kh\u00f4ng"
                            });
                        }
                        stockTableModel.addRow(new Object[]{
                            row.get("sku"), row.get("productName"), row.get("size"), row.get("color"), row.get("stockQuantity")
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void populateSelectedProductDetail() {
        int row = productTable.getSelectedRow();
        if (row < 0 || row >= cachedProducts.size()) {
            selectedProductSnapshot = null;
            return;
        }
        selectedProductSnapshot = (Map) cachedProducts.get(row);
        detailNameField.setText(String.valueOf(selectedProductSnapshot.get("productName")));
        detailPriceField.setText(String.valueOf(selectedProductSnapshot.get("basePrice")));
        detailCategoryCombo.setSelectedItem(String.valueOf(selectedProductSnapshot.get("categoryName")));
        detailDescArea.setText(String.valueOf(selectedProductSnapshot.get("description")));
        detailActiveCheck.setSelected(Boolean.TRUE.equals(selectedProductSnapshot.get("active")));
        detailVersionLabel.setText("Phi\u00ean b\u1ea3n: " + selectedProductSnapshot.get("productVersion") + " (ch\u1ec9 \u0111\u1ecdc)");
    }

    private void doSaveProduct() {
        if (selectedProductSnapshot == null) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t s\u1ea3n ph\u1ea9m trong danh s\u00e1ch.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "X\u00e1c nh\u1eadn l\u01b0u thay \u0111\u1ed5i cho s\u1ea3n ph\u1ea9m \"" + detailNameField.getText().trim() + "\"?",
            "X\u00e1c nh\u1eadn c\u1eadp nh\u1eadt", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Map payload = new LinkedHashMap();
        payload.put("productId", selectedProductSnapshot.get("productId"));
        payload.put("productName", detailNameField.getText().trim());
        payload.put("basePrice", detailPriceField.getText().trim());
        payload.put("description", detailDescArea.getText().trim());
        payload.put("active", detailActiveCheck.isSelected());
        payload.put("productVersion", selectedProductSnapshot.get("productVersion"));

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("PRODUCT_UPDATE", payload, 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 x\u1eed l\u00fd c\u1eadp nh\u1eadt s\u1ea3n ph\u1ea9m");
                    if (Boolean.TRUE.equals(env.data().get("ok"))) {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "\u0110\u00e3 l\u01b0u thay \u0111\u1ed5i s\u1ea3n ph\u1ea9m th\u00e0nh c\u00f4ng.");
                        loadCatalogAsync(prodSearchField.getText());
                    } else if ("OPTIMISTIC_LOCK_CONFLICT".equals(env.data().get("errorCode"))) {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            "S\u1ea3n ph\u1ea9m \u0111\u00e3 \u0111\u01b0\u1ee3c ng\u01b0\u1eddi kh\u00e1c s\u1eeda, b\u1ea5m T\u1ea3i l\u1ea1i",
                            "Xung \u0111\u1ed9t phi\u00ean b\u1ea3n", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i c\u1eadp nh\u1eadt", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i m\u1ea1ng: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doAdjustStock() {
        String sku = stockSkuField.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t bi\u1ebfn th\u1ec3 SKU t\u1eeb b\u1ea3ng.");
            return;
        }
        int delta = (Integer) stockDeltaSpinner.getValue();
        String reason = stockReasonField.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng nh\u1eadp l\u00fd do \u0111i\u1ec1u ch\u1ec9nh t\u1ed3n kho.");
            return;
        }

        Map payload = Map.of("sku", sku, "delta", delta, "reason", reason);
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("STOCK_ADJUST", payload, 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 c\u1eadp nh\u1eadt t\u1ed3n kho");
                    if (Boolean.TRUE.equals(env.data().get("ok"))) {
                        stockReasonField.setText("");
                        loadCatalogAsync(null);
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i t\u1ed3n kho", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i m\u1ea1ng: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadConversationsAsync() {
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("LIST_CONVERSATIONS", Map.of(), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List list = (List) payload.get("conversations");

                    Long keepSelectedId = currentChatId;
                    conversationListModel.clear();
                    int reselectIndex = -1;
                    for (int i = 0; i < list.size(); i++) {
                        Map c = (Map) list.get(i);
                        long id = Long.parseLong(String.valueOf(c.get("id")));
                        String title = String.valueOf(c.get("title"));
                        String status = c.get("status") != null ? String.valueOf(c.get("status")) : "OPEN";
                        boolean isInternal = "INTERNAL".equals(status);
                        boolean assigned = c.get("assignedStaffId") != null;
                        conversationListModel.addElement(new ChatRoomItem(id, title, assigned, isInternal, status, 0));
                        if (keepSelectedId != null && keepSelectedId == id) {
                            reselectIndex = i;
                        }
                    }
                    if (reselectIndex >= 0) {
                        conversationList.setSelectedIndex(reselectIndex);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void openSelectedConversation() {
        ChatRoomItem item = conversationList.getSelectedValue();
        if (item == null) {
            roomMembersLabel.setText(" ");
            return;
        }
        currentChatId = item.id;
        item.unread = 0;
        conversationList.repaint();
        boolean isClosed = "CLOSED".equals(item.status);
        rightChatBorder.setTitle(item.label + (isClosed ? "  \u25cf \u0111\u00e3 \u0111\u00f3ng" : "  \u25cf online"));
        rightChatPanel.repaint();
        chatMessageField.setEnabled(authenticated && !isClosed);
        chatSendBtn.setEnabled(authenticated && !isClosed);
        addStaffBtn.setEnabled(authenticated && item.internal);

        if (item.internal) {
            loadInternalMembersAsync(item.id);
        } else {
            roomMembersLabel.setText(" ");
        }

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("GET_MESSAGES", Map.of("conversationId", item.id), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110ang m\u1edf h\u1ed9i tho\u1ea1i #" + item.id);
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List msgs = (List) payload.get("messages");
                    chatHistoryArea.setText("");
                    for (Object _obj_m : msgs) { Map m = (Map) _obj_m; 
                        Object rawSid = m.get("senderId"); long sidNum = (rawSid instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(rawSid)); String who = (currentUserId != null && currentUserId.longValue() == sidNum) ? "B\u1ea1n" : (m.get("senderName") != null ? String.valueOf(m.get("senderName")) : ("User #" + sidNum));
                        chatHistoryArea.append(String.format("[%s] %s: %s%n",
                            formatShortTime(String.valueOf(m.get("createdAt"))), who, m.get("filteredContent")));
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void loadInternalMembersAsync(long conversationId) {
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("LIST_INTERNAL_MEMBERS", Map.of("conversationId", conversationId), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List members = (List) payload.get("members");
                    List<String> names = new ArrayList<>();
                    for (Object _obj_m : members) { Map m = (Map) _obj_m; 
                        names.add(m.get("fullName") + " (" + m.get("roleInChat") + ")");
                    }
                    roomMembersLabel.setText("Th\u00e0nh vi\u00ean k\u00eanh (" + names.size() + "): " + String.join(", ", names));
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void doCreateInternalRoom() {
        String roomTitle = JOptionPane.showInputDialog(this,
            "Nh\u1eadp t\u00ean k\u00eanh n\u1ed9i b\u1ed9 m\u1edbi (v\u00ed d\u1ee5: Nh\u00f3m Kho, Nh\u00f3m T\u01b0 v\u1ea5n):",
            "T\u1ea1o k\u00eanh n\u1ed9i b\u1ed9", JOptionPane.PLAIN_MESSAGE);
        if (roomTitle == null || roomTitle.trim().isEmpty()) return;

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("CREATE_INTERNAL_CHAT", Map.of("title", roomTitle.trim()), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 t\u1ea1o k\u00eanh n\u1ed9i b\u1ed9");
                    if (Boolean.TRUE.equals(env.data().get("ok"))) {
                        Map payload = (Map) env.data().get("payload");
                        Map room = (Map) payload.get("conversation");
                        currentChatId = Long.parseLong(String.valueOf(room.get("id")));
                        loadConversationsAsync();
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i t\u1ea1o k\u00eanh", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doAddStaffToInternalRoom() {
        ChatRoomItem item = conversationList.getSelectedValue();
        if (item == null || !item.internal) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t k\u00eanh n\u1ed9i b\u1ed9 tr\u01b0\u1edbc khi th\u00eam nh\u00e2n vi\u00ean.");
            return;
        }

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("LIST_STAFF_USERS", Map.of("conversationId", item.id), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Map payload = (Map) env.data().get("payload");
                    List staffList = (List) payload.get("staffUsers");
                    List<StaffChoiceItem> candidates = new ArrayList<>();
                    for (Object _obj_s : staffList) { Map s = (Map) _obj_s; 
                        boolean joined = Boolean.TRUE.equals(s.get("joined"));
                        if (!joined) {
                            candidates.add(new StaffChoiceItem(
                                Integer.parseInt(String.valueOf(s.get("userId"))),
                                String.valueOf(s.get("fullName")),
                                String.valueOf(s.get("email")),
                                String.valueOf(s.get("role")),
                                false
                            ));
                        }
                    }
                    if (candidates.isEmpty()) {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            "T\u1ea5t c\u1ea3 t\u00e0i kho\u1ea3n STAFF/ADMIN \u0111\u1ec1u \u0111\u00e3 c\u00f3 m\u1eb7t trong k\u00eanh \"" + item.label + "\".");
                        return;
                    }

                    StaffChoiceItem chosen = (StaffChoiceItem) JOptionPane.showInputDialog(
                        ShopManagerSwingApp.this,
                        "Ch\u1ecdn nh\u00e2n vi\u00ean th\u00eam v\u00e0o k\u00eanh \"" + item.label + "\":",
                        "Th\u00eam nh\u00e2n vi\u00ean v\u00e0o k\u00eanh n\u1ed9i b\u1ed9",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        candidates.toArray(),
                        candidates.get(0)
                    );
                    if (chosen == null) return;

                    new SwingWorker<ResponseEnvelope, Void>() {
                        @Override
                        protected ResponseEnvelope doInBackground() throws Exception {
                            return connection.sendJsonCommand("ADD_INTERNAL_MEMBER",
                                Map.of("conversationId", item.id, "userId", chosen.userId), 5);
                        }

                        @Override
                        protected void done() {
                            try {
                                ResponseEnvelope addEnv = get();
                                updateStatusMetrics(addEnv, "\u0110\u00e3 th\u00eam nh\u00e2n vi\u00ean");
                                if (Boolean.TRUE.equals(addEnv.data().get("ok"))) {
                                    loadInternalMembersAsync(item.id);
                                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                                        "\u0110\u00e3 th\u00eam " + chosen.fullName + " v\u00e0o k\u00eanh \"" + item.label + "\".");
                                } else {
                                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                                        String.valueOf(addEnv.data().get("error")), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }.execute();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doClaimSelectedChat() {
        ChatRoomItem item = conversationList.getSelectedValue();
        if (item == null || item.internal || "CLOSED".equals(item.status)) return;
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("CLAIM_CHAT", Map.of("conversationId", item.id), 5);
            }

            @Override
            protected void done() {
                loadConversationsAsync();
            }
        }.execute();
    }

    private void doCloseSelectedChat() {
        ChatRoomItem item = conversationList.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t cu\u1ed9c h\u1ed9i tho\u1ea1i.");
            return;
        }
        if (item.internal) {
            JOptionPane.showMessageDialog(this, "K\u00eanh n\u1ed9i b\u1ed9 kh\u00f4ng d\u00f9ng tr\u1ea1ng th\u00e1i \u0111\u00f3ng; Admin c\u00f3 th\u1ec3 x\u00f3a k\u00eanh n\u1ebfu kh\u00f4ng c\u00f2n s\u1eed d\u1ee5ng.");
            return;
        }
        if ("CLOSED".equals(item.status)) {
            JOptionPane.showMessageDialog(this, "Cu\u1ed9c h\u1ed9i tho\u1ea1i n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u00f3ng tr\u01b0\u1edbc \u0111\u00f3.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "X\u00e1c nh\u1eadn \u0111\u00f3ng h\u1ed9i tho\u1ea1i \"" + item.label + "\"?\n(L\u1ecbch s\u1eed tin nh\u1eafn v\u1eabn \u0111\u01b0\u1ee3c l\u01b0u tr\u1eef \u0111\u1ec3 \u0111\u1ed1i so\u00e1t)",
            "\u0110\u00f3ng h\u1ed9i tho\u1ea1i", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("CLOSE_CONVERSATION", Map.of("conversationId", item.id), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 \u0111\u00f3ng h\u1ed9i tho\u1ea1i #" + item.id);
                    if (Boolean.TRUE.equals(env.data().get("ok"))) {
                        if (currentChatId != null && currentChatId == item.id) {
                            chatMessageField.setEnabled(false);
                            chatSendBtn.setEnabled(false);
                            rightChatBorder.setTitle(item.label + "  \u25cf \u0111\u00e3 \u0111\u00f3ng");
                            rightChatPanel.repaint();
                        }
                        loadConversationsAsync();
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i \u0111\u00f3ng h\u1ed9i tho\u1ea1i", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doDeleteSelectedChat() {
        ChatRoomItem item = conversationList.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t cu\u1ed9c h\u1ed9i tho\u1ea1i ho\u1eb7c k\u00eanh n\u1ed9i b\u1ed9.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "X\u00e1c nh\u1eadn X\u00d3A V\u0128NH VI\u1ec4N \"" + item.label + "\"?\nTo\u00e0n b\u1ed9 tin nh\u1eafn v\u00e0 danh s\u00e1ch th\u00e0nh vi\u00ean s\u1ebd b\u1ecb x\u00f3a kh\u1ecfi h\u1ec7 th\u1ed1ng.",
            "X\u00f3a h\u1ed9i tho\u1ea1i (Admin)", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("DELETE_CONVERSATION", Map.of("conversationId", item.id), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "\u0110\u00e3 x\u00f3a h\u1ed9i tho\u1ea1i #" + item.id);
                    if (Boolean.TRUE.equals(env.data().get("ok"))) {
                        if (currentChatId != null && currentChatId == item.id) {
                            currentChatId = null;
                            chatHistoryArea.setText("");
                            roomMembersLabel.setText(" ");
                            rightChatBorder.setTitle("Ch\u01b0a ch\u1ecdn h\u1ed9i tho\u1ea1i");
                            rightChatPanel.repaint();
                        }
                        loadConversationsAsync();
                    } else {
                        JOptionPane.showMessageDialog(ShopManagerSwingApp.this,
                            String.valueOf(env.data().get("error")), "L\u1ed7i x\u00f3a h\u1ed9i tho\u1ea1i", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "L\u1ed7i: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void doSendChat() {
        if (currentChatId == null) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn m\u1ed9t cu\u1ed9c h\u1ed9i tho\u1ea1i tr\u01b0\u1edbc khi g\u1eedi.");
            return;
        }
        String text = chatMessageField.getText().trim();
        if (text.isEmpty()) return;
        chatMessageField.setText("");

        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("SEND_CHAT", Map.of("conversationId", currentChatId, "content", text), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    updateStatusMetrics(env, "S\u1eb5n s\u00e0ng");
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) {
                        filterNoticeLabel.setText("B\u1ed9 l\u1ecdc: " + env.data().get("error"));
                        filterNoticeLabel.setForeground(new Color(190, 30, 30));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ShopManagerSwingApp.this, "G\u1eedi tin th\u1ea5t b\u1ea1i: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadBannedWordsAsync() {
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("LIST_BANNED_WORDS", Map.of(), 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List words = (List) payload.get("words");
                    bannedTableModel.setRowCount(0);
                    for (Object _obj_w : words) { Map w = (Map) _obj_w; 
                        bannedTableModel.addRow(new Object[]{w.get("id"), w.get("word"), w.get("normalizedWord"), w.get("action")});
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void doAddBannedWord() {
        String word = newWordField.getText().trim();
        if (word.isEmpty()) return;
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("ADD_BANNED_WORD", Map.of("word", word, "action", wordActionCombo.getSelectedItem()), 5);
            }

            @Override
            protected void done() {
                newWordField.setText("");
                loadBannedWordsAsync();
            }
        }.execute();
    }

    private void doDeleteBannedWord() {
        int row = bannedTable.getSelectedRow();
        if (row < 0) return;
        int wordId = Integer.parseInt(String.valueOf(bannedTableModel.getValueAt(row, 0)));
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                return connection.sendJsonCommand("DELETE_BANNED_WORD", Map.of("wordId", wordId), 5);
            }

            @Override
            protected void done() {
                loadBannedWordsAsync();
            }
        }.execute();
    }

    private void handlePushEvent(Map frame) {
        String type = String.valueOf(frame.get("type"));
        Map payload = frame.get("payload") instanceof Map m ? m : Map.of();

        if ("CHAT_MESSAGE".equals(type)) {
            long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
            boolean masked = Boolean.TRUE.equals(payload.get("masked"));
            if (masked) {
                filterNoticeLabel.setText("B\u1ed9 l\u1ecdc: \"Tin c\u00f3 t\u1eeb kh\u00f4ng ph\u00f9 h\u1ee3p \u0111\u00e3 b\u1ecb che\"");
                filterNoticeLabel.setForeground(new Color(180, 90, 10));
            }
            if (currentChatId != null && currentChatId == convId) {
                Object rtSid = payload.get("senderId"); long rtSidNum = (rtSid instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(rtSid)); String sender = (currentUserId != null && currentUserId.longValue() == rtSidNum) ? "B\u1ea1n" : String.valueOf(payload.get("senderName"));
                chatHistoryArea.append(String.format("[%s] %s: %s%n",
                    formatShortTime(String.valueOf(payload.get("createdAt"))), sender, payload.get("content")));
                chatHistoryArea.setCaretPosition(chatHistoryArea.getDocument().getLength());
            } else {
                for (int i = 0; i < conversationListModel.size(); i++) {
                    ChatRoomItem item = conversationListModel.get(i);
                    if (item.id == convId) {
                        item.unread++;
                        conversationList.repaint();
                        break;
                    }
                }
            }
        } else if ("PRODUCT_UPDATED".equals(type) || "STOCK_UPDATED".equals(type)) {
            loadCatalogAsync(prodSearchField.getText());
        } else if ("CONVERSATION_CLAIMED".equals(type)) {
            loadConversationsAsync();
        } else if ("INTERNAL_MEMBER_ADDED".equals(type)) {
            loadConversationsAsync();
            long convId = Long.parseLong(String.valueOf(payload.get("conversationId")));
            if (currentChatId != null && currentChatId == convId) {
                loadInternalMembersAsync(convId);
            }
        } else if ("CONVERSATION_CLOSED".equals(type)) {
            long closedId = Long.parseLong(String.valueOf(payload.get("conversationId")));
            if (currentChatId != null && currentChatId == closedId) {
                chatMessageField.setEnabled(false);
                chatSendBtn.setEnabled(false);
            }
            loadConversationsAsync();
        } else if ("CONVERSATION_DELETED".equals(type)) {
            long deletedId = Long.parseLong(String.valueOf(payload.get("conversationId")));
            if (currentChatId != null && currentChatId == deletedId) {
                currentChatId = null;
                chatHistoryArea.setText("");
                roomMembersLabel.setText(" ");
                rightChatBorder.setTitle("H\u1ed9i tho\u1ea1i \u0111\u00e3 b\u1ecb x\u00f3a");
                rightChatPanel.repaint();
            }
            loadConversationsAsync();
        }
    }

    private void updateControlStates() {
        boolean conn = connection.isConnected();
        boolean isAdmin = "ADMIN".equals(currentRole);
        connectBtn.setEnabled(!conn);
        disconnectBtn.setEnabled(conn);
        loginBtn.setEnabled(conn);

        tabbedPane.setEnabled(authenticated);
        prodSearchBtn.setEnabled(authenticated);
        prodReloadBtn.setEnabled(authenticated);
        prodSaveBtn.setEnabled(authenticated);
        prodRevertBtn.setEnabled(authenticated);
        stockAdjustBtn.setEnabled(authenticated);
        stockReloadBtn.setEnabled(authenticated);
        claimChatBtn.setEnabled(authenticated);
        refreshChatListBtn.setEnabled(authenticated);
        createInternalBtn.setEnabled(authenticated);
        addStaffBtn.setEnabled(authenticated);
        closeChatBtn.setEnabled(authenticated);
        deleteChatBtn.setVisible(!authenticated || isAdmin);
        deleteChatBtn.setEnabled(authenticated && isAdmin);
        chatSendBtn.setEnabled(authenticated);
        addWordBtn.setEnabled(authenticated && isAdmin);
        delWordBtn.setEnabled(authenticated && isAdmin);
    }

    private void loadStockHistoryAsync(String skuFilter) {
        if (!connection.isConnected() || currentRole.isBlank()) return;
        new SwingWorker<ResponseEnvelope, Void>() {
            @Override
            protected ResponseEnvelope doInBackground() throws Exception {
                Map<String, Object> req = new LinkedHashMap<>();
                if (skuFilter != null && !skuFilter.isBlank()) {
                    req.put("sku", skuFilter.trim());
                }
                return connection.sendJsonCommand("LIST_STOCK_HISTORY", req, 5);
            }

            @Override
            protected void done() {
                try {
                    ResponseEnvelope env = get();
                    if (!Boolean.TRUE.equals(env.data().get("ok"))) return;
                    Map payload = (Map) env.data().get("payload");
                    List items = (List) payload.get("history");
                    stockHistoryTableModel.setRowCount(0);
                    if (items == null) return;
                    for (Object obj : items) {
                        Map row = (Map) obj;
                        String rawTime = String.valueOf(row.getOrDefault("adjustedAt", ""));
                        String fmtTime = rawTime.replace("T", " ");
                        if (fmtTime.length() > 19) fmtTime = fmtTime.substring(0, 19);

                        int prevVal = (int) Math.round(Double.parseDouble(String.valueOf(row.getOrDefault("previousStock", 0))));
                        int deltaVal = (int) Math.round(Double.parseDouble(String.valueOf(row.getOrDefault("deltaStock", 0))));
                        int newVal = (int) Math.round(Double.parseDouble(String.valueOf(row.getOrDefault("newStock", 0))));
                        String deltaStr = deltaVal > 0 ? ("+" + deltaVal) : String.valueOf(deltaVal);

                        stockHistoryTableModel.addRow(new Object[]{
                            fmtTime,
                            row.get("sku"),
                            prevVal,
                            deltaStr,
                            newVal,
                            row.get("reason"),
                            row.get("adjustedBy")
                        });
                    }
                } catch (Exception ex) {
                    statusBarState.setText("L\u1ed7i t\u1ea3i l\u1ecbch s\u1eed kho: " + ex.getMessage());
                }
            }
        }.execute();
    }
    private void updateStatusMetrics(ResponseEnvelope env, String stateText) {
        statusBarState.setText("Tr\u1ea1ng th\u00e1i: " + stateText);
        statusBarLatency.setText("Ph\u1ea3n h\u1ed3i: " + env.latencyMs() + " ms");
        Object pObj = env.data().get("payload");
        if (pObj instanceof Map m && m.get("handledBy") != null) {
            statusBarThread.setText("Lu\u1ed3ng ph\u1ee5c v\u1ee5: " + m.get("handledBy"));
        }
    }

    private void appendColoredLog(LogLevel level, String msg, Long latencyMs) {
        StyledDocument doc = logTextPane.getStyledDocument();
        Style style = logTextPane.addStyle("logStyle", null);
        Color color = switch (level) {
            case OUTGOING -> new Color(80, 220, 130);
            case INCOMING -> Color.WHITE;
            case ERROR -> new Color(255, 95, 95);
            case INFO -> new Color(150, 195, 255);
        };
        StyleConstants.setForeground(style, color);

        String ts = "[" + LocalTime.now().format(TIME_FMT) + "] ";
        String suffix = (latencyMs != null) ? "   (" + latencyMs + " ms)\n" : "\n";
        try {
            doc.insertString(doc.getLength(), ts + msg + suffix, style);
            Element root = doc.getDefaultRootElement();
            if (root.getElementCount() > MAX_LOG_LINES) {
                int endOffset = root.getElement(root.getElementCount() - MAX_LOG_LINES).getEndOffset();
                doc.remove(0, endOffset);
            }
            logTextPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private void exportLogToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("socket-evidence-" + System.currentTimeMillis() + ".log"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), logTextPane.getText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "\u0110\u00e3 l\u01b0u nh\u1eadt k\u00fd minh ch\u1ee9ng th\u00e0nh c\u00f4ng.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Kh\u00f4ng th\u1ec3 ghi file: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String formatShortTime(String iso) {
        if (iso != null && iso.length() >= 16 && iso.contains("T")) {
            return iso.substring(iso.indexOf('T') + 1, iso.indexOf('T') + 6);
        }
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
            UIManager.put("Label.font", uiFont);
            UIManager.put("Button.font", uiFont);
            UIManager.put("TextField.font", uiFont);
            UIManager.put("Table.font", uiFont);
            UIManager.put("TabbedPane.font", uiFont);

            new ShopManagerSwingApp().setVisible(true);
        });
    }
}
