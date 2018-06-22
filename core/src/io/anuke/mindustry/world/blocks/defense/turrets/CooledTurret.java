package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.effect.Fire;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class CooledTurret extends Turret {
    /**How much reload is lowered by for each unit of liquid of heat capacity 1.*/
    protected float coolantMultiplier = 1f;
    protected float maxUsed = 1f;
    protected Effect coolEffect = BlockFx.fuelburn;

    public CooledTurret(String name) {
        super(name);
        hasLiquids = true;
        liquidCapacity = 20f;
    }

    @Override
    protected void updateShooting(Tile tile) {
        super.updateShooting(tile);

        TurretEntity entity = tile.entity();

        float used = Math.min(Math.min(entity.liquids.amount, maxUsed * Timers.delta()), Math.max(0, ((reload - entity.reload) / coolantMultiplier) / entity.liquids.liquid.heatCapacity));
        entity.reload += (used * entity.liquids.liquid.heatCapacity) / entity.liquids.liquid.heatCapacity;
        entity.liquids.amount -= used;

        if(Mathf.chance(0.04 * used)){
            Effects.effect(coolEffect, tile.drawx() + Mathf.range(size * tilesize/2f), tile.drawy() + Mathf.range(size * tilesize/2f));
        }

        //don't use oil as coolant, thanks
        if(Mathf.chance(entity.liquids.liquid.flammability / 10f * used)){
            Fire.create(tile);
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && liquid.temperature <= 0.5f;
    }
}
