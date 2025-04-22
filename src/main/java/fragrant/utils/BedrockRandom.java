package fragrant.utils;

import java.util.Random;

public class BedrockRandom extends Random {
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = 0x9908b0df;
    private static final int U_MASK = 0x80000000;
    private static final int L_MASK = 0x7fffffff;
    private static final int[] MAG_01 = {0, MATRIX_A};
    private static final double TWO_POW_M32 = 1.0 / (1L << 32);

    private int seed;
    private int[] mt = new int[N];
    private int mti;
    private int mtiFast;
    private boolean valid;

    public BedrockRandom() {
        this(new Random().nextInt());
    }

    public BedrockRandom(int seed) {
        valid = true;
        _setSeed(seed);
    }

    public int getSeed() {
        return seed;
    }

    @Override
    public void setSeed(long seed) {
        if (valid) setSeed((int) seed);
    }

    public void setSeed(int seed) {
        _setSeed(seed);
    }

    @Override
    public int nextInt() {
        return _genRandInt32() >>> 1;
    }

    @Override
    public int nextInt(int bound) {
        if (bound > 0) return (int) (Integer.toUnsignedLong(_genRandInt32()) % bound);
        else return 0;
    }

    public int nextInt(int a, int b) {
        return (a < b) ? a + nextInt(b - a) : a;
    }

    @Override
    public boolean nextBoolean() {
        return (_genRandInt32() & 0x8000000) != 0;
    }

    @Override
    public float nextFloat() {
        return (float) _genRandReal2();
    }

    public float nextFloat(float bound) {
        return nextFloat() * bound;
    }

    public float nextFloat(float a, float b) {
        return a + (nextFloat() * (b - a));
    }

    @Override
    public double nextDouble() {
        return _genRandReal2();
    }

    public static int[] genNums(int s, int n) {
        int[] nums = new int[n];
        int[] st = new int[n + M];
        st[0] = s;

        for(int i=1; i<st.length; i++) {
            int prev = st[i - 1];
            st[i] = (int) (0x6C078965L * (prev ^ (prev >>> 30)) + i);
        }
        for (int i = 0; i < n; i++) {
            int y = (st[i] & U_MASK) | (st[i + 1] & L_MASK);
            int val = st[i + M] ^ (y >>> 1) ^ ((y & 1) * MATRIX_A);
            nums[i] = temper(val);
        }

        return nums;
    }

    private static int temper(int y) {
        y ^= y>>>11;
        y ^= (y<<7) & 0x9D2C5680;
        y ^= (y<<15) & 0xEFC60000;
        return y ^ y>>>18;
    }

    public static int mod(int a, int n) {
        return Integer.remainderUnsigned(a, n);
    }

    @Override
    protected int next(int bits) {
        return _genRandInt32() >>> (32 - bits);
    }

    private void _setSeed(int seed) {
        this.seed = seed;
        this.mti = N + 1;
        _initGenRandFast(seed);
    }

    private void _initGenRandUnified(int initialValue, int limit) {
        mt[0] = initialValue;
        for (int i = 1; i <= limit; i++) {
            mt[i] = 1812433253 * ((mt[i - 1] >>> 30) ^ mt[i - 1]) + i;
        }
        mti = N;
        mtiFast = Math.min(limit + 1, N);
    }

    private void _initGenRand() {
        _initGenRandUnified(5489, N - 1);
    }

    private void _initGenRandFast(int initialValue) {
        _initGenRandUnified(initialValue, M);
    }

    private int _genRandInt32() {
        if (this.mti == N) {
            this.mti = 0;
        } else if (this.mti > N) {
            _initGenRand();
            this.mti = 0;
        }

        int mtCurrent = this.mt[this.mti];
        int mtNext = this.mt[this.mti + 1];

        if (this.mti >= N - M) {
            if (this.mti == N - 1) {
                this.mt[N - 1] = MAG_01[mtCurrent & 1] ^ ((mtCurrent & L_MASK | this.mt[N - 1] & U_MASK) >>> 1) ^ this.mt[M - 1];
            } else {
                this.mt[this.mti] = MAG_01[mtNext & 1] ^ ((mtNext & L_MASK | mtCurrent & U_MASK) >>> 1) ^ this.mt[this.mti - (N - M)];
            }
        } else {
            this.mt[this.mti] = MAG_01[mtNext & 1] ^ ((mtNext & L_MASK | mtCurrent & U_MASK) >>> 1) ^ this.mt[this.mti + M];
            if (this.mtiFast < N) {
                this.mt[this.mtiFast] = 1812433253 * ((this.mt[this.mtiFast - 1] >>> 30) ^ this.mt[this.mtiFast - 1]) + this.mtiFast;
                this.mtiFast++;
            }
        }

        int ret = this.mt[this.mti++];
        ret = ((ret ^ (ret >>> 11)) << 7) & 0x9d2c5680 ^ ret ^ (ret >>> 11);
        ret = (ret << 15) & 0xefc60000 ^ ret ^ (((ret << 15) & 0xefc60000 ^ ret) >>> 18);
        return ret;
    }

    private double _genRandReal2() {
        return Integer.toUnsignedLong(_genRandInt32()) * TWO_POW_M32;
    }

    public static int genRandIntRange(int min, int max, BedrockRandom random) {
        if (min >= max) {
            return min;
        }
        return random.nextInt(min, max + 1);
    }

    public static class ChunkRand {
        private final long seed;

        public ChunkRand(long seed) {
            this.seed = seed;
        }

        public BedrockRandom chunkRandom(int chunkX, int chunkZ) {
            BedrockRandom mt = new BedrockRandom((int) seed);
            long mulX = mt.nextInt() | 1;
            long mulZ = mt.nextInt() | 1;
            long chunkSeed = seed ^ (mulX * chunkX + mulZ * chunkZ);
            return new BedrockRandom((int) chunkSeed);
        }
    }
}
