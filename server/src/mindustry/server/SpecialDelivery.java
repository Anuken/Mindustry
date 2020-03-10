package mindustry.server;

import arc.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SpecialDelivery implements ApplicationListener{


    private Interval timer = new Interval();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(20)) return;

        state.teams.getActive().each(td -> {
            td.cores.each(ce -> {
                Tile upgradable = Geometry.findClosest(ce.x, ce.y, indexer.getAllied(ce.getTeam(), BlockFlag.upgradable).select(t -> !upgrading.containsValue(t, false) ));
                if(upgradable == null || upgradable.block.upgrade == null || upgradable.block.upgrade.get() == null) return;

                if(!ce.items.has(upgradable.block.upgrade.get().requirements, state.rules.buildCostMultiplier)) return;
                for(ItemStack is: upgradable.block.upgrade.get().requirements){
                    ce.items.remove(is.item, (int)(is.amount * state.rules.buildCostMultiplier));
                }

                float dst = ce.dst(upgradable);
                float maxTraveled = Bullets.driverBolt.lifetime * Bullets.driverBolt.speed;

                float velocity = 1f;
                velocity += Bullets.driverBolt.drag * dst / 8.25f;

                Call.createBullet(Bullets.driverBolt, td.team, ce.x, ce.y, ce.angleTo(upgradable), velocity, (dst / maxTraveled));
                upgrading.put(bulletGroup.entitiesToAdd.get(bulletGroup.entitiesToAdd.size -1), upgradable);
            });
        });
    }
}
