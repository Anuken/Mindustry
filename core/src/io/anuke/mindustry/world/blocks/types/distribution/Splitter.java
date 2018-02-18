package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public class Splitter extends Block{

    public Splitter(String name){
        super(name);
        solid = true;
        instantTransfer = true;
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, false);

        return to != null && to.block().acceptItem(item, to, tile);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        Tile to = getTileTarget(item, tile, source, true);

        to.block().handleItem(item, to, tile);
    }

    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        int dir = source.relativeTo(dest.x, dest.y);
        if(dir == -1) return null;
        Tile to;

        Tile a = dest.getNearby(Mathf.mod(dir - 1, 4));
        Tile b = dest.getNearby(Mathf.mod(dir + 1, 4));
        boolean ac = !(a.block().instantTransfer && source.block().instantTransfer) &&
                a.block().acceptItem(item, a, dest);
        boolean bc = !(b.block().instantTransfer && source.block().instantTransfer) &&
                b.block().acceptItem(item, b, dest);

        if(ac && !bc){
            to = a;
        }else if(bc && !ac){
            to = b;
        }else{
            if(dest.getDump() == 0){
                to = a;
                if(flip)
                    dest.setDump((byte)1);
            }else{
                to = b;
                if(flip)
                    dest.setDump((byte)0);
            }
        }

        return to;
    }
}
