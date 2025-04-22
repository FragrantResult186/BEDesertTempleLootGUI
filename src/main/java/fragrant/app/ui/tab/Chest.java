package fragrant.app.ui.tab;

import fragrant.app.search.ConditionalItem;
import fragrant.app.ui.Frame;
import fragrant.app.ui.ItemIcon;
import fragrant.temple.loot.DesertTempleLootTable;
import fragrant.utils.Position;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Chest extends JPanel {
    private static final String ALL_CHESTS = "All Chests";
    private static final String[] CHEST_NUMS = {ALL_CHESTS, "Chest 1", "Chest 2", "Chest 3", "Chest 4"};
    private static final String[] ITEMS = DesertTempleLootTable.getLootTable().stream().sorted().toArray(String[]::new);
    private final Frame parent;
    private final Temple templeTabPanel;
    private JTable chestTable;
    private DefaultTableModel chestTableModel;
    private JButton addItemButton;
    private JButton removeItemButton;

    public Chest(Frame parent, Temple templeTabPanel) {
        this.parent = parent;
        this.templeTabPanel = templeTabPanel;

        setLayout(new BorderLayout(5, 5));
        initUI();
        setupEventHandlers();
    }

    private void initUI() {
        createChestTable();

        JScrollPane chestScrollPane = new JScrollPane(chestTable);
        add(chestScrollPane, BorderLayout.CENTER);

        createButtonPanel();
    }

    private void createChestTable() {
        String[] chestColumnNames = getColumnNames();
        chestTableModel = new NonEditableTableModel(chestColumnNames, 0);
        chestTable = new JTable(chestTableModel);
        chestTable.setDefaultEditor(Object.class, null);

        setupTempleIdColumn();
        setupItemNameColumn();
        setupChestNumColumn();
        setupConditionColumn();
        setupCountColumn();
    }

    private String[] getColumnNames() {
        String[] chestColumnNames = parent.t("chestHeader").split(",");

        if (chestColumnNames.length == 4) {
            String[] newColumnNames = new String[5];
            System.arraycopy(chestColumnNames, 0, newColumnNames, 0, 2);
            newColumnNames[2] = parent.t("chestNumber");
            System.arraycopy(chestColumnNames, 2, newColumnNames, 3, 2);
            return newColumnNames;
        }
        return chestColumnNames;
    }

    /**
     * IDカラム
     */
    private void setupTempleIdColumn() {
        TableColumn templeIdColumn = chestTable.getColumnModel().getColumn(0);
        JTextField templeIdField = new JTextField();
        templeIdField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (!text.matches("\\d+")) return false;

                int templeId = Integer.parseInt(text);
                return templeId >= 1 && templeId <= templeTabPanel.getTemplePositions().size();
            }
        });
        templeIdColumn.setCellEditor(new DefaultCellEditor(templeIdField));
    }

    /**
     * アイテムカラム
     */
    private void setupItemNameColumn() {
        TableColumn itemNameColumn = chestTable.getColumnModel().getColumn(1);
        itemNameColumn.setCellRenderer(new ItemIcon());
    }

    /**
     * チェスト番号カラム
     */
    private void setupChestNumColumn() {
        TableColumn chestNumberColumn = chestTable.getColumnModel().getColumn(2);
        JComboBox<String> chestNumberComboBox = new JComboBox<>(CHEST_NUMS);
        chestNumberColumn.setCellEditor(new DefaultCellEditor(chestNumberComboBox));
    }

    /**
     * 条件カラム
     */
    private void setupConditionColumn() {
        TableColumn conditionColumn = chestTable.getColumnModel().getColumn(3);
        JComboBox<String> conditionComboBox = new JComboBox<>(
                Arrays.stream(ConditionalItem.Condition.values())
                        .map(ConditionalItem.Condition::getSymbol)
                        .toArray(String[]::new)
        );
        conditionColumn.setCellEditor(new DefaultCellEditor(conditionComboBox));
    }

    /**
     * 個数カラム
     */
    private void setupCountColumn() {
        TableColumn countColumn = chestTable.getColumnModel().getColumn(4);
        JTextField countField = new JTextField();
        countField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                return ((JTextField) input).getText().matches("[1-9]\\d*");
            }
        });
        countColumn.setCellEditor(new DefaultCellEditor(countField));
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        addItemButton = new JButton(parent.t("addItem"));
        removeItemButton = new JButton(parent.t("removeItem"));

        buttonPanel.add(addItemButton);
        buttonPanel.add(removeItemButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        addItemButton.addActionListener(_ -> addChestItem());
        removeItemButton.addActionListener(_ -> removeChestItem());

        chestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = chestTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        editChestItem(row);
                    }
                }
            }
        });
    }

    private void addChestItem() {
        List<Position.ChunkPos> templePositions = templeTabPanel.getTemplePositions();
        if (templePositions.isEmpty()) {
            JOptionPane.showMessageDialog(parent, parent.t("errorNoTemples"));
            return;
        }

        JPanel inputPanel = createItemInputPanel(templePositions, "", ALL_CHESTS,
                ConditionalItem.Condition.EQUAL.getSymbol(), 1);

        int result = JOptionPane.showConfirmDialog(parent, inputPanel, parent.t("inputItem"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                ItemInputData input = getItemInputData(inputPanel);

                if (input.itemName != null && !input.itemName.isEmpty() && input.count >= 0) {
                    Position.ChunkPos pos = templePositions.get(input.templeIndex);
                    ConditionalItem item = createConditionalItem(input);

                    templeTabPanel.getTempleChests().get(pos).add(item);

                    chestTableModel.addRow(new Object[]{
                            input.templeIndex + 1,
                            input.itemName,
                            input.chestNumberStr,
                            input.conditionStr,
                            input.count
                    });
                }
            } catch (NumberFormatException ex) {
                // Invalid
            }
        }
    }

    private void editChestItem(int row) {
        List<Position.ChunkPos> templePositions = templeTabPanel.getTemplePositions();
        if (templePositions.isEmpty()) {
            JOptionPane.showMessageDialog(parent, parent.t("errorNoTemples"));
            return;
        }

        String currentItemName = (String) chestTableModel.getValueAt(row, 1);
        String currentChestNumberStr = (String) chestTableModel.getValueAt(row, 2);
        String currentConditionStr = (String) chestTableModel.getValueAt(row, 3);
        Object currentCountObj = chestTableModel.getValueAt(row, 4);

        int currentCount = Integer.parseInt(currentCountObj.toString());

        JPanel inputPanel = createItemInputPanel(
                templePositions,
                currentItemName,
                currentChestNumberStr,
                currentConditionStr,
                currentCount
        );

        int result = JOptionPane.showConfirmDialog(parent, inputPanel, parent.t("editItem"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // 古いアイテム削除
                removeChestItem(row);

                ItemInputData input = getItemInputData(inputPanel);

                if (input.itemName != null && !input.itemName.isEmpty()) {
                    Position.ChunkPos pos = templePositions.get(input.templeIndex);
                    ConditionalItem item = createConditionalItem(input);

                    templeTabPanel.getTempleChests().get(pos).add(item);

                    chestTableModel.setValueAt(input.templeIndex + 1, row, 0);
                    chestTableModel.setValueAt(input.itemName, row, 1);
                    chestTableModel.setValueAt(input.chestNumberStr, row, 2);
                    chestTableModel.setValueAt(input.conditionStr, row, 3);
                    chestTableModel.setValueAt(input.count, row, 4);
                }
            } catch (NumberFormatException ex) {
                // Invalid
            }
        }
    }

    private JPanel createItemInputPanel(List<Position.ChunkPos> templePositions,
                                        String selectedItem,
                                        String selectedChestNumber,
                                        String selectedCondition,
                                        int itemCount) {

        JComboBox<String> templeComboBox = new JComboBox<>();
        templeComboBox.setName("templeComboBox");
        for (int i = 0; i < templePositions.size(); i++) {
            int[] range = templeTabPanel.getTempleRangeByIndex(i);
            String xRange = range[0] + " ~ " + range[2];
            String zRange = range[1] + " ~ " + range[3];
            templeComboBox.addItem((i + 1) + ": chunkPos{" + xRange + ", " + zRange + "}");
        }

        // アイテム名コンボボックス
        JComboBox<String> itemNameComboBox = new JComboBox<>(ITEMS);
        itemNameComboBox.setName("itemNameComboBox");
        itemNameComboBox.setRenderer(new ItemIcon());
        if (!selectedItem.isEmpty()) {
            itemNameComboBox.setSelectedItem(selectedItem);
        }

        // チェスト番号コンボボックス
        JComboBox<String> chestNumberComboBox = new JComboBox<>(CHEST_NUMS);
        chestNumberComboBox.setName("chestNumberComboBox");
        chestNumberComboBox.setSelectedItem(selectedChestNumber);

        // 条件コンボボックス
        JComboBox<String> conditionComboBox = new JComboBox<>(
                Arrays.stream(ConditionalItem.Condition.values())
                        .map(ConditionalItem.Condition::getSymbol)
                        .toArray(String[]::new)
        );
        conditionComboBox.setName("conditionComboBox");
        conditionComboBox.setSelectedItem(selectedCondition);

        JTextField itemCountField = new JTextField(String.valueOf(itemCount), 10);
        itemCountField.setName("itemCountField");
        itemCountField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                return ((JTextField) input).getText().matches("\\d+");
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel(parent.t("selectTemple")));
        panel.add(templeComboBox);
        panel.add(new JLabel(parent.t("itemName")));
        panel.add(itemNameComboBox);
        panel.add(new JLabel(parent.t("chestNumber")));
        panel.add(chestNumberComboBox);
        panel.add(new JLabel(parent.t("condition")));
        panel.add(conditionComboBox);
        panel.add(new JLabel(parent.t("itemCount")));
        panel.add(itemCountField);

        return panel;
    }

    private ItemInputData getItemInputData(JPanel panel) {
        JComboBox<?> templeComboBox = getComponentName(panel, "templeComboBox");
        JComboBox<?> itemNameComboBox = getComponentName(panel, "itemNameComboBox");
        JComboBox<?> chestNumberComboBox = getComponentName(panel, "chestNumberComboBox");
        JComboBox<?> conditionComboBox = getComponentName(panel, "conditionComboBox");
        JTextField itemCountField = getComponentName(panel, "itemCountField");

        ItemInputData data = new ItemInputData();

        data.templeIndex = Objects.requireNonNull(templeComboBox).getSelectedIndex();
        data.itemName = (String) Objects.requireNonNull(itemNameComboBox).getSelectedItem();
        data.chestNumberStr = (String) Objects.requireNonNull(chestNumberComboBox).getSelectedItem();
        data.conditionStr = (String) Objects.requireNonNull(conditionComboBox).getSelectedItem();
        data.count = Integer.parseInt(Objects.requireNonNull(itemCountField).getText().trim());
        data.chestNumber = -1;
        if (!ALL_CHESTS.equals(data.chestNumberStr)) {
            data.chestNumber = Integer.parseInt(Objects.requireNonNull(data.chestNumberStr).replaceAll("\\D+", "")) - 1;
        }

        return data;
    }

    private ConditionalItem createConditionalItem(ItemInputData input) {
        ConditionalItem.Condition condition = ConditionalItem.Condition.fromSymbol(input.conditionStr);
        return new ConditionalItem(input.itemName, input.count, condition, input.chestNumber);
    }

    private void removeChestItem() {
        int selectedRow = chestTable.getSelectedRow();
        if (selectedRow >= 0) {
            removeChestItem(selectedRow);
            chestTableModel.removeRow(selectedRow);
        }
    }

    private void removeChestItem(int row) {
        Object templeIdObj = chestTableModel.getValueAt(row, 0);
        String itemName = (String) chestTableModel.getValueAt(row, 1);
        String conditionStr = (String) chestTableModel.getValueAt(row, 3);
        Object countObj = chestTableModel.getValueAt(row, 4);

        int templeId = templeIdObj instanceof Integer ?
                (Integer) templeIdObj : Integer.parseInt(templeIdObj.toString());

        int count;
        if (countObj instanceof Integer) {
            count = (Integer) countObj;
        } else {
            String countStr = countObj.toString().replaceAll("\\D", "");
            count = countStr.isEmpty() ? 0 : Integer.parseInt(countStr);
        }

        List<Position.ChunkPos> templePositions = templeTabPanel.getTemplePositions();
        Position.ChunkPos pos = templePositions.get(templeId - 1);
        List<ConditionalItem> items = templeTabPanel.getTempleChests().get(pos);

        for (int i = 0; i < items.size(); i++) {
            ConditionalItem item = items.get(i);
            if (item.getName().equals(itemName) && item.getCount() == count
                    && item.getCompareOperator().getSymbol().equals(conditionStr)) {
                items.remove(i);
                break;
            }
        }
    }

    public void updateLanguage() {
        addItemButton.setText(parent.t("addItem"));
        removeItemButton.setText(parent.t("removeItem"));

        String[] chestColumnNames = parent.t("chestHeader").split(",");

        chestTable.getColumnModel().getColumn(0).setHeaderValue(chestColumnNames[0]);
        chestTable.getColumnModel().getColumn(1).setHeaderValue(chestColumnNames[1]);
        chestTable.getColumnModel().getColumn(2).setHeaderValue(parent.t("chestNumber"));
        chestTable.getColumnModel().getColumn(3).setHeaderValue(chestColumnNames[2]);
        chestTable.getColumnModel().getColumn(4).setHeaderValue(parent.t("itemCount"));

        chestTable.getTableHeader().repaint();
    }

    public DefaultTableModel getChestTableModel() {
        return chestTableModel;
    }

    /**
     * ヘルパー
     */
    private static class ItemInputData {
        int templeIndex;
        String itemName;
        String chestNumberStr;
        int chestNumber;
        String conditionStr;
        int count;
    }

    private static class NonEditableTableModel extends DefaultTableModel {
        public NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            // ID チェスト番号 個数カラムのみ編集可
            return column == 0 || column == 2 || column == 4;
        }
    }

    private <T extends Component> T getComponentName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (name.equals(comp.getName())) {
                return (T) comp;
            }
            if (comp instanceof Container) {
                T result = getComponentName((Container) comp, name);
                if (result != null) return result;
            }
        }
        return null;
    }

}