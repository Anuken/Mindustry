package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ReloadTurret extends BaseTurret{
    public float reloadTime = 10f;

    public ReloadTurret(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        if(acceptCoolant && coolantConsumer != null){
            stats.add(Stat.booster, StatValues.boosters(reloadTime, coolantConsumer.amount, coolantMultiplier, true, l -> l.coolant && consumesLiquid(l)));
        }
    }

    public class ReloadTurretBuild extends BaseTurretBuild{
        public float reload;

        @Override
        public void created(){
            super.created();
            //for visual reasons, the turret does not need reloading when placed
            reload = reloadTime;
        }

        protected void updateCooling(){
            if(reload < reloadTime && acceptCoolant){
                float maxUsed = coolantConsumer.amount;
                Liquid liquid = liquids.current();

                float used = Math.min(liquids.get(liquid), maxUsed * Time.delta) * baseReloadSpeed();
                reload += used * liquid.heatCapacity * coolantMultiplier;
                liquids.remove(liquid, used);

                if(Mathf.chance(0.06 * used)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        protected float baseReloadSpeed(){
            return efficiency();
        }
    }
}
