package mindustry.world.blocks.distribution;

import arc.struct.Array;
import arc.util.Time;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.Item;
import mindustry.world.*;
import mindustry.world.meta.BlockGroup;

public class Router extends Block{
    public float speed = 8f;

    public Router(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
        unloadable = false;
    }

    @Override
    public void updateTile(){
        if(lastItem == null && items.total() > 0){
            items.clear();
        }

        if(lastItem != null){
            time += 1f / speed * Time.delta();
            Tile target = getTileTarget(tile, lastItem, lastInput, false);

            if(target != null && (time >= 1f || !(target.block() instanceof Router))){
                getTileTarget(tile, lastItem, lastInput, true);
                target.block().handleItem(target, Edges.getFacingEdge(tile, target), lastItem);
                items.remove(lastItem, 1);
                lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Tile source, Item item){
        return team == source.team() && lastItem == null && items.total() == 0;
    }

    @Override
    public void handleItem(Tile source, Item item){
        items.add(item, 1);
        lastItem = item;
        time = 0f;
        lastInput = source;
    }

    Tile getTileTarget(Item item, Tile from, boolean set){
        Array<Tile> proximity = tile.proximity();
        int counter = tile.rotation();
        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + counter) % proximity.size);
            if(set) tile.rotation((byte)((tile.rotation() + 1) % proximity.size));
            if(other == from && from.block() == Blocks.overflowGate) continue;
            if(other.block().acceptItem(other, Edges.getFacingEdge(tile, other), item)){
                return other;
            }
        }
        return null;
    }

    @Override
    public int removeStack(Item item, int amount){
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == lastItem){
            lastItem = null;
        }
        return result;
    }

    public class RouterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;
    }
}
