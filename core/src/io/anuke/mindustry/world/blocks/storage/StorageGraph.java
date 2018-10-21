package io.anuke.mindustry.world.blocks.storage;

import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.StorageBlock.StorageEntity;

public class StorageGraph{
    private static IntSet closedSet = new IntSet();
    private static Queue<Tile> queue = new Queue<>();
    private static int lastID;

    private ObjectSet<Tile> tiles = new ObjectSet<>();
    private int cores;
    private int id = lastID++;

    public void add(Tile tile){
        if(tiles.add(tile) && tile.block() instanceof CoreBlock){
            cores ++;
        }
    }

    public void remove(Tile tile){
        for(Tile other : tiles){
            other.<StorageEntity>entity().graph = null;
        }

        cores = 0;

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
        }

        tiles.addAll(other.tiles);
        cores += other.cores;
    }

    public boolean hasCores(){
        return cores > 0;
    }

    public int getID(){
        return id;
    }
}
