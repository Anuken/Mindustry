package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.ucore.core.Timers;

public class Router extends Block{
    protected float speed = 8f;

    public Router(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        itemCapacity = 1;
        group = BlockGroup.transportation;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove(BarType.inventory);
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
        SplitterEntity entity = tile.entity();

        return tile.getTeamID() == source.getTeamID() && entity.lastItem == null && entity.items.total() == 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        SplitterEntity entity = tile.entity();
        entity.items.add(item, 1);
        entity.lastItem = item;
        entity.time = 0f;
        entity.lastInput = source;
    }

    Tile getTileTarget(Tile tile, Item item, Tile from, boolean set){
        Array<Tile> proximity = tile.entity.proximity();
        int counter = tile.getDump();
        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + counter) % proximity.size);
            if(tile == from) continue;
            if(set) tile.setDump((byte) ((tile.getDump() + 1) % proximity.size));
            if(other.block().acceptItem(item, other, Edges.getFacingEdge(tile, other))){
                return other;
            }
        }
        return null;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        SplitterEntity entity = tile.entity();
        int result = super.removeStack(tile, item, amount);
        if(result != 0 && item == entity.lastItem){
            entity.lastItem = null;
        }
        return result;
    }

    @Override
    public TileEntity newEntity(){
        return new SplitterEntity();
    }

    public class SplitterEntity extends TileEntity{
        Item lastItem;
        Tile lastInput;
        float time;
    }
}
