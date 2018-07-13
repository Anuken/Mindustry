package io.anuke.mindustry.world.meta;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.BarType;

public class BlockBars{
    private Array<BlockBar> list = Array.with(new BlockBar(BarType.health, false, tile -> tile.entity.health / (float) tile.block().health));

    public void add(BlockBar bar){
        list.add(bar);
    }

    public void replace(BlockBar bar){
        remove(bar.type);
        list.add(bar);
    }

    public void remove(BarType type){
        for(BlockBar bar : list){
            if(bar.type == type){
                list.removeValue(bar, true);
                break;
            }
        }
    }

    public void removeAll(BarType type){
        Array<BlockBar> removals = new Array<>(4);

        for(BlockBar bar : list){
            if(bar.type == type){
                removals.add(bar);
            }
        }

        list.removeAll(removals, true);
    }

    public Array<BlockBar> list(){
        return list;
    }
}
