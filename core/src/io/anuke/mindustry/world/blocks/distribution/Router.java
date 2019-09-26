package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.collection.Array;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.BlockGroup;

public class Router extends Block{
    protected float speed = 8f;

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
    public void update(Tile tile){
        RouterEntity entity = tile.entity();

        if(entity.lastItem == null && entity.items.total() > 0){
            entity.items.clear();
        }

        if(entity.lastItem != null){
            entity.time += 1f / speed * Time.delta();
            Tile target = getTileTarget(tile, entity.lastItem, entity.lastInput, false);

            if(target != null && (entity.time >= 1f || !(target.block() instanceof Router))){
                getTileTarget(tile, entity.lastItem, entity.lastInput, true);
                target.block().handleItem(entity.lastItem, target, Edges.getFacingEdge(tile, target));
                entity.items.remove(entity.lastItem, 1);
                entity.lastItem = null;
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();

        return tile.getTeam() == source.getTeam() && entity.lastItem == null && entity.items.total() == 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        RouterEntity entity = tile.entity();
        entity.items.add(item, 1);
        entity.lastItem = item;
        entity.time = 0f;
        entity.lastInput = source;
    }

    Tile getTileTarget(Tile tile, Item item, Tile from, boolean set){
        Array<Tile> proximity = tile.entity.proximity();
        int counter = tile.rotation();
        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + counter) % proximity.size);
            if(set) tile.rotation((byte)((tile.rotation() + 1) % proximity.size));
            if(other == from && from.block() == Blocks.overflowGate) continue;
            if(other.block().acceptItem(item, other, Edges.getFacingEdge(tile, other))){
                return other;
            }
        }
        return null;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        RouterEntity entity = tile.entity();
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == entity.lastItem){
            entity.lastItem = null;
        }
        return result;
    }

    @Override
    public TileEntity newEntity(){
        return new RouterEntity();
    }

    public class RouterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;
    }
}
