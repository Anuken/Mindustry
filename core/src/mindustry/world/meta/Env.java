package mindustry.world.meta;

/** Environmental flags for different types of locations. */
public class Env{
    public static final int
    //is on a planet
    terrestrial = 1,
    //is in space, no atmosphere
    space = 1 << 1,
    //is underwater, on a planet
    underwater = 1 << 2,
    //has a spores
    spores = 1 << 3,
    //has a scorching env effect
    scorching = 1 << 4,
    //has oil reservoirs
    groundOil = 1 << 5,
    //has water reservoirs
    groundWater = 1 << 6,
    //has oxygen in the atmosphere
    oxygen = 1 << 7,
    //all attributes combined, only used for bitmasking purposes
    any = 0xffffffff,
    //no attributes (0)
    none = 0;
}