package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();

    private long lastFrameUpdated;

    public void update(){
        //Log.info("producers {0}\nconsumers {1}\nall {2}", producers, consumers, all);
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
        for(Tile other : all){
            other.entity.power.graph = null;
        }

        all.remove(tile);
        producers.remove(tile);
        consumers.remove(tile);

        for(Tile other : tile.entity.proximity()){
            if(other.entity.power.graph != null) continue;
            PowerGraph graph = new PowerGraph();
            queue.clear();
            queue.addLast(other);
            while(queue.size > 0){
                Tile child = queue.removeFirst();
                child.entity.power.graph = graph;
                add(child);
                for(Tile next : child.entity.proximity()){
                    if(next != tile && next.entity.power != null && next.entity.power.graph == null){
                        queue.addLast(next);
                    }
                }
            }
        }
    }
}
