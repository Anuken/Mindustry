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
    /** Block that stored power for resupply. */
    battery,
    /** Any reactor block. */
    reactor,
    /** Blocks that extinguishes fires. */
    extinguisher,

    //special, internal identifiers
    launchPad,
    unitCargoUnloadPoint,
    unitAssembler,
    hasFogRadius;

    public final static BlockFlag[] all = values();

    /** Values for logic only. Filters out some internal flags. */
    public final static BlockFlag[] allLogic = {core, storage, generator, turret, factory, repair, battery, reactor};
}
