package io.anuke.mindustry.world.meta;

import io.anuke.arc.Core;

import java.util.Locale;

/** Describes one type of stat for a block. */
public enum BlockStat{
    health(StatCategory.general),
    size(StatCategory.general),
    buildTime(StatCategory.general),
    buildCost(StatCategory.general),

    itemCapacity(StatCategory.items),
    itemsMoved(StatCategory.items),
    launchTime(StatCategory.items),

    liquidCapacity(StatCategory.liquids),

    powerCapacity(StatCategory.power),
    powerUse(StatCategory.power),
    powerDamage(StatCategory.power),
    powerRange(StatCategory.power),
    basePowerGeneration(StatCategory.power),

    input(StatCategory.crafting),
    output(StatCategory.crafting),
    productionTime(StatCategory.crafting),
    drillTier(StatCategory.crafting),
    drillSpeed(StatCategory.crafting),
    maxUnits(StatCategory.crafting),

    speedIncrease(StatCategory.shooting),
    repairTime(StatCategory.shooting),
    range(StatCategory.shooting),
    shootRange(StatCategory.shooting),
    inaccuracy(StatCategory.shooting),
    shots(StatCategory.shooting),
    reload(StatCategory.shooting),
    powerShot(StatCategory.shooting),
    targetsAir(StatCategory.shooting),
    targetsGround(StatCategory.shooting),
    damage(StatCategory.shooting),
    ammo(StatCategory.shooting),

    booster(StatCategory.optional),
    boostEffect(StatCategory.optional);

    public final StatCategory category;

    BlockStat(StatCategory category){
        this.category = category;
    }

    public String localized(){
        return Core.bundle.get("blocks." + name().toLowerCase(Locale.ROOT));
    }
}
