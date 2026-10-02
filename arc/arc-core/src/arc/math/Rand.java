package arc.math;

import java.util.Random;

/**
 * This class implements the xorshift128+ algorithm that is a very fast, top-quality 64-bit pseudo-random number generator. The
 * quality of this PRNG is much higher than {@link Random}'s, and its cycle length is 2<sup>128</sup>&nbsp;&minus;&nbsp;1, which
 * is more than enough for any single-thread application. More details and algorithms can be found <a
 * href="http://xorshift.di.unimi.it/">here</a>.
 * <p>
 * Instances of RandomXS128 are not thread-safe.
 * @author Inferno
 * @author davebaol
 */
public class Rand extends Random{
    /** Normalization constant for double. */
    private static final double NORM_DOUBLE = 1.0 / (1L << 53);
    /** Normalization constant for float. */
    private static final double NORM_FLOAT = 1.0 / (1L << 24);

    /** The first half of the internal state of this pseudo-random number generator. */
    public long seed0;
    /** The second half of the internal state of this pseudo-random number generator. */
    public long seed1;

    /**
     * Creates a new random number generator. This constructor sets the seed of the random number generator to a value very likely
     * to be distinct from any other invocation of this constructor.
     * <p>
     * This implementation creates a {@link Random} instance to generate the initial seed.
     */
    public Rand(){
        setSeed(new Random().nextLong());
    }

    /**
     * Creates a new random number generator using a single {@code long} seed.
     * @param seed the initial seed
     */
    public Rand(long seed){
        setSeed(seed);
    }

    /**
     * Creates a new random number generator using two {@code long} seeds.
     * @param seed0 the first part of the initial seed
     * @param seed1 the second part of the initial seed
     */
    public Rand(long seed0, long seed1){
        setState(seed0, seed1);
    }

    private static long murmurHash3(long x){
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;

        return x;
    }

    /**
     * Returns the next pseudo-random, uniformly distributed {@code long} value from this random number generator's sequence.
     * <p>
     * Subclasses should override this, as this is used by all other methods.
     */
    @Override
    public long nextLong(){
        long s1 = this.seed0;
        final long s0 = this.seed1;
        this.seed0 = s0;
        s1 ^= s1 << 23;
        return (this.seed1 = (s1 ^ s0 ^ (s1 >>> 17) ^ (s0 >>> 26))) + s0;
    }

    /** This protected method is final because, contrary to the superclass, it's not used anymore by the other methods. */
    @Override
    protected final int next(int bits){
        return (int)(nextLong() & ((1L << bits) - 1));
    }

    /**
     * Returns the next pseudo-random, uniformly distributed {@code int} value from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     */
    @Override
    public int nextInt(){
        return (int)nextLong();
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code int} value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudo-random {@code int} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
    @Override
    public int nextInt(final int n){
        return (int)nextLong(n);
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code long} value between 0 (inclusive) and the specified value (exclusive),
     * drawn from this random number generator's sequence. The algorithm used to generate the value guarantees that the result is
     * uniform, provided that the sequence of 64-bit values produced by this generator is.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudo-random {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
    public long nextLong(final long n){
        if(n <= 0) throw new IllegalArgumentException("n must be positive");
        for(;;){
            final long bits = nextLong() >>> 1;
            final long value = bits % n;
            if(bits - value + (n - 1) >= 0) return value;
        }
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code double} value between 0.0 and 1.0 from this random number generator's
     * sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     */
    @Override
    public double nextDouble(){
        return (nextLong() >>> 11) * NORM_DOUBLE;
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code float} value between 0.0 and 1.0 from this random number generator's
     * sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     */
    @Override
    public float nextFloat(){
        return (float)((nextLong() >>> 40) * NORM_FLOAT);
    }

    /**
     * Returns a pseudo-random, uniformly distributed {@code boolean } value from this random number generator's sequence.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     */
    @Override
    public boolean nextBoolean(){
        return (nextLong() & 1) != 0;
    }

    /**
     * Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the
     * length of the byte array.
     * <p>
     * This implementation uses {@link #nextLong()} internally.
     */
    @Override
    public void nextBytes(final byte[] bytes){
        int n;
        int i = bytes.length;
        while(i != 0){
            n = i < 8 ? i : 8; // min(i, 8);
            for(long bits = nextLong(); n-- != 0; bits >>= 8)
                bytes[--i] = (byte)bits;
        }
    }

    /**
     * Sets the internal seed of this generator based on the given {@code long} value.
     * <p>
     * The given seed is passed twice through a hash function. This way, if the user passes a small value we avoid the short
     * irregular transient associated with states having a very small number of bits set.
     * @param seed a nonzero seed for this generator (if zero, the generator will be seeded with {@link Long#MIN_VALUE}).
     */
    @Override
    public void setSeed(final long seed){
        long seed0 = murmurHash3(seed == 0 ? Long.MIN_VALUE : seed);
        setState(seed0, murmurHash3(seed0));
    }

    public boolean chance(double chance){
        return nextDouble() < chance;
    }

    public float range(float amount){
        return nextFloat() * amount * 2 - amount;
    }

    public float random(float max){
        return nextFloat() * max;
    }

    /** Inclusive. */
    public int random(int max){
        return nextInt(max + 1);
    }

    public float random(float min, float max){
        return min + (max - min) * nextFloat();
    }

    public int range(int amount){
        return nextInt(amount * 2 + 1) - amount;
    }

    public int random(int min, int max){
        if(min >= max) return min;
        return min + nextInt(max - min + 1);
    }

    /**
     * Sets the internal state of this generator.
     * @param seed0 the first part of the internal state
     * @param seed1 the second part of the internal state
     */
    public void setState(final long seed0, final long seed1){
        this.seed0 = seed0;
        this.seed1 = seed1;
    }

    /**
     * Returns the internal seeds to allow state saving.
     * @param seed must be 0 or 1, designating which of the 2 long seeds to return
     * @return the internal seed that can be used in setState
     */
    public long getState(int seed){
        return seed == 0 ? seed0 : seed1;
    }

}
