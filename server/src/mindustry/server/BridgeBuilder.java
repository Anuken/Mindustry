package mindustry.server;

import arc.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BridgeBuilder implements ApplicationListener{

    private Interval timer = new Interval();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(60)) return;

        state.teams.getActive().each(data -> {
            final boolean[] did = {false};
            indexer.getAllied(data.team, BlockFlag.collapsable).each(tile -> {
                if(did[0]) return;
                if(!(tile.block instanceof ItemBridge)) return;
                if(tile.entity.proximity().size != 0) return;
                if(linked(tile)) return;

                if(Units.closest(data.team, tile.drawx(), tile.drawy(), tilesize * 20, u -> u instanceof Player) != null) return;

                tile.deconstructNet();
                did[0] = true;
            });
        });
    }

    private boolean linked(Tile tile){
        ItemBridgeEntity entity = tile.ent();
        ItemBridge bridge = (ItemBridge)tile.block;

        Tile other = world.tile(entity.link);
        return bridge.linkValid(tile, other);
    }
}
