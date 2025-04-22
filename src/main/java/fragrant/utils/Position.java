package fragrant.utils;

import fragrant.app.ui.Language;

public class Position {
    private static Language currentLanguage;

    /**
     * 表示言語を設定
     * @param language 使用する言語
     */
    public static void setLanguage(Language language) {
        currentLanguage = language;
    }

    /**
     * 現在の言語に応じたチャンク位置のフォーマット文字列を返す
     */
    private static String getChunkFormat() {
        return currentLanguage != null ? currentLanguage.get("chunkFmt") : "CPos{x=%d, z=%d}";
    }

    /**
     * 現在の言語に応じたブロック位置のフォーマット文字列を返す
     */
    private static String getBlockFormat() {
        return currentLanguage != null ? currentLanguage.get("blockFmt") : "BPos{x=%d, z=%d}";
    }

    public record ChunkPos(int x, int z) {
        public BlockPos toBlock() {
            return new BlockPos(x * 16 + 8, z * 16 + 8);
        }

        @Override
        public String toString() {
            return getChunkFormat().formatted(x, z);
        }
    }

    public record BlockPos(int x, int z) {
        public ChunkPos toChunk() {
            return new ChunkPos(x / 16, z / 16);
        }

        @Override
        public String toString() {
            return getBlockFormat().formatted(x, z);
        }
    }

}
