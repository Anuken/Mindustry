package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import mindustry.world.consumers.*;
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

        if(coolant != null){
            stats.add(Stat.booster, StatValues.boosters(reloadTime, coolant.amount, coolantMultiplier, true, l -> l.coolant && consumesLiquid(l)));
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
            if(reload < reloadTime && coolant != null && coolant.efficiency(this) > 0){
                float capacity = coolant instanceof ConsumeLiquidFilter filter ? filter.getConsumed(this).heatCapacity : 1f;
                coolant.update(this);
                reload += coolant.amount * edelta() * capacity * coolantMultiplier;

                if(Mathf.chance(0.06 * coolant.amount)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        protected float baseReloadSpeed(){
            return efficiency;
        }
    }
}
