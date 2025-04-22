package fragrant.temple.loot;

import fragrant.utils.BedrockRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DesertTempleLoot {
    /**
     * 指定されたチャンク座標での砂漠の寺院の戦利品を生成
     *
     * @param worldSeed ワールドシード
     * @param chunkX チャンクX座標
     * @param chunkZ チャンクZ座標
     * @return チェスト番号ごとのアイテムリスト
     */
    public static Map<Integer, List<LootType.LootItem>> generateLootByChest(long worldSeed, int chunkX, int chunkZ) {
        List<Integer> chestSeeds = generateChestSeed(worldSeed, chunkX, chunkZ);
        Map<Integer, List<LootType.LootItem>> result = new HashMap<>();

        for (int i = 0; i < chestSeeds.size(); i++) {
            int chestSeed = chestSeeds.get(i);
            List<LootType.LootItem> items = DesertTempleLootGenerator.generateLootItems(
                    DesertTempleLootTable.getDesertTempleLootTable(),
                    chestSeed
            );
            result.put(i, items);
        }
        return result;
    }

    public static List<LootType.LootItem> generateLoot(long worldSeed, int chunkX, int chunkZ) {
        Map<Integer, List<LootType.LootItem>> chestMap = generateLootByChest(worldSeed, chunkX, chunkZ);
        List<LootType.LootItem> result = new ArrayList<>();

        for (List<LootType.LootItem> items : chestMap.values()) {
            result.addAll(items);
        }
        return result;
    }

    /**
     * チャンク座標から砂漠の寺院の４つのチェストシードを生成
     */
    public static List<Integer> generateChestSeed(long worldSeed, int chunkX, int chunkZ) {
        BedrockRandom.ChunkRand chunkRandom = new BedrockRandom.ChunkRand(worldSeed);
        BedrockRandom random = chunkRandom.chunkRandom(chunkX, chunkZ);

        random.nextInt(); // 乱数を消費
        List<Integer> seeds = new ArrayList<>();
        for (int i = 0; i < 4; i++) { // 4つのチェストシードを生成
            seeds.add(random.nextInt());
        }
        return seeds;
    }
}