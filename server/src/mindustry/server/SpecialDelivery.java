package mindustry.server;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.game.*;
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
                Array.with(Geometry.findClosest(ce.x, ce.y, upgradable(td.team))).each(t -> {
                    if(t == null) return;
                    charge(td.team, t.block.upgrade.get(t));

                    float dst = ce.dst(t);
                    float maxTraveled = Bullets.driverBolt.lifetime * Bullets.driverBolt.speed;

                    float velocity = 1f;
                    velocity += Bullets.driverBolt.drag * dst / 8.25f;

                    Call.createBullet(Bullets.driverBolt, td.team, ce.x, ce.y, ce.angleTo(t), velocity, (dst / maxTraveled));
                    upgrading.put(bulletGroup.entitiesToAdd.get(bulletGroup.entitiesToAdd.size -1), t);
                });
            });
        });
    }

    protected Array<Tile> upgradable(Team team){
        return indexer.getAllied(team, BlockFlag.upgradable).select(t -> {
            if(upgrading.containsValue(t, false)) return false;
            if(t == null || t.block.upgrade == null || t.block.upgrade.get(t) == null) return false;
            if(!afford(team, t.block.upgrade.get(t))) return false;

            if((t.block.upgrade.get(t) == Blocks.armoredConveyor || t.block.upgrade.get(t) == Blocks.platedConduit) && Units.closest(team, t.drawx(), t.drawy(), tilesize * 20, u -> u instanceof Player) != null) return false;

            return true;
        }).asArray();
    }

    protected boolean afford(Team team, Block block){
        return team.core().items.has(block.requirements, state.rules.buildCostMultiplier * 11);
    }

    protected void charge(Team team, Block block){
        for(ItemStack is: block.requirements){
            team.core().items.remove(is.item, (int)(is.amount * state.rules.buildCostMultiplier));
        }
    }
}
