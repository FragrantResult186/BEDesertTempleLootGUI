package fragrant.app.ui;

import fragrant.app.ui.tab.Chest;
import fragrant.app.ui.tab.Result;
import fragrant.app.ui.tab.Temple;
import fragrant.utils.Position;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

public class Frame extends JFrame {
    private final Preferences prefs = Preferences.userNodeForPackage(Frame.class);
    private final Language languager;
    private final AtomicBoolean isCalculating = new AtomicBoolean(false);
    private final AtomicLong currentSeed = new AtomicLong(0);
    private final Map<Integer, ImageIcon> selectedIcons = new HashMap<>();
    private final Map<Integer, ImageIcon> unselectedIcons = new HashMap<>();
    private int fontSize;
    private JTabbedPane tabPane;
    private Temple templeTab;
    private Chest chestTab;
    private Result resultTab;
    private JSpinner threadSpinner;
    private boolean useBlockCoordinates;

    public Frame() {
        languager = new Language(_ -> updateLanguage());
        Position.setLanguage(languager);

        fontSize = prefs.getInt("fontSize", 12);
        useBlockCoordinates = prefs.getBoolean("useBlockCoordinates", false);

        initUI();
    }

    private void initUI() {
        setTitle(t("title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 450);
        setLocationRelativeTo(null);

        loadAppIcon();

        // メインパネルの設定
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // トップパネル（設定ボタンを含む）
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // タブパネルの設定
        initTabs();
        mainPanel.add(tabPane, BorderLayout.CENTER);

        add(mainPanel);

        loadTabIcons();
        updateFontSize();
    }

    private void loadAppIcon() {
        try {
            URL appIconUrl = getClass().getResource("/icon/app.png");
            if (appIconUrl != null) {
                Image originalIcon = ImageIO.read(appIconUrl);
                Image resizedIcon = originalIcon.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                setIconImage(resizedIcon);
            } else {
                System.err.println("アプリアイコンが見つかりません: /icon/app.png");
            }
        } catch (Exception e) {
            System.err.println("アプリアイコンの読み込みエラー: " + e.getMessage());
        }
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton settingsButton = createSettingsButton();
        topPanel.add(settingsButton);

        return topPanel;
    }

    private JButton createSettingsButton() {
        JButton settingsButton;

        try {
            URL iconUrl = getClass().getResource("/icon/tab/Settings.png");
            if (iconUrl != null) {
                Image img = ImageIO.read(iconUrl).getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                settingsButton = new JButton(new ImageIcon(img));
            } else {
                settingsButton = new JButton("⚙");
            }
        } catch (Exception e) {
            settingsButton = new JButton("⚙");
        }

        settingsButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        settingsButton.addActionListener(_ -> showSettingsDialog());

        return settingsButton;
    }

    private void initTabs() {
        tabPane = new JTabbedPane();
        tabPane.addChangeListener(_ -> updateTabIcons());

        templeTab = new Temple(this);
        chestTab = new Chest(this, templeTab);
        resultTab = new Result(this, templeTab, chestTab);

        tabPane.addTab(t("temples"), null, templeTab);
        tabPane.addTab(t("chests"), null, chestTab);
        tabPane.addTab(t("results"), null, resultTab);
    }

    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, t("settings"), true);
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        // 言語設定
        AtomicInteger tempLanguageIndex = new AtomicInteger(languager.getLanguage());
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        langPanel.add(new JLabel(t("language") + ": "));

        JComboBox<String> langCombo = new JComboBox<>(Language.LANG);
        langCombo.setSelectedIndex(tempLanguageIndex.get());
        langCombo.addActionListener(_ -> tempLanguageIndex.set(langCombo.getSelectedIndex()));
        langPanel.add(langCombo);

        // フォントサイズ設定
        JPanel fontPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fontPanel.add(new JLabel(t("fontSize") + ": "));

        JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(fontSize, 8, 24, 1));
        fontSizeSpinner.addChangeListener(_ -> {
            fontSize = (Integer) fontSizeSpinner.getValue();
            prefs.putInt("fontSize", fontSize);
            updateFontSize();
        });
        fontPanel.add(fontSizeSpinner);

        // スレッド数設定
        JPanel threadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        threadPanel.add(new JLabel(t("threadCount") + ": "));

        int currentThreads = prefs.getInt("threadCount", Runtime.getRuntime().availableProcessors());
        threadSpinner = new JSpinner(new SpinnerNumberModel(
                currentThreads,
                1,
                Runtime.getRuntime().availableProcessors(),
                1
        ));
        threadPanel.add(threadSpinner);

        // 座標表示形式の切り替え
        AtomicBoolean tempUseBlockCoordinates = new AtomicBoolean(useBlockCoordinates);
        JPanel coordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        coordPanel.add(new JLabel(t("coordDisplay") + ": "));

        JComboBox<String> coordCombo = new JComboBox<>(new String[]{t("chunkCoords"), t("blockCoords")});
        coordCombo.setSelectedIndex(tempUseBlockCoordinates.get() ? 1 : 0);
        coordCombo.addActionListener(_ -> tempUseBlockCoordinates.set(coordCombo.getSelectedIndex() == 1));
        coordPanel.add(coordCombo);

