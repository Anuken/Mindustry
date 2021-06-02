package mindustry.world.meta;

/** Stores special flags of blocks for easy querying. */
public enum BlockFlag{
    /** Enemy core; primary target for all units. */
    core,
    /** Vault/container/etc */
    storage,
    /** Something that generates power. */
    generator,
    /** Any turret. */
    turret,
    /** A block that transforms resources. */
    factory,
    /** Repair point. */
    repair,
    /** Rally point. */
    rally,
    /** Block that stored power for resupply. */
    battery,
    /** Block used for resupply. */
    resupply,
    /** Any reactor block. */
    reactor,
    /** This flag is unused, and will be removed. */
    @Deprecated
    unitModifier,
    /** Blocks that extinguishes fires. */
    extinguisher,
    /** Just a launch pad. */
    launchPad;

    public final static BlockFlag[] all = values();

    /** Values for logic only. Filters out some internal flags. */
    public final static BlockFlag[] allLogic = {core, storage, generator, turret, factory, repair, rally, battery, resupply, reactor};
}
