package fragrant.temple.loot;

import fragrant.temple.loot.LootType.*;
import fragrant.utils.BedrockRandom;

import java.util.ArrayList;
import java.util.List;

public class DesertTempleLootGenerator {
    /**
     * 指定されたシードと戦利品テーブルからアイテムを生成
     *
     * @param lootTable 戦利品テーブル
     * @param randomSeed 乱数シード
     * @return アイテムリスト
     */
    public static List<LootItem> generateLootItems(LootTable lootTable, int randomSeed) {
        BedrockRandom random = new BedrockRandom(randomSeed);
        List<LootItem> items = new ArrayList<>();

        for (LootPool pool : lootTable.pools()) {
            int totalWeight = 0;
            for (LootEntry entry : pool.getEntries()) {
                totalWeight += entry.weight();
            }
            if (totalWeight <= 0) {
                continue;
            }
            random.nextFloat(); // 乱数を消費
            int rolls;
            if (pool.getRolls() instanceof Integer) {
                random.nextInt(); // 乱数を消費
                rolls = (Integer) pool.getRolls();
            } else {
                RollRange rollRange = (RollRange) pool.getRolls();
                rolls = BedrockRandom.genRandIntRange(rollRange.min(), rollRange.max(), random);
            }

            for (int i = 0; i < rolls; i++) {
                int selectedWeight = random.nextInt(totalWeight);
                int currentWeight = 0;

                for (LootEntry entry : pool.getEntries()) {
                    currentWeight += entry.weight();
                    if (selectedWeight < currentWeight) {
                        if ("item".equals(entry.type())) {
                            items.add(generateItemFromEntry(entry, random));
                        }
                        break;
                    }
                }
            }
        }
        return items;
    }

    private static LootItem generateItemFromEntry(LootEntry entry, BedrockRandom random) {
        LootItem item = new LootItem(entry.name(), 1);

        if (entry.functions() != null) {
            for (LootFunction function : entry.functions()) {
                applyLootFunction(item, function, random);
            }
        }
        return item;
    }

    private static void applyLootFunction(LootItem item, LootFunction function, BedrockRandom random) {
        switch (function.function()) {
            case "set_count":
                CountRange countRange = function.count();
                item.setCount(BedrockRandom.genRandIntRange(countRange.min(), countRange.max(), random));
                break;
            case "enchant_randomly":
                random.nextInt(); // エンチャント用の乱数を消費
//                item.setEnchantment(generateRandomEnchantment(random));
                break;
        }
    }

// TODO: Bedrock用エンチャントを再現する必要があります。（まあ再現しなくても乱数に影響ないです）
//    private static Enchantment generateRandomEnchantment(BedrockRandom random)
//
//        return new Enchantment(enchantName, level);
//    }
}