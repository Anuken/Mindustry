package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.*;

public class ReloadTurret extends BaseTurret{
    public float reloadTime = 10f;

    public ReloadTurret(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        if(acceptCoolant){
            stats.add(Stat.booster, new BoosterListValue(reloadTime, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, true, l -> consumes.liquidfilters.get(l.id)));
        }
    }

    public class ReloadTurretBuild extends BaseTurretBuild{
        public float reload;

        protected void updateCooling(){
            float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

            Liquid liquid = liquids.current();

            float used = Math.min(Math.min(liquids.get(liquid), maxUsed * Time.delta), Math.max(0, ((reloadTime - reload) / coolantMultiplier) / liquid.heatCapacity)) * baseReloadSpeed();
            reload += used * liquid.heatCapacity * coolantMultiplier;
            liquids.remove(liquid, used);

            if(Mathf.chance(0.06 * used)){
                coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
            }
        }

        protected float baseReloadSpeed(){
            return efficiency();
        }
    }
}
