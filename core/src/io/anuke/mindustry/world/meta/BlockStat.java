package io.anuke.mindustry.world.meta;

import io.anuke.ucore.util.Bundles;

import java.util.Locale;

/**Describes one type of stat for a block.*/
public enum BlockStat{
    health(StatCategory.general),
    size(StatCategory.general),

    itemCapacity(StatCategory.items),
    inputItemCapacity(StatCategory.items),
    outputItemCapacity(StatCategory.items),
    itemSpeed(StatCategory.items),

    liquidCapacity(StatCategory.liquids),
    liquidOutput(StatCategory.liquids),
    liquidOutputSpeed(StatCategory.liquids),
    coolant(StatCategory.liquids),
    coolantUse(StatCategory.liquids),

    powerCapacity(StatCategory.power),
    powerUse(StatCategory.power),
    powerDamage(StatCategory.power),
    powerRange(StatCategory.power),
    powerTransferSpeed(StatCategory.power),
    basePowerGeneration(StatCategory.power),
    inputLiquidFuel(StatCategory.power),
    liquidFuelUse(StatCategory.power),

    inputLiquid(StatCategory.crafting),
    liquidUse(StatCategory.crafting),
    inputItem(StatCategory.crafting),
    inputItems(StatCategory.crafting),
    inputFuel(StatCategory.crafting),
    fuelBurnTime(StatCategory.crafting),
    craftSpeed(StatCategory.crafting),
    outputItem(StatCategory.crafting),
    drillTier(StatCategory.crafting),
    drillSpeed(StatCategory.crafting),

    shootRange(StatCategory.shooting),
    inaccuracy(StatCategory.shooting),
    shots(StatCategory.shooting),
    reload(StatCategory.shooting),
    powerShot(StatCategory.shooting),
    targetsAir(StatCategory.shooting),

    boostItem(StatCategory.optional),
    boostLiquid(StatCategory.optional),;

    public final StatCategory category;

    BlockStat(StatCategory category){
        this.category = category;
    }

    public String localized(){
        return Bundles.get("text.blocks." + name().toLowerCase(Locale.ROOT));
    }
}
