package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.Effects.*;
import mindustry.game.EventType.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.tilesize;

public class CooledTurret extends Turret{
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    public Effect coolEffect = Fx.fuelburn;

    public CooledTurret(String name){
        super(name);
        hasLiquids = true;
        liquidCapacity = 20f;

        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.2f)).update(false).boost();
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.booster, new BoosterListValue(reload, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, true, l -> consumes.liquidfilters.get(l.id)));
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        if(tile.entity.liquids.currentAmount() <= 0.001f){
            Events.fire(Trigger.turretCool);
        }

        super.handleLiquid(tile, source, liquid, amount);
    }

    @Override
    protected void updateShooting(Tile tile){
        super.updateShooting(tile);

        float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

        TurretEntity entity = tile.ent();
        Liquid liquid = entity.liquids.current();

        float used = Math.min(Math.min(entity.liquids.get(liquid), maxUsed * Time.delta()), Math.max(0, ((reload - entity.reload) / coolantMultiplier) / liquid.heatCapacity)) * baseReloadSpeed(tile);
        entity.reload += used * liquid.heatCapacity * coolantMultiplier;
        entity.liquids.remove(liquid, used);

        if(Mathf.chance(0.06 * used)){
            Effects.effect(coolEffect, tile.drawx() + Mathf.range(size * tilesize / 2f), tile.drawy() + Mathf.range(size * tilesize / 2f));
        }
    }
}
