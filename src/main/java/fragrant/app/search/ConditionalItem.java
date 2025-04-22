package fragrant.app.search;

import fragrant.temple.loot.LootType;

public class ConditionalItem extends LootType.LootItem {
    private final Condition compareOperator;  // 比較演算子
    private final int targetChestId;

    public enum Condition {
        EQUAL("=="),
        GREATER_OR_EQUAL(">="),
        LESS_OR_EQUAL("<=");

        private final String symbol;

        Condition(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        /**
         * 記号から対応する条件を取得
         * 無効な記号の場合はデフォルトとして「以上」を返す
         */
        public static Condition fromSymbol(String symbol) {
            for (Condition c : values()) {
                if (c.symbol.equals(symbol)) {
                    return c;
                }
            }
            return GREATER_OR_EQUAL;
        }
    }

    /**
     * 条件付きアイテムを作成
     *
     * @param name アイテム名
     * @param count 数量
     * @param compareOperator 比較演算子
     * @param targetChestId 対象チェスト番号
     */
    public ConditionalItem(String name, int count, Condition compareOperator, int targetChestId) {
        super(name, count);
        this.compareOperator = compareOperator;
        this.targetChestId = targetChestId;
    }

    /**
     * 実際のアイテム数が条件を満たすか確認
     *
     * @param actualCount 実際のアイテム数
     * @return 条件を満たす場合はtrue
     */
    public boolean matchesCount(int actualCount) {
        return switch (compareOperator) {
            case EQUAL -> actualCount == getCount();
            case GREATER_OR_EQUAL -> actualCount >= getCount();
            case LESS_OR_EQUAL -> actualCount <= getCount();
        };
    }

    public Condition getCompareOperator() {
        return compareOperator;
    }

    public int getTargetChestId() {
        return targetChestId;
    }

}