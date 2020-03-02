package mindustry.server;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.distribution.ItemBridge.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BridgeBuilder implements ApplicationListener{

    private Interval timer = new Interval();
    private Array<Tile> line = new Array<>();

    private Block into;

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(60)) return;

        state.teams.getActive().each(data -> {
            indexer.getAllied(data.team, BlockFlag.collapsable).each(tile -> {
                if(!(tile.block instanceof ItemBridge)) return;
                ItemBridgeEntity entity = tile.ent();

                if(tile.entity.proximity().select(t -> !(t.block instanceof ItemBridge)).size == 0) return;

                Tile other = world.tile(entity.link);
                if(!linked(tile)) return;

                line = Placement.normalizeLine(tile.x, tile.y, other.x, other.y).map(point2 -> world.tile(point2.x, point2.y));

                int air = line.select(t -> t != tile && t != other).count(t -> t.block != Blocks.air);
                if(air > 0) return;

                if(Units.closest(data.team, tile.drawx(), tile.drawy(), tilesize * 15, u -> u instanceof Player) != null) return;

                int angle = (int)(tile.angleTo(other) / 90);
                into = ground(tile.block);

                if(linked(other)) line.remove(other);
                line.each(t -> t.setNet(into, data.team, angle));
            });
        });
    }

    private Block ground(Block bridge){
        if(bridge == Blocks.itemBridge)    return Blocks.conveyor;
        if(bridge == Blocks.bridgeConduit) return Blocks.conduit;

        return Blocks.air;
    }

    private boolean linked(Tile tile){
        ItemBridgeEntity entity = tile.ent();
        ItemBridge bridge = (ItemBridge)tile.block;

        Tile other = world.tile(entity.link);
        return bridge.linkValid(tile, other);
    }
}
