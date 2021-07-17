package mindustry.world.meta;

import arc.*;

import java.util.*;

/** Describes one type of stat for content. */
public enum Stat{
    health,
    armor,
    size,
    displaySize,
    buildTime,
    buildCost,
    memoryCapacity,
    explosiveness,
    flammability,
    radioactivity,
    charge,
    heatCapacity,
    viscosity,
    temperature,
    flying,
    speed,
    buildSpeed,
    mineSpeed,
    mineTier,
    payloadCapacity,
    commandLimit,
    baseDeflectChance,
    lightningChance, 
    lightningDamage,
    abilities,
    canBoost,
    maxUnits,

    damageMultiplier,
    healthMultiplier,
    speedMultiplier,
    reloadMultiplier,
    buildSpeedMultiplier,
    reactive,
    healing,

    itemCapacity(StatCat.items),
    itemsMoved(StatCat.items),
    launchTime(StatCat.items),
    maxConsecutive(StatCat.items),

    liquidCapacity(StatCat.liquids),

    powerCapacity(StatCat.power),
    powerUse(StatCat.power),
    powerDamage(StatCat.power),
    powerRange(StatCat.power),
    powerConnections(StatCat.power),
    basePowerGeneration(StatCat.power),

    tiles(StatCat.crafting),
    input(StatCat.crafting),
    output(StatCat.crafting),
    productionTime(StatCat.crafting),
    drillTier(StatCat.crafting),
    drillSpeed(StatCat.crafting),
    linkRange(StatCat.crafting),
    instructions(StatCat.crafting),

    weapons(StatCat.function),
    bullet(StatCat.function),

    speedIncrease(StatCat.function),
    repairTime(StatCat.function),
    repairSpeed(StatCat.function),
    range(StatCat.function),
    shootRange(StatCat.function),
    inaccuracy(StatCat.function),
    shots(StatCat.function),
    reload(StatCat.function),
    powerShot(StatCat.function),
    targetsAir(StatCat.function),
    targetsGround(StatCat.function),
    damage(StatCat.function),
    ammo(StatCat.function),
    ammoUse(StatCat.function),
    shieldHealth(StatCat.function),
    cooldownTime(StatCat.function),

    booster(StatCat.optional),
    boostEffect(StatCat.optional),
    affinities(StatCat.optional),
    opposites(StatCat.optional);

    public final StatCat category;

    Stat(StatCat category){
        this.category = category;
    }

    Stat(){
        this.category = StatCat.general;
    }

    public String localized(){
        return Core.bundle.get("stat." + name().toLowerCase(Locale.ROOT));
    }
}
