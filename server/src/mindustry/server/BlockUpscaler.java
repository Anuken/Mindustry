package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BlockUpscaler implements ApplicationListener{
    private Interval timer = new Interval();
    private Array<Tile> tmp = new Array<>();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;

        if(!timer.get(0, 60 * 10)) return;

        for(Team team : Team.base()){
            trigger(team);
        }
    }

    @Override
    public void init(){
        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking) trigger(event.team);
        });
    }

    private void trigger(Team team){
        indexer.getAllied(team, BlockFlag.scalable).each(tile -> {

            if(tile == null) return;
            if(!tile.block().flags.contains(BlockFlag.scalable)) return;

            tile.getLinkedTilesAs(tile.block().upscale.get(), tmp);
            if(tmp.size != tile.block().upscale.get().size * tile.block().upscale.get().size) return;
            if(tmp.select(t -> t.block() != tile.block()).size > 0) return;

            float healthf = tmp.sumf(t -> t.entity.healthf()) / tmp.size;

            Call.onConstructFinish(tile, tile.block().upscale.get(), -1, tile.rotation(), tile.getTeam(), true);
            tile.block.placed(tile);

            tile.entity.damage(tile.entity.maxHealth() - (tile.entity.maxHealth() * healthf));
        });
    }
}
