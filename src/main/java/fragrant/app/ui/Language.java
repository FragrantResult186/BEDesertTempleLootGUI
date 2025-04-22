package fragrant.app.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class Language {
    public static final String[] LANG = {"English", "日本語"};
    private static final String[] LANG_KEY = {"en", "ja"};
    private final Map<String, Properties> languageProperties = new HashMap<>();
    private final Consumer<String> languageChangeListener;
    private int selectedLanguageIndex = 0; // 0: English, 1: 日本語

    public Language(Consumer<String> languageChangeListener) {
        this.languageChangeListener = languageChangeListener;
        loadAllLanguages();
    }

    private void loadAllLanguages() {
        for (String langKey : LANG_KEY) {
            Properties props = new Properties();
            try (InputStream is = getClass().getResourceAsStream("/language/" + langKey + ".properties")) {
                if (is != null) {
                    props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
                    languageProperties.put(langKey, props);
                } else {
                    System.err.println("Language file not found: " + langKey + ".properties");
                }
            }
            catch (IOException e) {
                System.err.println("Error loading language file: " + langKey + ".properties");
                e.printStackTrace();
            }
        }
    }

    /**
     * 指定されたキーに対応する翻訳テキストを取得
     *
     * @param key 翻訳キー
     * @return 現在の言語での翻訳テキスト（見つからない場合はキーをそのまま返す）
     */
    public String get(String key) {
        String langKey = LANG_KEY[selectedLanguageIndex];
        Properties props = languageProperties.get(langKey);

        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }
        return key;
    }

    /**
     * 現在選択されている言語のインデックスを取得
     *
     * @return 言語インデックス（0: 英語 1: 日本語）
     */
    public int getLanguage() {
        return selectedLanguageIndex;
    }

    /**
     * 使用言語を変更
     *
     * @param languageIndex 設定する言語インデックス
     */
    public void setLanguage(int languageIndex) {
        if (languageIndex >= 0 && languageIndex < LANG.length) {
            this.selectedLanguageIndex = languageIndex;
            if (languageChangeListener != null) {
                languageChangeListener.accept(LANG_KEY[languageIndex]);
            }
        }
    }
}