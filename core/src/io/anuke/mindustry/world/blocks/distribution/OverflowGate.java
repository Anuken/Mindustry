package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public class OverflowGate extends Splitter {

    public OverflowGate(String name) {
        super(name);
        hasItems = true;
    }

    @Override
    Tile getTileTarget(Item item, Tile dest, Tile source, boolean flip){
        int dir = source.relativeTo(dest.x, dest.y);
        if(dir == -1) return null;
        Tile to = dest.getNearby(dir);

        if(!(to.block().acceptItem(item, to, dest) &&
                !(to.block().instantTransfer && source.block().instantTransfer))){
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
        }

        return to;
    }
}
