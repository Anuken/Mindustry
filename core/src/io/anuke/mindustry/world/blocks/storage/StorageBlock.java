package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public abstract class StorageBlock extends Block{

    public StorageBlock(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void onProximityAdded(Tile tile){
        StorageEntity entity = tile.entity();
        entity.graph.add(tile);

        for(Tile prox : tile.entity.proximity()){
            if(prox.block() instanceof StorageBlock){
                StorageEntity other = prox.entity();
                entity.graph.merge(other.graph);
            }
        }
    }

    @Override
    public void onProximityRemoved(Tile tile){
        StorageEntity entity = tile.entity();
        entity.graph.remove(tile);
    }

    @Override
    public TileEntity newEntity(){
        return new StorageEntity();
    }

    @Override
    public Array<Object> getDebugInfo(Tile tile){
        Array<Object> arr = super.getDebugInfo(tile);

        StorageEntity entity = tile.entity();
        arr.addAll("storage graph", entity.graph.getID());

        return arr;
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    public Item removeItem(Tile tile, Item item){
        TileEntity entity = tile.entity;

        if(item == null){
            return entity.items.take();
        }else{
            if(entity.items.has(item)){
                entity.items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    public boolean hasItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        if(item == null){
            return entity.items.total() > 0;
        }else{
            return entity.items.has(item);
        }
    }

    public class StorageEntity extends TileEntity{
        public StorageGraph graph = new StorageGraph();
    }
}
