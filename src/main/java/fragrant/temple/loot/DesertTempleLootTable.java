package fragrant.temple.loot;

import fragrant.temple.loot.LootType.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DesertTempleLootTable {

    private static final LootTable DESERT_TEMPLE_LOOT_TABLE = createDesertTempleLootTable();

    public static LootTable getDesertTempleLootTable() {
        return DESERT_TEMPLE_LOOT_TABLE;
    }

    private static LootTable createDesertTempleLootTable() {
        List<LootPool> pools = new ArrayList<>();

        // pool1
        List<LootEntry> pool1 = List.of(
                entry("minecraft:diamond", 5, 1, 3),
                entry("minecraft:iron_ingot", 15, 1, 5),
                entry("minecraft:gold_ingot", 15, 2, 7),
                entry("minecraft:emerald", 15, 1, 3),
                entry("minecraft:bone", 25, 4, 6),
                entry("minecraft:spider_eye", 25, 1, 3),
                entry("minecraft:rotten_flesh", 25, 3, 7),
                entry("minecraft:saddle", 20),
                entry("minecraft:iron_horse_armor", 15),
                entry("minecraft:golden_horse_armor", 10),
                entry("minecraft:diamond_horse_armor", 5),
                entry("minecraft:enchanted_book", 20, new LootFunction("enchant_randomly", null)),
                entry("minecraft:golden_apple", 20),
                entry("minecraft:enchanted_golden_apple", 2),
                new LootEntry("", 15, "empty", null)
        );
        pools.add(new LootPool(new RollRange(2, 4), pool1));

        // pool2
        List<LootEntry> pool2 = List.of(
                entry("minecraft:bone", 10, 1, 8),
                entry("minecraft:gunpowder", 10, 1, 8),
                entry("minecraft:rotten_flesh", 10, 1, 8),
                entry("minecraft:string", 10, 1, 8),
                entry("minecraft:sand", 10, 1, 8)
        );
        pools.add(new LootPool(4, pool2));

//        // pool3
//        List<LootEntry> pool3 = List.of(
//                entry("minecraft:dune_armor_trim_smithing_template", 1, 2, 2),
//                new LootEntry("", 6, "empty", null)
//        );
//        pools.add(new LootPool(1, pool3));

        return new LootTable(pools);
    }

    private static LootEntry entry(String name, int weight, int min, int max) {
        return new LootEntry(name, weight, "item",
                Collections.singletonList(new LootFunction("set_count", new CountRange(min, max))));
    }
    private static LootEntry entry(String name, int weight) {
        return new LootEntry(name, weight, "item", null);
    }
    private static LootEntry entry(String name, int weight, LootFunction function) {
        return new LootEntry(name, weight, "item",
                Collections.singletonList(function));
    }

    public static List<String> getLootTable() {
        List<String> items = new ArrayList<>();
        for (LootPool pool : DESERT_TEMPLE_LOOT_TABLE.pools()) {
            for (LootEntry entry : pool.getEntries()) {
                if (!entry.name().isEmpty() && !"empty".equals(entry.type())) {
                    items.add(entry.name());
                }
            }
        }
        return items.stream().distinct().collect(Collectors.toList());
    }
}
