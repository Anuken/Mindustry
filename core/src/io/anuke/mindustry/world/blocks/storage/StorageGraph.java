package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.StorageBlock.StorageEntity;
import io.anuke.mindustry.world.modules.ItemModule;

public class StorageGraph{
    private static IntSet closedSet = new IntSet();
    private static Queue<Tile> queue = new Queue<>();
    private static ObjectSet<ItemModule> itemSet = new ObjectSet<>();
    private static int lastID;

    private final int id = lastID++;
    private ObjectSet<Tile> tiles = new ObjectSet<>();
    private ItemModule items = new ItemModule();
    private int capacity;

    public void set(Tile tile){
        items.addAll(tile.entity.items);
        items.setID(tile.entity.items.getID());

        add(tile);
    }

    public void add(Tile tile){

        if(!tiles.add(tile)) return;

        StorageEntity e = tile.entity();
        e.graph = this;

        capacity += tile.block().itemCapacity;

        if(tile.entity.items != null && tile.entity.items.getID() != items.getID()){
            items.addAll(tile.entity.items);
        }

        tile.entity.items = items;
    }

    public void remove(Tile tile){
        if(!tiles.contains(tile)) return;

        for(Tile other : tiles){
            if(other == tile) continue;

            StorageEntity entity = other.entity();
            entity.graph = null;
            entity.items = new ItemModule();

            float fraction = (float)other.block().itemCapacity / capacity;
            items.forEach((item, amount) -> {
                int added = (int)(fraction * amount);
                entity.items.add(item, added);
                items.remove(item, added);
            });
        }

        //handle remaining items that didn't get added
        Item taken;
        while((taken = items.take()) != null){
            for(Tile other : tiles){
                if(other == tile) continue;

                //insert item into first found block
                if(other.entity.items.get(taken) < other.block().itemCapacity){
                    other.entity.items.add(taken, 1);
                    break;
                }
            }
        }

        items.clear();
        capacity = 0;

        for(Tile other : tile.entity.proximity()){
            if(other.block() instanceof StorageBlock && other.<StorageEntity>entity().graph == null){
                StorageGraph graph = new StorageGraph();
                other.<StorageEntity>entity().graph = graph;
                graph.reflow(tile, other);
            }
        }
    }

    public void reflow(Tile base, Tile tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        itemSet.clear();

        while(queue.size > 0){
            Tile child = queue.removeFirst();
            StorageEntity entity = child.entity();
            entity.graph = this;

            if(!itemSet.add(child.entity.items)) child.entity.items = null;
            add(child);

            for(Tile next : child.entity.proximity()){
                if(next != base && next.block() instanceof StorageBlock && next.<StorageEntity>entity().graph == null && !closedSet.contains(next.pos())){
                    queue.addLast(next);
                    closedSet.add(next.pos());
                }
            }
        }
    }

    public void merge(StorageGraph other){
        if(this == other || other == null) return;

        itemSet.clear();
        for(Tile tile : other.tiles){
            if(!itemSet.add(tile.entity.items)){
                tile.entity.items = null;
            }
        }

        for(Tile tile : other.tiles){
            add(tile);
        }
    }

    public boolean accept(Item item){
        return accept(item, 1) == 1;
    }

    public int accept(Item item, int amount){
        return Math.min(capacity - items.get(item), amount);
    }

    public ObjectSet<Tile> getTiles(){
        return tiles;
    }

    public int getID(){
        return id;
    }

    public int getCapacity(){
        return capacity;
    }

    public ItemModule items(){
        return items;
    }

}
