package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.entities.type.base.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitFactory.*;

import static mindustry.Vars.*;

public class Katamari implements ApplicationListener{
    private Interval timer = new Interval();

    private Array<Unit> nearby = new Array<>();
    private Array<UnitType> tiers = new Array<UnitType>(){{
        addAll(content.units());
        sort(u -> -u.health);
    }};

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(160f)) return;

        for(BaseUnit unit : unitGroup){
            nearby.clear();
            Units.nearby(unit.getTeam(), unit.x, unit.y, tilesize * 5, nearby::add);

            nearby = nearby.select(u -> {
//                if(unit == u) return false;
                if(unit instanceof BaseDrone) return false;
                if(unit.isFlying() != u.isFlying()) return false;
                if(u instanceof Player) return false;
                if(((BaseUnit)u).getType().health > unit.getType().health) return false;
                if(unit.getSpawner() == null) return false;

                return true;
            });

            final float[] health = {0};
            nearby.each(u -> health[0] += u.health);

            UnitType affort = afford(health[0], unit.isFlying());

            if(affort != null && affort.health > unit.getType().health){
                BaseUnit spawn = affort.create(unit.getTeam());
                spawn.setSpawner(unit.getSpawner());
                spawn.set(unit.x, unit.y);
                spawn.add();

                ((UnitFactoryEntity)unit.getSpawner().ent()).spawned++;
                netServer.titanic.add(unit.getSpawner());

                nearby.each(HealthTrait::kill);
            }
        }
    }

    public UnitType afford(float health, boolean flies){
        for(UnitType tier : tiers){
            if(health >= tier.health && flies == tier.flying) return tier;
        }

        return null;
    }
}
