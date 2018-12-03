package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;

import static io.anuke.mindustry.Vars.tilesize;

public abstract class StorageBlock extends Block{

    public StorageBlock(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove(BarType.inventory);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void onProximityAdded(Tile tile){
        StorageEntity entity = tile.entity();
        entity.graph.set(tile);

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
    public void drawSelect(Tile tile){

        StorageEntity entity = tile.entity();

        if(entity.graph.getTiles().size > 1){

            Shaders.outline.color.set(Palette.accent);
            Graphics.beginShaders(Shaders.outline);

            for(Tile other : entity.graph.getTiles()){
                Fill.square(other.drawx(), other.drawy(), other.block().size * tilesize);
            }

            Draw.color(Color.CLEAR);
            Graphics.endShaders();
            Draw.color();
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        StorageEntity entity = tile.entity();
        return entity.graph.accept(item);
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        StorageEntity entity = tile.entity();
        if(acceptItem(item, tile, tile) && hasItems && (source == null || source.getTeam() == tile.getTeam())){
            return Math.min(entity.graph.accept(item, amount), amount);
        }else{
            return 0;
        }
    }

    @Override
    public float inventoryScaling(Tile tile){
        StorageEntity entity = tile.entity();
        return 1f / entity.graph.getTiles().size;
    }

    @Override
    public TileEntity newEntity(){
        return new StorageEntity();
    }

    @Override
    public Array<Object> getDebugInfo(Tile tile){
        Array<Object> arr = super.getDebugInfo(tile);

        StorageEntity entity = tile.entity();
        arr.addAll("storage graph", entity.graph.getID(),
            "graph capacity", entity.graph.getCapacity(),
            "graph tiles", entity.graph.getTiles().size,
            "graph item ID", entity.graph.items().getID());

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
