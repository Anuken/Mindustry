package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Vault extends StorageBlock{

    public Vault(String name){
        super(name);
        solid = true;
        update = true;
        itemCapacity = 1000;
    }

    @Override
    public void update(Tile tile){
        int iterations = Math.max(1, (int) (Timers.delta() + 0.4f));

        for(int i = 0; i < iterations; i++){
            if(tile.entity.items.total() > 0){
                tryDump(tile);
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        super.handleItem(item, tile, source);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.total() < itemCapacity;
    }

    @Override
    public boolean tryDump(Tile tile, Item todump){
        TileEntity entity = tile.entity;
        if(entity == null || !hasItems || tile.entity.items.total() == 0 || (todump != null && !entity.items.has(todump)))
            return false;

        Array<Tile> proximity = entity.proximity();
        int dump = tile.getDump();

        if(proximity.size == 0) return false;

        for(int i = 0; i < proximity.size; i++){
            Tile other = proximity.get((i + dump) % proximity.size);
            Tile in = Edges.getFacingEdge(tile, other);

            if(other == null || !(other.block() instanceof StorageBlock)) continue;

            if(!(other.block() instanceof Vault)){

                for(int ii = 0; ii < Item.all().size; ii++){
                    Item item = Item.getByID(ii);

                    if(entity.items.has(item) && other.block().acceptItem(item, other, in) && canDump(tile, other, item)){
                        other.block().handleItem(item, other, in);
                        tile.entity.items.remove(item, 1);
                        incrementDump(tile, proximity.size);
                        return true;
                    }
                }
            }else{
                todump = Item.getByID(0);

                if(other.block().acceptItem(todump, other, in) && canDump(tile, other, todump)){
                    other.block().handleItem(removeItem(tile, null), other, in);
                    incrementDump(tile, proximity.size);
                    return true;
                }
            }

            incrementDump(tile, proximity.size);
        }

        return false;
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return !(to.block() instanceof Vault) || (float) to.entity.items.total() / to.block().itemCapacity < (float) tile.entity.items.total() / itemCapacity;
    }
}
