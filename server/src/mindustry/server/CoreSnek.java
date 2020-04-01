package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class CoreSnek implements ApplicationListener{
    private Interval timer = new Interval();

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
            Core.app.post(() -> trigger(event.team));
        });
    }

    private void trigger(Team team){
        indexer.getAllied(team, BlockFlag.core).each(tile -> {
            if(tile == null) return;

            tile.entity.proximity().each(e -> e.block == Blocks.vault, vault -> {
                if(vault.entity.proximity().select(e -> e.block instanceof CoreBlock).size > 0){
                    if(vault.entity.proximity().count(t -> t.block == Blocks.vault) > 0){
                        vault.constructNet(Blocks.coreShard, tile.getTeam(), (byte)0);
                    }
                }
            });

            if(1 >= team.cores().size) return;
            if(tile.block != Blocks.coreShard) return;

            if(tile.entity.proximity().select(e -> e.block == Blocks.vault).size == 0){
                if(tile.entity.proximity().select(e -> e.block instanceof CoreBlock).size < 2){
                    tile.constructNet(Blocks.vault, tile.getTeam(), (byte)0);
                }
            }

            if(tile.entity.proximity().select(e -> e.block instanceof CoreBlock).size > 2){
                if(tile.entity.proximity().select(e -> e.block == Blocks.vault).size == 0){
                    tile.constructNet(Blocks.vault, tile.getTeam(), (byte)0);
                }
            }
        });
    }
}
