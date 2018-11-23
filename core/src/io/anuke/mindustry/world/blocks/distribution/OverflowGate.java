package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class OverflowGate extends Router{

    public OverflowGate(String name){
        super(name);
        hasItems = true;
        speed = 1f;
    }

    @Override
    public void update(Tile tile){
        SplitterEntity entity = tile.entity();

        if(entity.lastItem == null && entity.items.total() > 0){
            entity.items.clear();
        }

        if(entity.lastItem != null){
            entity.time += 1f/speed * Timers.delta();
            Tile target = getTileTarget(tile, entity.lastItem, entity.lastInput, false);

            if(target != null && (entity.time >= 1f)){
                getTileTarget(tile, entity.lastItem, entity.lastInput, true);
                target.block().handleItem(entity.lastItem, target, Edges.getFacingEdge(tile, target));
                entity.items.remove(entity.lastItem, 1);
                entity.lastItem = null;
            }
        }
    }

    @Override
    Tile getTileTarget(Tile tile, Item item, Tile src, boolean flip){
        int from = tile.relativeTo(src.x, src.y);
        if(from == -1) return null;
        Tile to = tile.getNearby((from + 2) % 4);
        if(to == null) return null;
        Tile edge = Edges.getFacingEdge(tile, to);

        if(!to.block().acceptItem(item, to, edge) || (to.block() instanceof OverflowGate)){
            Tile a = tile.getNearby(Mathf.mod(from - 1, 4));
            Tile b = tile.getNearby(Mathf.mod(from + 1, 4));
            boolean ac = a != null && a.block().acceptItem(item, a, edge) && !(a.block() instanceof OverflowGate);
            boolean bc = b != null && b.block().acceptItem(item, b, edge) && !(b.block() instanceof OverflowGate);

            if(!ac && !bc){
                return null;
            }

            if(ac && !bc){
                to = a;
            }else if(bc && !ac){
                to = b;
            }else{
                if(tile.getDump() == 0){
                    to = a;
                    if(flip) tile.setDump((byte) 1);
                }else{
                    to = b;
                    if(flip) tile.setDump((byte) 0);
                }
            }
        }

        return to;
    }
}
