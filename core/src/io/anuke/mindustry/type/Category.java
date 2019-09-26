package io.anuke.mindustry.type;

public enum Category{
    /** Offensive turrets. */
    turret,
    /** Blocks that produce raw resources, such as drills. */
    production,
    /** Blocks that move items around. */
    distribution,
    /** Blocks that move liquids around. */
    liquid,
    /** Blocks that generate or transport power. */
    power,
    /** Walls and other defensive structures. */
    defense,
    /** Blocks that craft things. */
    crafting,
    /** Blocks that create units. */
    units,
    /** Things that upgrade the player such as mech pads. */
    upgrade,
    /** Things for storage or passive effects. */
    effect;

    public static final Category[] all = values();
}
