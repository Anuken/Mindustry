package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();
    private final TileEntity seed;

    private long lastFrameUpdated;

    public PowerGraph(TileEntity seed){
        this.seed = seed;
    }

    public boolean isSeed(TileEntity entity){
        return seed == entity;
    }

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 || producers.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        for(Tile producer : producers){
            float accumulator = producer.entity.power.amount;

            float toEach = accumulator / consumers.size;
            float outputs = 0f;

            for(Tile tile : consumers){
                outputs += Math.min(tile.block().powerCapacity - tile.entity.power.amount, toEach) / toEach;
            }

            float finalEach = toEach / outputs;
            float buffer = 0f;

            if(Float.isNaN(finalEach)){
                return;
            }

            for(Tile tile : consumers){
                float used = Math.min(tile.block().powerCapacity - tile.entity.power.amount, finalEach);
                buffer += used;
                tile.entity.power.amount += used;
            }

            producer.entity.power.amount -= buffer;
        }
    }

    public void add(Tile tile){
        all.add(tile);
        if(tile.block().outputsPower){
            producers.add(tile);
        }else{
            consumers.add(tile);
        }
    }

    public void remove(Tile tile){
        all.add(tile);
        producers.remove(tile);
        consumers.remove(tile);
    }
}
