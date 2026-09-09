package mindustry.type;

/** Note: Don't use this class; difficulty will be reworked in v9 to use specific discrete values instead of arbitrary numbers. */
public class SectorDifficulty{
    /** Note that these values represent the minimum integer value. Thus, low would be 0-2, medium would be 3-4, etc. */
    public static final int
    low = 0,
    medium = 3,
    high = 5,
    extreme = 8,
    eradication = 10,
    //special difficulty reserved for 27
    unreasonable = 13;
}