        // OKボタン - 設定を適用
        JButton okButton = new JButton("OK");
        okButton.addActionListener(_ -> {
            // スレッド数を保存
            prefs.putInt("threadCount", (Integer) threadSpinner.getValue());

            // 言語設定を適用
            if (tempLanguageIndex.get() != languager.getLanguage()) {
                languager.setLanguage(tempLanguageIndex.get());
            }

            // 座標表示形式を適用
            if (tempUseBlockCoordinates.get() != useBlockCoordinates) {
                useBlockCoordinates = tempUseBlockCoordinates.get();
                prefs.putBoolean("useBlockCoordinates", useBlockCoordinates);
                if (resultTab != null) {
                    resultTab.updateCoordinateDisplay();
                }
            }

            dialog.dispose();
        });

        panel.add(langPanel);
        panel.add(fontPanel);
        panel.add(threadPanel);
        panel.add(coordPanel);
        panel.add(okButton);

        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateFontSize() {
        Font newFont = new Font("SansSerif", Font.PLAIN, fontSize);

        SwingUtilities.invokeLater(() -> {
            updateComponentFont(this.getContentPane(), newFont);
            revalidate();
            repaint();
        });
    }

    private void updateComponentFont(Component component, Font font) {
        component.setFont(font);

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                updateComponentFont(child, font);
            }
        }
    }

    private void loadTabIcons() {
        SwingUtilities.invokeLater(() -> {
            try {
                int tabHeight = tabPane.getUI().getTabBounds(tabPane, 0).height;
                int normalIconSize = Math.max(24, tabHeight - 10);
                int templeIconSize = Math.max(29, tabHeight);

                // ピラミッドアイコン
                loadIconPair(
                        "/icon/tab/DesertTemple1.png",
                        "/icon/tab/DesertTemple2.png",
                        0,
                        templeIconSize
                );

                // チェストアイコン
                loadIconPair(
                        "/icon/tab/Chest1.png",
                        "/icon/tab/Chest2.png",
                        1,
                        normalIconSize
                );

                // 虫眼鏡アイコン
                loadIconPair(
                        "/icon/tab/Glass1.png",
                        "/icon/tab/Glass2.png",
                        2,
                        normalIconSize
                );

                if (!selectedIcons.containsKey(2) || !unselectedIcons.containsKey(2)) {
                    loadSingleIcon("/icon/tab/Glass.png", 2, normalIconSize);
                }

                updateTabIcons();
            } catch (Exception e) {
                System.err.println("画像の読み込みまたはリサイズエラー: " + e.getMessage());
            }
        });
    }

    /**
     * アイコンペア（選択/非選択状態）をロード
     */
    private void loadIconPair(String selectedPath, String unselectedPath, int tabIndex, int size) {
        try {
            URL selectedUrl = getClass().getResource(selectedPath);
            URL unselectedUrl = getClass().getResource(unselectedPath);

            if (selectedUrl != null && unselectedUrl != null) {
                Image selectedImg = ImageIO.read(selectedUrl);
                Image unselectedImg = ImageIO.read(unselectedUrl);

                Image resizedSelectedImg = selectedImg.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                Image resizedUnselectedImg = unselectedImg.getScaledInstance(size, size, Image.SCALE_SMOOTH);

                selectedIcons.put(tabIndex, new ImageIcon(resizedSelectedImg));
                unselectedIcons.put(tabIndex, new ImageIcon(resizedUnselectedImg));
            } else {
                System.err.println("アイコンが見つかりません: " + selectedPath + " または " + unselectedPath);
            }
        } catch (Exception e) {
            System.err.println("アイコンロードエラー: " + e.getMessage());
        }
    }

    /**
     * 単一アイコンをロード（選択/非選択両方に同じアイコンを使用）
     */
    private void loadSingleIcon(String path, int tabIndex, int size) {
        try {
            URL iconUrl = getClass().getResource(path);
            if (iconUrl != null) {
                Image iconImg = ImageIO.read(iconUrl);
                Image resizedIconImg = iconImg.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(resizedIconImg);

                selectedIcons.put(tabIndex, icon);
                unselectedIcons.put(tabIndex, icon);
            } else {
                System.err.println("フォールバックアイコンが見つかりません: " + path);
            }
        } catch (Exception e) {
            System.err.println("フォールバックアイコンロードエラー: " + e.getMessage());
        }
    }

    private void updateTabIcons() {
        int selectedIndex = tabPane.getSelectedIndex();

        for (int i = 0; i < tabPane.getTabCount(); i++) {
            if (i == selectedIndex) {
                if (selectedIcons.containsKey(i)) {
                    tabPane.setIconAt(i, selectedIcons.get(i));
                }
            } else {
                if (unselectedIcons.containsKey(i)) {
                    tabPane.setIconAt(i, unselectedIcons.get(i));
                }
            }
        }
    }

    private void updateLanguage() {
        setTitle(t("title"));

        tabPane.setTitleAt(0, t("temples"));
        tabPane.setTitleAt(1, t("chests"));
        tabPane.setTitleAt(2, t("results"));

        updateTabIcons();

        templeTab.updateLanguage();
        chestTab.updateLanguage();
        resultTab.updateLanguage();
    }

    /**
     * ヘルパー
     */
    public String t(String key) {
        return languager.get(key);
    }

    public AtomicBoolean getIsCalculating() {
        return isCalculating;
    }

    public AtomicLong getCurrentSeed() {
        return currentSeed;
    }

    public void selectResultsTab() {
        tabPane.setSelectedIndex(2);
    }

    public int getThreadCount() {
        return prefs.getInt("threadCount", Runtime.getRuntime().availableProcessors());
    }

    public Chest getChestTabPanel() {
        return chestTab;
    }

    public boolean useBlockCoordinates() {
        return useBlockCoordinates;
    }
}