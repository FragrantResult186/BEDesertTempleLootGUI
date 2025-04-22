package fragrant.app.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemIcon extends DefaultTableCellRenderer implements ListCellRenderer<String> {
    private static final int ICON_SIZE = 16;
    private static final int ICON_TEXT_GAP = 10;  // アイコンとテキストの間隔
    private static final Logger LOGGER = Logger.getLogger(ItemIcon.class.getName());
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();

    /**
     * JTable用のセルレンダラー
     */
    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (value != null) {
            applyIcon(label, value.toString());
        }

        return label;
    }

    /**
     * JList用のセルレンダラー
     */
    @Override
    public Component getListCellRendererComponent(
            JList<? extends String> list, String value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = new JLabel(value);

        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(list.getSelectionBackground());
            label.setForeground(list.getSelectionForeground());
        } else {
            label.setBackground(list.getBackground());
            label.setForeground(list.getForeground());
        }

        if (value != null) {
            applyIcon(label, value);
        }

        return label;
    }

    /**
     * ラベルにアイテムアイコンを適用する
     *
     * @param label 適用先のJLabelコンポーネント
     * @param itemName アイテム名
     */
    public static void applyIcon(JLabel label, String itemName) {
        try {
            ImageIcon icon = getItemIcon(itemName);

            if (icon != null) {
                label.setIcon(icon);
                label.setIconTextGap(ICON_TEXT_GAP);
            } else {
                label.setIcon(null);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "アイコンの読み込みに失敗しました: " + itemName, e);
            label.setIcon(null);
        }
    }

    /**
     * アイテム名に対応するアイコンを取得する（キャッシュ対応）
     *
     * @param itemName アイテム名
     * @return スケーリング済みのImageIcon、見つからない場合はnull
     */
    private static ImageIcon getItemIcon(String itemName) {
        if (iconCache.containsKey(itemName)) {
            return iconCache.get(itemName);
        }

        String iconName = itemName.replace("minecraft:", "") + ".png";
        String resourcePath = "/icon/item/" + iconName;

        try {
            ImageIcon originalIcon = new ImageIcon(Objects.requireNonNull(
                    ItemIcon.class.getResource(resourcePath)));

            if (originalIcon.getIconWidth() <= 0) {
                iconCache.put(itemName, null);
                return null;
            }

            Image originalImage = originalIcon.getImage();
            Image scaledImage = originalImage.getScaledInstance(
                    ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            iconCache.put(itemName, scaledIcon);
            return scaledIcon;

        } catch (Exception e) {
            iconCache.put(itemName, null);
            return null;
        }
    }

}