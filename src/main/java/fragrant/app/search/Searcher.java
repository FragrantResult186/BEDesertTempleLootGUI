package fragrant.app.search;

import fragrant.temple.loot.*;
import fragrant.temple.generator.DesertTempleGenerator;
import fragrant.utils.Position;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class Searcher {
    private static final long MAX_SEED_VALUE = (1L << 32) - 1;
    private static final long UPDATE_INTERVAL = MAX_SEED_VALUE / 100;
    private final AtomicLong seedsProcessed = new AtomicLong(0);
    private final AtomicLong currentSeed = new AtomicLong(0);
    private final int threadCount;
    private volatile boolean isCalculating = false;
    private volatile boolean isStopped = false;
    private long startTime;
    private List<Position.ChunkPos> templePositions;
    private Map<Position.ChunkPos, List<ConditionalItem>> templeChests;
    private Map<Integer, int[]> templeRanges;
    private ProgressCallback progressCallback;
    private ResultCallback resultCallback;
    private ExecutorService executor;

    /**
     * 検索の進捗を通知するためのコールバックインターフェース
     */
    public interface ProgressCallback {
        void updateLanguage();
        void onProgressUpdate(double percentComplete, long currentSeed, long seedsPerSecond, long elapsedTimeMs);
    }

    /**
     * 検索結果を通知するためのコールバックインターフェース
     */
    public interface ResultCallback {
        void onSearchResult(long seed, Position.ChunkPos position, List<LootType.LootItem> loot);
        void onSearchComplete();
    }

    /**
     * 指定したスレッド数で検索を行うSearcherを作成
     */
    public Searcher(int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * 進捗通知用コールバックを設定
     */
    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    /**
     * 結果通知用コールバックを設定
     */
    public void setResultCallback(ResultCallback callback) {
        this.resultCallback = callback;
    }

    /**
     * 検索パラメータを設定
     */
    public void setSearchParams(
            List<Position.ChunkPos> templePositions,
            Map<Position.ChunkPos, List<ConditionalItem>> templeChests,
            Map<Integer, int[]> templeRanges) {
        this.templePositions = new ArrayList<>(templePositions);
        this.templeChests = new HashMap<>(templeChests);
        this.templeRanges = new HashMap<>(templeRanges);
    }

    public void startSearch(long startSeed) {
        if (isCalculating) {
            return;
        }

        isCalculating = true;
        isStopped = false;
        seedsProcessed.set(0);
        startTime = System.currentTimeMillis();
        currentSeed.set(startSeed);

        executor = Executors.newFixedThreadPool(threadCount);
        final Map<Long, Integer> seedMatchCounts = Collections.synchronizedMap(new HashMap<>(100));
        final AtomicLong progressCounter = new AtomicLong(0);

        for (int threadId = 0; threadId < threadCount; threadId++) {
            final int threadFinal = threadId;
            executor.submit(() -> searchTask(startSeed + threadFinal, threadCount, progressCounter, seedMatchCounts));
        }
        monitorProgress();
    }

    public void stopSearch() {
        isStopped = true;
        isCalculating = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public long getSeed() {
        return currentSeed.get();
    }

    public boolean isCalculating() {
        return isCalculating;
    }

    public boolean isStopped() {
        return isStopped;
    }

    private void searchTask(long startFromSeed, int step, AtomicLong progressCounter, Map<Long, Integer> seedMatchCounts) {
        try {
            for (long seed = startFromSeed; seed < MAX_SEED_VALUE && !isStopped; seed += step) {
                currentSeed.set(seed);
                seedsProcessed.incrementAndGet();

                if (progressCounter.incrementAndGet() % UPDATE_INTERVAL == 0) {
                    updateProgressDisplay();
                }

                Map<Integer, List<Position.ChunkPos>> foundTemplesPerRange = getTemplePos(seed);
                if (foundTemplesPerRange == null) continue;

                processFoundTemples(seed, foundTemplesPerRange, seedMatchCounts);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたシードでピラミッドのチャンク位置を取得
     */
    private Map<Integer, List<Position.ChunkPos>> getTemplePos(long seed) {
        Map<Integer, List<Position.ChunkPos>> foundTemplesPerRange = new HashMap<>(templePositions.size());

        for (int i = 0; i < templePositions.size(); i++) {
            int templeId = i + 1;
            int[] range = templeRanges.get(templeId);

            // 特定の座標で検索する場合
            if (range == null) {
                Position.ChunkPos pos = templePositions.get(i);
                if (!DesertTempleGenerator.isTempleChunk(seed, pos)) {
                    return null;
                }

                List<Position.ChunkPos> singlePosList = new ArrayList<>(1);
                singlePosList.add(pos);
                foundTemplesPerRange.put(templeId, singlePosList);
            }
            // 範囲で検索する場合
            else {
                // 範囲が1点の場合
                if (range[0] == range[2] && range[1] == range[3]) {
                    Position.ChunkPos pos = new Position.ChunkPos(range[0], range[1]);
                    if (!DesertTempleGenerator.isTempleChunk(seed, pos)) {
                        return null;
                    }

                    List<Position.ChunkPos> singlePosList = new ArrayList<>(1);
                    singlePosList.add(pos);
                    foundTemplesPerRange.put(templeId, singlePosList);
                }
                // 実際の範囲検索
                else {
                    List<Position.ChunkPos> rangeTemples = DesertTempleGenerator.getTemplesArea(
                            seed, range[0], range[1], range[2], range[3]);
                    if (rangeTemples.isEmpty()) {
                        return null;
                    }

                    foundTemplesPerRange.put(templeId, rangeTemples);
                }
            }
        }

        return foundTemplesPerRange;
    }

    /**
     * 見つかったピラミッドの内容を検証し、条件に一致するかチェック
     */
    private void processFoundTemples(long seed, Map<Integer, List<Position.ChunkPos>> foundTemplesPerRange,
                                     Map<Long, Integer> seedMatchCounts) {
        int matchCount = 0;
        Map<Position.ChunkPos, List<LootType.LootItem>> cachedLoot = new HashMap<>();
        Position.ChunkPos matchingPos = null;

        for (int i = 0; i < templePositions.size(); i++) {
            int templeId = i + 1;
            List<Position.ChunkPos> templePosInRange = foundTemplesPerRange.get(templeId);
            Position.ChunkPos representativePos = templePositions.get(i);
            List<ConditionalItem> knownContents = templeChests.get(representativePos);

            if (knownContents.isEmpty()) continue;

            boolean foundMatch = false;
            for (Position.ChunkPos pos : templePosInRange) {
                List<LootType.LootItem> generatedLoot = cachedLoot.computeIfAbsent(pos,
                        p -> DesertTempleLoot.generateLoot(seed, p.x(), p.z()));

                if (checkMatch(knownContents, generatedLoot)) {
                    matchCount++;
                    foundMatch = true;
                    if (matchingPos == null) matchingPos = pos;
                    break;
                }
            }

            if (!foundMatch) {
                return;
            }
        }

        if (matchCount > 0) {
            seedMatchCounts.put(seed, matchCount);
            Position.ChunkPos actualPos = matchingPos != null ? matchingPos :
                    getSimpleTemplePos(seed, foundTemplesPerRange);
            List<LootType.LootItem> loot = cachedLoot.getOrDefault(actualPos,
                    DesertTempleLoot.generateLoot(seed, actualPos.x(), actualPos.z()));

            if (resultCallback != null) {
                resultCallback.onSearchResult(seed, actualPos, loot);
            }
        }
    }

    private void monitorProgress() {
        new Thread(() -> {
            try {
                while (isCalculating && !executor.isTerminated()) {
                    Thread.sleep(500);
                    updateProgressDisplay();
                }

                executor.shutdown();
                isCalculating = false;

                if (resultCallback != null) {
                    resultCallback.onSearchComplete();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 条件アイテムリストと実際の戦利品が条件を満たすかチェック
     */
    public static boolean checkMatch(List<ConditionalItem> requiredItems, List<LootType.LootItem> lootItems) {
        if (requiredItems.isEmpty()) return true;
        if (lootItems.isEmpty()) return false;

        Map<Integer, List<LootType.LootItem>> chestItems = chestSplit(lootItems);
        Map<String, Map<Integer, Integer>> itemCountsByChest = new HashMap<>();

        for (int chestIdx = 0; chestIdx < chestItems.size(); chestIdx++) {
            for (LootType.LootItem item : chestItems.get(chestIdx)) {
                itemCountsByChest
                        .computeIfAbsent(item.getName(), _ -> new HashMap<>())
                        .merge(chestIdx, item.getCount(), Integer::sum);
            }
        }

        // アイテム合計数
        Map<String, Integer> totalCounts = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Integer>> entry : itemCountsByChest.entrySet()) {
            int sum = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            totalCounts.put(entry.getKey(), sum);
        }

        for (ConditionalItem required : requiredItems) {
            String itemName = required.getName();

            // 全チェスト合計での条件
            if (required.getTargetChestId() == -1) {
                int totalCount = totalCounts.getOrDefault(itemName, 0);
                if (!required.matchesCount(totalCount)) {
                    return false;
                }
            }
            // 特定チェストでの条件
            else {
                Map<Integer, Integer> chestCounts = itemCountsByChest.getOrDefault(itemName, Collections.emptyMap());
                int chestCount = chestCounts.getOrDefault(required.getTargetChestId(), 0);
                if (!required.matchesCount(chestCount)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 戦利品アイテムを4つのチェストに分配
     */
    public static Map<Integer, List<LootType.LootItem>> chestSplit(List<LootType.LootItem> items) {
        Map<Integer, List<LootType.LootItem>> map = new HashMap<>(4);
        for (int i = 0; i < 4; i++) {
            map.put(i, new ArrayList<>());
        }

        int itemsPerChest = (int) Math.ceil(items.size() / 4.0);
        for (int i = 0; i < items.size(); i++) {
            int chestIdx = i / itemsPerChest;
            map.get(chestIdx).add(items.get(i));
        }
        return map;
    }

    /**
     * 最初の有効なピラミッド位置を取得
     */
    private Position.ChunkPos getSimpleTemplePos(long seed, Map<Integer, List<Position.ChunkPos>> foundTemplesPerRange) {
        for (List<Position.ChunkPos> temples : foundTemplesPerRange.values()) {
            for (Position.ChunkPos pos : temples) {
                if (DesertTempleGenerator.isTempleChunk(seed, pos)) {
                    return pos;
                }
            }
        }
        return templePositions.isEmpty() ? new Position.ChunkPos(0, 0) : templePositions.getFirst();
    }

    private void updateProgressDisplay() {
        if (progressCallback != null) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            long currentProcessed = seedsProcessed.get();
            double seedsPerSecond = elapsedTime > 0 ? (currentProcessed * 1000.0 / elapsedTime) : 0;
            double percentComplete = (currentSeed.get() * 100.0 / MAX_SEED_VALUE);

            progressCallback.onProgressUpdate(percentComplete, currentSeed.get(), (long)seedsPerSecond, elapsedTime);
        }
    }

    /**
     * 戦利品アイテムのリストを読みやすい文字列形式にフォーマット
     */
    public static String formatCounts(List<LootType.LootItem> loot) {
        Map<String, Integer> counts = new LinkedHashMap<>(loot.size());

        // アイテム名でグループ化してカウント
        for (LootType.LootItem item : loot) {
            String itemName = item.getName().replaceFirst("^minecraft:", "");
            counts.merge(itemName, item.getCount(), Integer::sum);
        }

        StringBuilder result = new StringBuilder(counts.size() * 15);
        boolean first = true;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!first) {
                result.append(", ");
            }
            result.append(entry.getKey()).append(" x").append(entry.getValue());
            first = false;
        }
        return result.toString();
    }
}