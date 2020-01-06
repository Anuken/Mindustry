package mindustry.world.blocks.units;

import mindustry.type.*;
import mindustry.world.*;

public class MinerFactory extends UnitFactory{
    public MinerFactory(String name){
        super(name);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        if(tile != source) return false;
        if(!unitType.toMine.contains(item)) return false;

        return tile.entity.items.get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        if(!unitType.toMine.contains(item)) return 0;
        return 50;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        tryDump(tile);
    }
}
