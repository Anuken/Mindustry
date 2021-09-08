package mindustry.world.meta;

/** Environmental flags for different types of locations. */
public class Env{
    public static final int
    terrestrial = 1,
    space = 1 << 1,
    underwater = 1 << 2,
    spores = 1 << 3,
    scorching = 1 << 4,
    groundOil = 1 << 5,
    groundWater = 1 << 6,
    any = 0xffffffff;
}
