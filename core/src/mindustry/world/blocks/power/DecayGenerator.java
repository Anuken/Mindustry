package mindustry.world.blocks.power;

import mindustry.type.Item;
import mindustry.world.*;

import static mindustry.Vars.netServer;

public class DecayGenerator extends ItemLiquidGenerator{

    public DecayGenerator(String name){
        super(true, false, name);
        hasItems = true;
        hasLiquids = false;
        sync = true;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.radioactivity;
    }

//    @Override
//    public void iceberg(Tile tile){
//        tile.entity.proximity().each(t -> {
//            if (t.block != tile.block) return;
//            if (t.entity.items.total() >= tile.entity.items.total()) return;
//            t.entity.items.add(tile.entity.items.first(), 1);
//            tile.entity.items.remove(tile.entity.items.first(), 1);
//        });
//    }

//    @Override
//    public void handleItem(Item item, Tile tile, Tile source){
//        super.handleItem(item, tile, source);
//
////        if(acceptItem(item, tile, source)) return;
//        tile.entity.proximity().each(t -> {
//            if (t.block != tile.block) return;
//            if (t.entity.items.total() >= tile.entity.items.total()) return;
//            t.entity.items.add(tile.entity.items.first(), 1);
//            tile.entity.items.remove(tile.entity.items.first(), 1);
//            netServer.titanic.addAll(t, tile);
//        });
//    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);

        tile.entity.proximity().each(other -> {
            if(other.block != tile.block) return;
            if(other.entity.items.get(item) >= tile.entity.items.get(item)) return;
            other.block.handleItem(item, other, tile);
            tile.entity.items.remove(item, 1);
            netServer.titanic.addAll(other, tile);
        });
    }
}
