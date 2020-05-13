package mindustry.world.meta;

/** Stores special flags of blocks for easy querying. */
public enum BlockFlag{
    /** Enemy core; primary target for all units. */
    core,
    /** Producer of important goods. */
    producer,
    /** A turret. */
    turret,
    /** Repair point. */
    repair,
    /** Rally point. TODO remove*/
    rally,
    /** Any block that boosts unit capacity. */
    unitModifier;

    public final static BlockFlag[] all = values();
}
