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
    private static int lastID;

    private final int id = lastID++;
    private ObjectSet<Tile> tiles = new ObjectSet<>();
    private ItemModule items = new ItemModule();
    private int capacity;
    private int cores;

    public void add(Tile tile){
        if(tiles.add(tile)){
            if(tile.block() instanceof CoreBlock) cores ++;
            capacity += tile.block().itemCapacity;

            if(tile.entity.items != items){
                items.addAll(tile.entity.items);
            }

            tile.entity.items = items;
        }
    }

    public void remove(Tile tile){
        for(Tile other : tiles){
            other.<StorageEntity>entity().graph = null;
        }

        cores = 0;
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
        while(queue.size > 0){
            Tile child = queue.removeFirst();
            StorageEntity entity = child.entity();
            entity.graph = this;
            add(child);
            for(Tile next : child.entity.proximity()){
                if(next != base && next.block() instanceof StorageBlock && next.<StorageEntity>entity().graph == null && !closedSet.contains(next.packedPosition())){
                    queue.addLast(next);
                    closedSet.add(next.packedPosition());
                }
            }
        }
    }

    public void merge(StorageGraph other){
        if(this == other || other == null) return;

        for(Tile tile : other.tiles){
            StorageEntity e = tile.entity();
            e.graph = this;
            add(tile);
        }
    }

    public boolean accept(Item item){
        return accept(item, 1) == 1;
    }

    public int accept(Item item, int amount){
        if(hasCores()){
            return Math.min(capacity - items.get(item), amount);
        }else{
            return Math.min(capacity - items.total(), amount);
        }
    }

    public ObjectSet<Tile> getTiles(){
        return tiles;
    }

    public boolean hasCores(){
        return cores > 0;
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
