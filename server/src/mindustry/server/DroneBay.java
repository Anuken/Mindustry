package mindustry.server;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.type.*;
import mindustry.entities.type.base.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class DroneBay implements ApplicationListener{
    private ObjectMap<BrokenBlock, BuilderDrone> rebuild = new ObjectMap<>();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;

        for(Team team : Team.base()){
            if(team.cores().isEmpty()) continue;

            for(BrokenBlock broken : team.data().brokenBlocks){
                if(rebuild.get(broken) == null) rebuild.put(broken, spawn(team));
            }
        }
    }

    private BuilderDrone spawn(Team team){
        BaseUnit unit = UnitTypes.phantom.create(team);
        unit.set(team.core().x + Mathf.range(4), team.core().y + Mathf.range(4));
        unit.add();
        Events.fire(new UnitCreateEvent(unit));
        return (BuilderDrone)unit;
    }
}
