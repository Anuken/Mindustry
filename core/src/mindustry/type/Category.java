package mindustry.type;

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
    effect,
    /** Additional Category 1. When mod uses this category, it is activated. Just a magic machine. XD */
    magichine,
    /** Additional Category 2. When mod uses this category, it is activated. It is not Bionic magical, it is bionic mechanical */
    bionical,
    /** Additional Category 3. When mod uses this category, it is activated. Succeeding You, Lich. */
    immortallizion,
    /** Additional Category 4. When mod uses this category, it is activated. Explosion is nature! */
    naturexplosion
    ;

    public static final Category[] all = values();

    public Category prev(){
        return all[(this.ordinal() - 1 + all.length) % all.length];
    }

    public Category next(){
        return all[(this.ordinal() + 1) % all.length];
    }
}
