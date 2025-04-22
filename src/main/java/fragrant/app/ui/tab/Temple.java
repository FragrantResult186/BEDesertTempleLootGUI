package fragrant.app.ui.tab;

import fragrant.app.search.ConditionalItem;
import fragrant.app.ui.Frame;
import fragrant.utils.Position;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Temple extends JPanel {
    private final Frame parent;
    private final List<Position.ChunkPos> templePositions = new ArrayList<>();
    private final Map<Position.ChunkPos, List<ConditionalItem>> templeChests = new HashMap<>();
    private final Map<Integer, int[]> templeRanges = new HashMap<>();  // Id -> [minX, minZ, maxX, maxZ]
    private JTable templeTable;
    private DefaultTableModel templeTableModel;
    private JButton addButton, removeButton;

    public Temple(Frame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(5, 5));

        createTableUI();
        createButtonPanel();
        setupListeners();
    }

    private void createTableUI() {
        String[] columnNames = parent.t("templeHeader").split(",");

        templeTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        templeTable = new JTable(templeTableModel);
        templeTable.setDefaultEditor(Object.class, null);
        templeTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(templeTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        addButton = new JButton(parent.t("addTemple"));
        removeButton = new JButton(parent.t("removeTemple"));

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        addButton.addActionListener(_ -> handleAddTemple());
        removeButton.addActionListener(_ -> handleRemoveTemple());
        templeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleEditTemple();
                }
            }
        });
    }

    public void updateLanguage() {
        addButton.setText(parent.t("addTemple"));
        removeButton.setText(parent.t("removeTemple"));

        String[] columnNames = parent.t("templeHeader").split(",");
        for (int i = 0; i < columnNames.length; i++) {
            templeTable.getColumnModel().getColumn(i).setHeaderValue(columnNames[i]);
        }
        templeTable.getTableHeader().repaint();
    }

    /**
     * ピラミッドを追加するダイアログ
     */
    private void handleAddTemple() {
        JTextField minXField = new JTextField(5);
        JTextField minZField = new JTextField(5);
        JTextField maxXField = new JTextField(5);
        JTextField maxZField = new JTextField(5);

        // 入力パネル
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel(parent.t("minChunkX")));
        panel.add(minXField);
        panel.add(new JLabel(parent.t("minChunkZ")));
        panel.add(minZField);
        panel.add(new JLabel(parent.t("maxChunkX")));
        panel.add(maxXField);
        panel.add(new JLabel(parent.t("maxChunkZ")));
        panel.add(maxZField);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                parent.t("inputTempleRange"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                int minX = Integer.parseInt(minXField.getText().trim());
                int minZ = Integer.parseInt(minZField.getText().trim());
                int maxX = Integer.parseInt(maxXField.getText().trim());
                int maxZ = Integer.parseInt(maxZField.getText().trim());

                if (minX > maxX || minZ > maxZ) {
                    JOptionPane.showMessageDialog(parent, parent.t("errorInvalidRange"));
                    return;
                }

                int centerX = (minX + maxX) / 2;
                int centerZ = (minZ + maxZ) / 2;
                Position.ChunkPos pos = new Position.ChunkPos(centerX, centerZ);

                templePositions.add(pos);
                templeChests.put(pos, new ArrayList<>());

                int templeId = templePositions.size();
                templeRanges.put(templeId, new int[]{minX, minZ, maxX, maxZ});

                templeTableModel.addRow(new Object[]{
                        templeId,
                        minX + " to " + maxX,
                        minZ + " to " + maxZ
                });

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, parent.t("errorInvalidCoordinates"));
            }
        }
    }

    private void handleRemoveTemple() {
        int selectedRow = templeTable.getSelectedRow();
        if (selectedRow < 0) return;

        Position.ChunkPos pos = templePositions.get(selectedRow);
        int removedTempleId = selectedRow + 1;

        templePositions.remove(selectedRow);
        templeChests.remove(pos);
        updateChestRemoval(removedTempleId); // 関連するチェストのエントリーも削除
        templeTableModel.removeRow(selectedRow);

        updateTempleIds();
    }

    /**
     * ピラミッド削除後、チェストタブを更新
     */
    private void updateChestRemoval(int removedTempleId) {
        Chest chestTabPanel = parent.getChestTabPanel();
        if (chestTabPanel == null) return;

        DefaultTableModel chestTableModel = chestTabPanel.getChestTableModel();

        for (int i = chestTableModel.getRowCount() - 1; i >= 0; i--) { // 後ろから検索して削除（インデックスが変わるため）
            Object templeIdObj = chestTableModel.getValueAt(i, 0);
            int templeId;

            if (templeIdObj instanceof Integer) {
                templeId = (Integer) templeIdObj;
            } else {
                templeId = Integer.parseInt(templeIdObj.toString());
            }

            // 削除されたピラミッドのチェストを削除
            if (templeId == removedTempleId) {
                chestTableModel.removeRow(i);
            }
            else if (templeId > removedTempleId) {
                chestTableModel.setValueAt(templeId - 1, i, 0);
            }
        }
    }

    /**
     * IDを振り直す（削除後）
     */
    private void updateTempleIds() {
        for (int i = 0; i < templeTableModel.getRowCount(); i++) {
            templeTableModel.setValueAt(i + 1, i, 0);
        }
    }

    private void handleEditTemple() {
        int selectedRow = templeTable.getSelectedRow();
        if (selectedRow < 0) return;

        int templeId = selectedRow + 1;
        int[] range = templeRanges.get(templeId);

        // 範囲情報がない場合は現在の位置から作成
        if (range == null) {
            Position.ChunkPos currentPos = templePositions.get(selectedRow);
            range = new int[]{currentPos.x(), currentPos.z(), currentPos.x(), currentPos.z()};
        }

        JTextField minXField = new JTextField(String.valueOf(range[0]), 10);
        JTextField minZField = new JTextField(String.valueOf(range[1]), 10);
        JTextField maxXField = new JTextField(String.valueOf(range[2]), 10);
        JTextField maxZField = new JTextField(String.valueOf(range[3]), 10);

        // 入力パネル
        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel(parent.t("minChunkX")));
        panel.add(minXField);
        panel.add(new JLabel(parent.t("minChunkZ")));
        panel.add(minZField);
        panel.add(new JLabel(parent.t("maxChunkX")));
        panel.add(maxXField);
        panel.add(new JLabel(parent.t("maxChunkZ")));
        panel.add(maxZField);

        int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                parent.t("editTemple"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                int minX = Integer.parseInt(minXField.getText().trim());
                int minZ = Integer.parseInt(minZField.getText().trim());
                int maxX = Integer.parseInt(maxXField.getText().trim());
                int maxZ = Integer.parseInt(maxZField.getText().trim());

                if (minX > maxX || minZ > maxZ) {
                    JOptionPane.showMessageDialog(parent, parent.t("errorInvalidRange"));
                    return;
                }

                int centerX = (minX + maxX) / 2;
                int centerZ = (minZ + maxZ) / 2;

                Position.ChunkPos currentPos = templePositions.get(selectedRow);
                Position.ChunkPos newPos = new Position.ChunkPos(centerX, centerZ);

                List<ConditionalItem> chests = templeChests.remove(currentPos);

                templePositions.set(selectedRow, newPos);
                templeChests.put(newPos, chests != null ? chests : new ArrayList<>());
                templeRanges.put(templeId, new int[]{minX, minZ, maxX, maxZ});
                templeTableModel.setValueAt(minX + "-" + maxX, selectedRow, 1);
                templeTableModel.setValueAt(minZ + "-" + maxZ, selectedRow, 2);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, parent.t("errorInvalidCoordinates"));
            }
        }
    }

    public List<Position.ChunkPos> getTemplePositions() {
        return templePositions;
    }

    public Map<Integer, int[]> getTempleRanges() {
        return templeRanges;
    }

    public int[] getTempleRangeByIndex(int index) {
        int templeId = index + 1;
        return templeRanges.getOrDefault(templeId, new int[]{0, 0, 0, 0});
    }

    public Map<Position.ChunkPos, List<ConditionalItem>> getTempleChests() {
        return templeChests;
    }
}