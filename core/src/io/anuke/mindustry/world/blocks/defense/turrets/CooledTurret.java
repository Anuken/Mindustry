package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;

public class CooledTurret extends Turret{
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    protected float coolantMultiplier = 5f;
    protected Effect coolEffect = Fx.fuelburn;

    public CooledTurret(String name){
        super(name);
        hasLiquids = true;
        liquidCapacity = 20f;

        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.2f)).update(false).boost();
    }

    @Override
    public void setStats(){
        super.setStats();

        float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

        stats.add(BlockStat.boostEffect, 1f + maxUsed * coolantMultiplier, StatUnit.timesSpeed);
    }

    @Override
    protected void updateShooting(Tile tile){
        super.updateShooting(tile);

        float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

        TurretEntity entity = tile.entity();
        Liquid liquid = entity.liquids.current();

        float used = Math.min(Math.min(entity.liquids.get(liquid), maxUsed * Time.delta()), Math.max(0, ((reload - entity.reload) / coolantMultiplier) / liquid.heatCapacity)) * baseReloadSpeed(tile);
        entity.reload += (used * liquid.heatCapacity) / liquid.heatCapacity;
        entity.liquids.remove(liquid, used);

        if(Mathf.chance(0.06 * used)){
            Effects.effect(coolEffect, tile.drawx() + Mathf.range(size * tilesize / 2f), tile.drawy() + Mathf.range(size * tilesize / 2f));
        }
    }
}
