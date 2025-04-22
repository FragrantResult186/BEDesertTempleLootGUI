package fragrant.temple.generator;

import fragrant.utils.BedrockRandom;
import fragrant.utils.Position;

import java.util.ArrayList;
import java.util.List;

public class DesertTempleGenerator {
    private record Config(int salt, int spacing, int sep, int n) { }

    private static Config getDesertTempleConfig() {
        return new Config(14357617, 32, 24, 2);
    }

    // 単一チャンクの速度向上のため
    public static boolean isTempleChunk(long seed, Position.ChunkPos cp) {
        Config c = getDesertTempleConfig();
        int sp = c.spacing(), se = c.sep();
        int xm = Math.floorMod(cp.x(), sp), zm = Math.floorMod(cp.z(), sp);
        int[] nums = BedrockRandom.genNums(c.salt() + (int)seed - 245998635 * Math.floorDiv(cp.z(), sp) - 1724254968 * Math.floorDiv(cp.x(), sp), c.n());
        return BedrockRandom.mod(nums[0], se) == xm && BedrockRandom.mod(nums[1], se) == zm;
    }

    public static List<Position.ChunkPos> getTemplesArea(long worldSeed, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        Config config = getDesertTempleConfig();
        int spacing = config.spacing();
        int sep = config.sep();

        List<Position.ChunkPos> temples = new ArrayList<>();

        int minGridX = Math.floorDiv(minChunkX - (sep - 1), spacing);
        int maxGridX = Math.floorDiv(maxChunkX, spacing);
        int minGridZ = Math.floorDiv(minChunkZ - (sep - 1), spacing);
        int maxGridZ = Math.floorDiv(maxChunkZ, spacing);
        int salt = config.salt() + (int) worldSeed;

        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            int xFactor = -1724254968 * gridX;

            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                int seedForGrid = salt - 245998635 * gridZ + xFactor;
                int[] nums = BedrockRandom.genNums(seedForGrid, config.n());

                int xm = BedrockRandom.mod(nums[0], sep);
                int zm = BedrockRandom.mod(nums[1], sep);
                int templeX = gridX * spacing + xm;
                int templeZ = gridZ * spacing + zm;

                if (templeX >= minChunkX && templeX <= maxChunkX && templeZ >= minChunkZ && templeZ <= maxChunkZ) {
                    temples.add(new Position.ChunkPos(templeX, templeZ));
                }
            }
        }
        return temples;
    }
}