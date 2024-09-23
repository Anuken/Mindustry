package mindustry.world.blocks.defense.turrets;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ReloadTurret extends BaseTurret{
    public float reload = 10f;

    public ReloadTurret(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        if(coolant != null){
            stats.remove(Stat.booster);

            //TODO this is very hacky, there is no current way to check if a ConsumeLiquidBase accepts something individually. fix later
            ObjectSet<Liquid> notBooster = content.liquids().select(l -> {
                for(Consume c : consumers){
                    if(!c.booster && c != coolant &&
                        ((c instanceof ConsumeLiquid cl && cl.liquid == l) ||
                        (c instanceof ConsumeLiquids cl2 && Structs.contains(cl2.liquids, s -> s.liquid == l)) ||
                        (c instanceof ConsumeLiquidFilter clf && clf.filter.get(l)))){

                        return true;
                    }
                }
                return false;
            }).asSet();

            stats.add(Stat.booster, StatValues.boosters(reload, coolant.amount, coolantMultiplier, true, l -> l.coolant && consumesLiquid(l) && !notBooster.contains(l)));
        }
    }

    public class ReloadTurretBuild extends BaseTurretBuild{
        public float reloadCounter;

        protected void updateCooling(){
            if(reloadCounter < reload && coolant != null && coolant.efficiency(this) > 0 && efficiency > 0){
                float capacity = coolant instanceof ConsumeLiquidFilter filter ? filter.getConsumed(this).heatCapacity : 1f;
                float amount = coolant.amount * coolant.efficiency(this);
                coolant.update(this);
                reloadCounter += amount * edelta() * capacity * coolantMultiplier;

                if(Mathf.chance(0.06 * amount)){
                    coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                }
            }
        }

        protected float baseReloadSpeed(){
            return efficiency;
        }
    }
}
