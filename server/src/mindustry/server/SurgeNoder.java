package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SurgeNoder implements ApplicationListener{
    private Interval timer = new Interval();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;

        if(!timer.get(0, 60)) return;

        for(Team team : Team.base()){
            indexer.getAllied(team, BlockFlag.surgeable).each(tile -> {

                if(tile == null) return;
                if(Units.closest(team, tile.drawx(), tile.drawy(), 250f, unit -> unit instanceof Player) != null) return;
                if(tile.block() instanceof PowerNode){
                    if(tile.entity.power.links.size == 2){
                        tile.setNet(Blocks.surgeTower, team, 0);
//                        Call.onConstructFinish(tile, Blocks.surgeTower, -1, (byte)0, team, false);
                        tile.block().placed(tile);
                    }
                }
            });
        }
    }
}
