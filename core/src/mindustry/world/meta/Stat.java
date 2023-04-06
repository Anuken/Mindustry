package mindustry.world.meta;

import arc.*;
import arc.struct.*;

import java.util.*;

/** Describes one type of stat for content. */
public class Stat implements Comparable<Stat>{
    public static final Seq<Stat> all = new Seq<>();

    public static final Stat

    health = new Stat("health"),
    armor = new Stat("armor"),
    size = new Stat("size"),
    displaySize = new Stat("displaySize"),
    buildTime = new Stat("buildTime"),
    buildCost = new Stat("buildCost"),
    memoryCapacity = new Stat("memoryCapacity"),
    explosiveness = new Stat("explosiveness"),
    flammability = new Stat("flammability"),
    radioactivity = new Stat("radioactivity"),
    charge = new Stat("charge"),
    heatCapacity = new Stat("heatCapacity"),
    viscosity = new Stat("viscosity"),
    temperature = new Stat("temperature"),
    flying = new Stat("flying"),
    speed = new Stat("speed"),
    buildSpeed = new Stat("buildSpeed"),
    mineSpeed = new Stat("mineSpeed"),
    mineTier = new Stat("mineTier"),
    payloadCapacity = new Stat("payloadCapacity"),
    baseDeflectChance = new Stat("baseDeflectChance"),
    lightningChance = new Stat("lightningChance"),
    lightningDamage = new Stat("lightningDamage"),
    abilities = new Stat("abilities"),
    canBoost = new Stat("canBoost"),
    maxUnits = new Stat("maxUnits"),

    damageMultiplier = new Stat("damageMultiplier"),
    healthMultiplier = new Stat("healthMultiplier"),
    speedMultiplier = new Stat("speedMultiplier"),
    reloadMultiplier = new Stat("reloadMultiplier"),
    buildSpeedMultiplier = new Stat("buildSpeedMultiplier"),
    reactive = new Stat("reactive"),
    healing = new Stat("healing"),
    immunities = new Stat("immunities"),

    itemCapacity = new Stat("itemCapacity", StatCat.items),
    itemsMoved = new Stat("itemsMoved", StatCat.items),
    launchTime = new Stat("launchTime", StatCat.items),
    maxConsecutive = new Stat("maxConsecutive", StatCat.items),

    liquidCapacity = new Stat("liquidCapacity", StatCat.liquids),

    powerCapacity = new Stat("powerCapacity", StatCat.power),
    powerUse = new Stat("powerUse", StatCat.power),
    powerDamage = new Stat("powerDamage", StatCat.power),
    powerRange = new Stat("powerRange", StatCat.power),
    powerConnections = new Stat("powerConnections", StatCat.power),
    basePowerGeneration = new Stat("basePowerGeneration", StatCat.power),

    tiles = new Stat("tiles", StatCat.crafting),
    input = new Stat("input", StatCat.crafting),
    output = new Stat("output", StatCat.crafting),
    productionTime = new Stat("productionTime", StatCat.crafting),
    maxEfficiency = new Stat("maxEfficiency", StatCat.crafting),
    drillTier = new Stat("drillTier", StatCat.crafting),
    drillSpeed = new Stat("drillSpeed", StatCat.crafting),
    linkRange = new Stat("linkRange", StatCat.crafting),
    instructions = new Stat("instructions", StatCat.crafting),

    weapons = new Stat("weapons", StatCat.function),
    bullet = new Stat("bullet", StatCat.function),

    speedIncrease = new Stat("speedIncrease", StatCat.function),
    repairTime = new Stat("repairTime", StatCat.function),
    repairSpeed = new Stat("repairSpeed", StatCat.function),
    range = new Stat("range", StatCat.function),
    shootRange = new Stat("shootRange", StatCat.function),
    inaccuracy = new Stat("inaccuracy", StatCat.function),
    shots = new Stat("shots", StatCat.function),
    reload = new Stat("reload", StatCat.function),
    targetsAir = new Stat("targetsAir", StatCat.function),
    targetsGround = new Stat("targetsGround", StatCat.function),
    damage = new Stat("damage", StatCat.function),
    ammo = new Stat("ammo", StatCat.function),
    ammoUse = new Stat("ammoUse", StatCat.function),
    shieldHealth = new Stat("shieldHealth", StatCat.function),
    cooldownTime = new Stat("cooldownTime", StatCat.function),
    moduleTier = new Stat("moduletier", StatCat.function),
    unitType = new Stat("unittype", StatCat.function),

    booster = new Stat("booster", StatCat.optional),
    boostEffect = new Stat("boostEffect", StatCat.optional),
    affinities = new Stat("affinities", StatCat.optional),
    opposites = new Stat("opposites", StatCat.optional);

    public final StatCat category;
    public final String name;
    public final int id;

    public Stat(String name, StatCat category){
        this.category = category;
        this.name = name;
        id = all.size;
        all.add(this);
    }

    public Stat(String name){
        this(name, StatCat.general);
    }

    public String localized(){
        return Core.bundle.get("stat." + name.toLowerCase(Locale.ROOT));
    }

    @Override
    public int compareTo(Stat o){
        return id - o.id;
    }
}
