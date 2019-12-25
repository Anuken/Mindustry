package mindustry.world;

/** Methods for a packed position 'struct', contained in an int. */
public class Pos{
    public static final int invalid = get(-1, -1);

    /** Returns packed position from an x/y position. The values must be within short limits. */
    public static int get(int x, int y){
        return (((short)x) << 16) | (((short)y) & 0xFFFF);
    }

    /** Returns the x component of a position. */
    public static short x(int pos){
        return (short)(pos >>> 16);
    }

    /** Returns the y component of a position. */
    public static short y(int pos){
        return (short)(pos & 0xFFFF);
    }
}
