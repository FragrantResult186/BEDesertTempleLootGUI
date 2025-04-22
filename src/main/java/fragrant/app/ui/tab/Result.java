package fragrant.app.ui.tab;

import fragrant.app.search.ConditionalItem;
import fragrant.app.search.Searcher;
import fragrant.app.ui.Frame;
import fragrant.app.ui.ItemIcon;
import fragrant.app.ui.ProgressBar;
import fragrant.temple.loot.*;
import fragrant.temple.generator.DesertTempleGenerator;
import fragrant.utils.Position;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Result extends JPanel implements Searcher.ProgressCallback, Searcher.ResultCallback {
    public static final long MAX_SEED = (1L << 32) - 1;
    private static final int MAX_DISPLAYED_TEMPLES = 10;
    private final Searcher seedSearcher;
    private final fragrant.app.ui.Frame parent;
    private final Temple templeTab;
    private final Chest chestTab;
    private final Map<Long, Map<Position.ChunkPos, List<LootType.LootItem>>> lootCache = new ConcurrentHashMap<>();
    private JTable resultTable;
    private DefaultTableModel resultTableModel;
    private JButton findButton, clearButton;
    private JTextField startSeedField;
    private JLabel startSeedLabel, progressLabel;
    private JProgressBar progressBar;
    private ProgressBar progressBarUI;
    private JPopupMenu popupMenu;
    private Timer progressUpdateTimer;
    private Timer animationTimer;
    private boolean useBlockCoordinates = false;

    public Result(Frame parent, Temple templeTabPanel, Chest chestTabPanel) {
        this.parent = parent;
        this.templeTab = templeTabPanel;
        this.chestTab = chestTabPanel;
        this.seedSearcher = new Searcher(parent.getThreadCount());
        this.seedSearcher.setProgressCallback(this);
        this.seedSearcher.setResultCallback(this);
        this.useBlockCoordinates = parent.useBlockCoordinates();

        setLayout(new BorderLayout(5, 5));
        initUI();
        setupEventListeners();
    }

    private void initUI() {
        String[] resultColumnNames = parent.t("resultHeader").split(",");
        resultTableModel = new DefaultTableModel(resultColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex <= 2 ? Long.class : Object.class;
            }
        };
        resultTable = new JTable(resultTableModel);
        resultTable.setDefaultEditor(Object.class, null);
        resultTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column <= 2) ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });

        if (resultTable.getColumnCount() > 3) {
            resultTable.getColumnModel().getColumn(3).setCellRenderer(new ItemIcon());
        }

        JScrollPane resultScrollPane = new JScrollPane(resultTable);
        add(resultScrollPane, BorderLayout.CENTER);

        initPopupMenu();
        initControlPanel();
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenu();
        JMenuItem copyMenuItem = new JMenuItem(parent.t("copy"));
        JMenuItem clearMenuItem = new JMenuItem(parent.t("delete"));
        JMenuItem viewChestsMenuItem = new JMenuItem(parent.t("viewChests"));

        copyMenuItem.addActionListener(_ -> copySelected());
        clearMenuItem.addActionListener(_ -> deleteSelected());
        viewChestsMenuItem.addActionListener(_ -> viewChests());

        popupMenu.add(copyMenuItem);
        popupMenu.add(clearMenuItem);
        popupMenu.add(viewChestsMenuItem);
    }

    private void initControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        findButton = new JButton(parent.t("find"));
        clearButton = new JButton(parent.t("clear"));
        startSeedLabel = new JLabel(parent.t("startSeed"));
        startSeedField = new JTextField("0", 10);

        buttonPanel.add(findButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(startSeedLabel);
        buttonPanel.add(startSeedField);

        JPanel statusPanel = new JPanel(new BorderLayout(5, 5));
        progressBar = new JProgressBar(0, 100);
        progressBar.setForeground(new Color(0, 180, 0)); // 進捗バー（塗りつぶし）の色
        progressBar.setStringPainted(true);
        progressBarUI = new ProgressBar();
        progressBar.setUI(progressBarUI);

        progressLabel = new JLabel("----- seeds/sec 00:00:00");
        progressLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        statusPanel.add(progressLabel, BorderLayout.EAST);

        controlPanel.add(buttonPanel, BorderLayout.WEST);
        controlPanel.add(statusPanel, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private void showMenu(MouseEvent e) {
        int row = resultTable.rowAtPoint(e.getPoint());
        if (row >= 0 && !resultTable.isRowSelected(row)) {
            resultTable.setRowSelectionInterval(row, row);
        }
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private long parseStartSeed() {
        try {
            long seed = Long.parseLong(startSeedField.getText().trim());
            if (seed < 0) {
                startSeedField.setText("0");
                return 0;
            } else if (seed >= MAX_SEED) {
                startSeedField.setText("0");
                JOptionPane.showMessageDialog(parent, parent.t("errorInvalidSeed"));
                return -1;
            }
            return seed;
        } catch (NumberFormatException ex) {
            startSeedField.setText("0");
            JOptionPane.showMessageDialog(parent, parent.t("errorInvalidSeed"));
            return -1;
        }
    }

    private void clearAll() {
        if (resultTableModel.getRowCount() == 0) return;

        int option = JOptionPane.showConfirmDialog(parent,
                parent.t("clearResults"),
                parent.t("confirmTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            resultTableModel.setRowCount(0);
            parent.getCurrentSeed().set(0);
            startSeedField.setText("0");
            lootCache.clear();
        }
    }

    private void copySelected() {
        int[] selectedRows = resultTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        StringBuilder seedsText = new StringBuilder(selectedRows.length * 10);
        for (int i = 0; i < selectedRows.length; i++) {
            int modelRow = resultTable.convertRowIndexToModel(selectedRows[i]);
            seedsText.append(resultTableModel.getValueAt(modelRow, 0));
            if (i < selectedRows.length - 1) seedsText.append('\n');
        }

        StringSelection selection = new StringSelection(seedsText.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void deleteSelected() {
        int[] selectedRows = resultTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        int[] modelRows = new int[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            modelRows[i] = resultTable.convertRowIndexToModel(selectedRows[i]);
        }
        Arrays.sort(modelRows);

        for (int i = modelRows.length - 1; i >= 0; i--) {
            long seed = (Long) resultTableModel.getValueAt(modelRows[i], 0);
            resultTableModel.removeRow(modelRows[i]);
            lootCache.remove(seed);
        }
    }

    private void viewChests() {
        int selectedRow = resultTable.getSelectedRow();
        if (selectedRow < 0) return;

        int modelRow = resultTable.convertRowIndexToModel(selectedRow);
        long seed = (Long) resultTableModel.getValueAt(modelRow, 0);
        displayChests(seed);
    }

    private void displayChests(long seed) {
        JPanel chestsPanel = new JPanel();
        chestsPanel.setLayout(new BoxLayout(chestsPanel, BoxLayout.Y_AXIS));

        List<Position.ChunkPos> templePositions = templeTab.getTemplePositions();
        Map<Integer, int[]> templeRanges = templeTab.getTempleRanges();
        Map<Position.ChunkPos, List<ConditionalItem>> templeChests = templeTab.getTempleChests();

        for (int templeIndex = 0; templeIndex < templePositions.size(); templeIndex++) {
            int templeId = templeIndex + 1;
            Position.ChunkPos representativePos = templePositions.get(templeIndex);
            int[] range = templeRanges.get(templeId);

            List<Position.ChunkPos> templePosInRange = getTempleRange(seed, representativePos, range);
            if (templePosInRange.isEmpty()) continue;

            processTemplesInRange(seed, chestsPanel, templeIndex, templePosInRange, representativePos, templeChests);
        }

        showChestsDialog(seed, chestsPanel);
    }

    private List<Position.ChunkPos> getTempleRange(long seed, Position.ChunkPos representativePos, int[] range) {
        if (range == null) {
            List<Position.ChunkPos> singlePos = new ArrayList<>(1);
            if (DesertTempleGenerator.isTempleChunk(seed, representativePos)) {
                singlePos.add(representativePos);
            }
            return singlePos;
        } else {
            return DesertTempleGenerator.getTemplesArea(seed, range[0], range[1], range[2], range[3]);
        }
    }

    private void processTemplesInRange(long seed, JPanel chestsPanel, int templeIndex,
                                       List<Position.ChunkPos> templePosInRange,
                                       Position.ChunkPos representativePos,
                                       Map<Position.ChunkPos, List<ConditionalItem>> templeChests) {

        List<ConditionalItem> requiredItems = templeChests.get(representativePos);
        int templesAdded = 0;

        for (int posIndex = 0; posIndex < templePosInRange.size(); posIndex++) {
            Position.ChunkPos pos = templePosInRange.get(posIndex);

            List<LootType.LootItem> generatedLoot = getOrGenerateLoot(seed, pos);
            if (!Searcher.checkMatch(requiredItems, generatedLoot)) {
                continue;
            }

            JPanel templePanel = createTemplePanel(seed, templeIndex, posIndex, pos, templePosInRange.size());
            chestsPanel.add(templePanel);
            templesAdded++;

            if (templesAdded >= MAX_DISPLAYED_TEMPLES && templePosInRange.size() > MAX_DISPLAYED_TEMPLES) {
                addMoreTemplesLabel(chestsPanel, templePosInRange.size() - MAX_DISPLAYED_TEMPLES);
                break;
            }
        }
    }

    private List<LootType.LootItem> getOrGenerateLoot(long seed, Position.ChunkPos pos) {
        return lootCache.computeIfAbsent(seed, _ -> new HashMap<>())
                .computeIfAbsent(pos, p -> DesertTempleLoot.generateLoot(seed, p.x(), p.z()));
    }

    private void addMoreTemplesLabel(JPanel chestsPanel, int remainingCount) {
        JLabel moreLabel = new JLabel("... and " + remainingCount + " more temples in this range");
        moreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chestsPanel.add(moreLabel);
    }

    private JPanel createTemplePanel(long seed, int templeIndex, int posIndex, Position.ChunkPos pos, int totalPositions) {
        JPanel templePanel = new JPanel(new BorderLayout());
        String posLabel = totalPositions > 1 ? " #" + (posIndex + 1) + " of " + totalPositions : "";

        String coordsText;
        if (useBlockCoordinates) {
            Position.BlockPos blockPos = pos.toBlock();
            coordsText = "(" + blockPos.x() + ", " + blockPos.z() + ")";
        } else {
            coordsText = "(" + pos + ")";
        }

        templePanel.setBorder(BorderFactory.createTitledBorder(
                parent.t("temples") + " " + (templeIndex + 1) + posLabel + " " + coordsText));

        JTabbedPane chestTabs = createChestTabs(seed, pos);
        templePanel.add(chestTabs, BorderLayout.CENTER);

        return templePanel;
    }

    private JTabbedPane createChestTabs(long seed, Position.ChunkPos pos) {
        List<Integer> chestSeeds = DesertTempleLoot.generateChestSeed(seed, pos.x(), pos.z());
        JTabbedPane chestTabs = new JTabbedPane();

        for (int chestIndex = 0; chestIndex < chestSeeds.size(); chestIndex++) {
            int chestSeed = chestSeeds.get(chestIndex);
            List<LootType.LootItem> loot = DesertTempleLootGenerator.generateLootItems(
                    DesertTempleLootTable.getDesertTempleLootTable(), chestSeed);

            JScrollPane chestScrollPane = createChestLoot(loot);
            chestTabs.addTab(parent.t("chest") + " " + (chestIndex + 1), chestScrollPane);
        }

        return chestTabs;
    }

    private JScrollPane createChestLoot(List<LootType.LootItem> loot) {
        DefaultTableModel lootModel = new DefaultTableModel(
                new String[]{parent.t("itemName"), parent.t("itemCount")}, 0);

        for (LootType.LootItem item : loot) {
            String itemName = item.getName().replaceFirst("^minecraft:", "");
            lootModel.addRow(new Object[]{itemName, item.getCount()});
        }

        JTable lootTable = new JTable(lootModel);
        lootTable.getColumnModel().getColumn(0).setCellRenderer(new ItemIcon());
        lootTable.setDefaultEditor(Object.class, null);

        JScrollPane scrollPane = new JScrollPane(lootTable);
        scrollPane.setPreferredSize(new Dimension(300, 150));
        return scrollPane;
    }

    private void showChestsDialog(long seed, JPanel chestsPanel) {
        JScrollPane mainScrollPane = new JScrollPane(chestsPanel);
        mainScrollPane.setPreferredSize(new Dimension(600, 600));
        JOptionPane.showMessageDialog(parent,
                mainScrollPane,
                parent.t("chestsForSeed") + " " + seed,
                JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void updateLanguage() {
        findButton.setText(seedSearcher.isCalculating() ? parent.t("stop") : parent.t("find"));
        clearButton.setText(parent.t("clear"));
        startSeedLabel.setText(parent.t("startSeed"));

        Component[] menuItems = popupMenu.getComponents();
        if (menuItems.length >= 3) {
            if (menuItems[0] instanceof JMenuItem) ((JMenuItem) menuItems[0]).setText(parent.t("copy"));
            if (menuItems[1] instanceof JMenuItem) ((JMenuItem) menuItems[1]).setText(parent.t("delete"));
            if (menuItems[2] instanceof JMenuItem) ((JMenuItem) menuItems[2]).setText(parent.t("viewChests"));
        }
        updateCoordinateDisplay();
    }

    private void updateChests() {
        List<Position.ChunkPos> templePositions = templeTab.getTemplePositions();
        Map<Position.ChunkPos, List<ConditionalItem>> templeChests = templeTab.getTempleChests();

        templePositions.forEach(pos -> templeChests.put(pos, new ArrayList<>()));

        DefaultTableModel chestTableModel = chestTab.getChestTableModel();
        int rowCount = chestTableModel.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            Object templeIdObj = chestTableModel.getValueAt(i, 0);
            String itemName = (String) chestTableModel.getValueAt(i, 1);
            String chestNumberStr = (String) chestTableModel.getValueAt(i, 2);
            String conditionStr = (String) chestTableModel.getValueAt(i, 3);
            Object countObj = chestTableModel.getValueAt(i, 4);

            int templeId = templeIdObj instanceof Integer ? (Integer) templeIdObj :
                    Integer.parseInt(templeIdObj.toString());
            int count = countObj instanceof Integer ? (Integer) countObj :
                    Integer.parseInt(countObj.toString().replaceAll("\\D", ""));
            int chestNumber = "All Chests".equals(chestNumberStr) ? -1 :
                    Integer.parseInt(chestNumberStr.replaceAll("\\D+", "")) - 1;

            Position.ChunkPos pos = templePositions.get(templeId - 1);
            ConditionalItem.Condition condition = ConditionalItem.Condition.fromSymbol(conditionStr);

            List<ConditionalItem> items = templeChests.get(pos);
            items.add(new ConditionalItem(itemName, count, condition, chestNumber));
        }
    }

    private void pauseFind() {
        seedSearcher.stopSearch();
        findButton.setText(parent.t("find"));
        startSeedField.setEnabled(true);
        startSeedField.setText(String.valueOf(seedSearcher.getSeed()));
        parent.getIsCalculating().set(false);
        if (progressUpdateTimer != null) {
            progressUpdateTimer.stop();
        }
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0).replace(".0K", "K");
        return String.format("%.1fM", number / 1000000.0).replace(".0M", "M");
    }

    private String formatET(long elapsedTimeMs) {
        long seconds = elapsedTimeMs / 1000;
        return String.format("%02d:%02d:%02d",
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60);
    }

    private void find(long startSeed) {
        List<Position.ChunkPos> templePositions = templeTab.getTemplePositions();
        Map<Position.ChunkPos, List<ConditionalItem>> templeChests = templeTab.getTempleChests();
        Map<Integer, int[]> templeRanges = templeTab.getTempleRanges();

        if (templePositions.isEmpty()) {
            JOptionPane.showMessageDialog(parent, parent.t("errorNoTemples"));
            return;
        }

        boolean hasItems = templeChests.values().stream().anyMatch(items -> !items.isEmpty());
        if (!hasItems) {
            JOptionPane.showMessageDialog(parent, parent.t("errorNoItems"));
            return;
        }

        final long finalStartSeed = handleStartSeed(startSeed);
        if (finalStartSeed < 0) return;

        setupSearch();
        seedSearcher.setSearchParams(templePositions, templeChests, templeRanges);
        seedSearcher.startSearch(finalStartSeed);
        parent.getIsCalculating().set(true);
    }

    private long handleStartSeed(long startSeed) {
        if (startSeed == 0 && resultTableModel.getRowCount() > 0) {
            int option = JOptionPane.showConfirmDialog(parent,
                    parent.t("confirmClearResults"),
                    parent.t("confirmTitle"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                return -1;
            } else if (option == JOptionPane.NO_OPTION) {
                long currentSeed = seedSearcher.getSeed();
                long seed = currentSeed > 0 ? currentSeed : 0;
                startSeedField.setText(String.valueOf(seed));
                return seed;
            } else {
                resultTableModel.setRowCount(0);
                parent.getCurrentSeed().set(0);
                startSeedField.setText("0");
                return 0;
            }
        } else {
            if (startSeed == 0) {
                resultTableModel.setRowCount(0);
                parent.getCurrentSeed().set(0);
            }
            return startSeed;
        }
    }

    private void setupSearch() {
        findButton.setText(parent.t("stop"));
        startSeedField.setEnabled(false);
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);

        if (progressUpdateTimer != null) progressUpdateTimer.stop();
        progressUpdateTimer = new Timer(1000, _ -> {});
        progressUpdateTimer.start();

        // Reset animation position
        progressBarUI.setAnimationPosition(0);
        if (animationTimer != null) animationTimer.stop();
        animationTimer = new Timer(15, _ -> {
            // Update the animation position in the custom UI
            progressBarUI.updateAnimationPosition(2); // Speed: 2
            progressBar.repaint();
        });
        animationTimer.start();
    }

    private void setupEventListeners() {
        findButton.addActionListener(_ -> {
            if (seedSearcher.isCalculating()) {
                pauseFind();
            } else {
                updateChests();
                long startSeed = parseStartSeed();
                if (startSeed >= 0) {
                    find(startSeed);
                }
            }
        });

        clearButton.addActionListener(_ -> clearAll());

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showMenu(e);
                } else if (e.getClickCount() == 2) {
                    viewChests();
                }
            }
        });
    }

    @Override
    public void onProgressUpdate(double percentComplete, long currentSeed, long seedsPerSecond, long elapsedTimeMs) {
        SwingUtilities.invokeLater(() -> {
            parent.getCurrentSeed().set(currentSeed);
            progressBar.setValue((int)percentComplete);
            progressBar.setString(String.format("%.1f%%", percentComplete));
            progressLabel.setText(formatNumber(seedsPerSecond) + " seeds/sec " +
                    formatET(elapsedTimeMs));

            // 100%時に停止
            if (percentComplete >= 100.0) {
                pauseFind();
            }
        });
    }

    @Override
    public void onSearchResult(long seed, Position.ChunkPos position, List<LootType.LootItem> loot) {
        String itemSummary = Searcher.formatCounts(loot);
        SwingUtilities.invokeLater(() -> {
            if (useBlockCoordinates) {
                Position.BlockPos blockPos = position.toBlock();
                resultTableModel.addRow(new Object[]{seed, blockPos.x(), blockPos.z(), itemSummary});
            } else {
                resultTableModel.addRow(new Object[]{seed, position.x(), position.z(), itemSummary});
            }
        });
    }

    @Override
    public void onSearchComplete() {
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(false);

            if (!seedSearcher.isStopped()) {
                progressBar.setValue(100);
            }

            parent.getCurrentSeed().set(0);
            startSeedField.setEnabled(true);
            findButton.setText(parent.t("find"));
            parent.getIsCalculating().set(false);

            if (!seedSearcher.isStopped()) {
                if (resultTableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(parent, parent.t("noResults"));
                } else {
                    parent.selectResultsTab();
                }
            } else {
                if (resultTableModel.getRowCount() > 0) {
                    parent.selectResultsTab();
                }
            }

            if (progressUpdateTimer != null) {
                progressUpdateTimer.stop();
            }
            if (animationTimer != null) {
                animationTimer.stop();
            }
        });
    }

    public void updateCoordinateDisplay() {
        useBlockCoordinates = parent.useBlockCoordinates();

        String[] newHeaders;
        if (useBlockCoordinates) {
            String[] headerParts = parent.t("resultHeader").split(",");
            newHeaders = new String[] {
                    headerParts[0],
                    parent.t("blockX"),
                    parent.t("blockZ"),
                    headerParts[3]
            };
        } else {
            newHeaders = parent.t("resultHeader").split(",");
        }

        resultTableModel.setColumnIdentifiers(newHeaders);

        refreshTableData();

        resultTable.getTableHeader().repaint();
    }

    private void refreshTableData() {
        int rowCount = resultTableModel.getRowCount();
        Object[][] data = new Object[rowCount][4];

        for (int i = 0; i < rowCount; i++) {
            data[i][0] = resultTableModel.getValueAt(i, 0); // シード
            data[i][1] = resultTableModel.getValueAt(i, 1); // X
            data[i][2] = resultTableModel.getValueAt(i, 2); // Z
            data[i][3] = resultTableModel.getValueAt(i, 3); // アイテム
        }

        resultTableModel.setRowCount(0);

        for (int i = 0; i < rowCount; i++) {
            long seed = (Long) data[i][0];
            int x = ((Number) data[i][1]).intValue();
            int z = ((Number) data[i][2]).intValue();

            // 座標変換
            if (useBlockCoordinates) {
                // チャンク座標からブロック座標への変換
                x = x * 16 + 8;
                z = z * 16 + 8;
            } else {
                // ブロック座標からチャンク座標への変換
                x = (x - 8) / 16;
                z = (z - 8) / 16;
            }

            resultTableModel.addRow(new Object[]{
                    seed, x, z, data[i][3]
            });
        }
    }

}