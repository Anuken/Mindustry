package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();

    private long lastFrameUpdated;
    private final int graphID;
    private static int lastGraphID;

    {
        graphID = lastGraphID++;
    }

    public int getID(){
        return graphID;
    }

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 || producers.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        for(Tile producer : producers){
            float accumulator = producer.entity.power.amount;

            if(accumulator <= 0.0001f) continue;

            float toEach = accumulator / consumers.size;
            float outputs = 0f;

            for(Tile tile : consumers){
                outputs += Math.min(tile.block().powerCapacity - tile.entity.power.amount, toEach) / toEach;
            }

            float finalEach = toEach / outputs;
            float buffer = 0f;

            if(Float.isNaN(finalEach) || Float.isInfinite(finalEach)){
                continue;
            }

            for(Tile tile : consumers){
                float used = Math.min(tile.block().powerCapacity - tile.entity.power.amount, finalEach);
                buffer += used;
                tile.entity.power.amount += used;
            }

            producer.entity.power.amount -= buffer;
        }
    }

    public void add(PowerGraph graph){
        for(Tile tile : graph.all){
            add(tile);
        }
    }

    public void add(Tile tile){
        tile.entity.power.graph = this;
        all.add(tile);
        if(tile.block().outputsPower){
            producers.add(tile);
        }else{
            consumers.add(tile);
        }
        Log.info("New graph: {0} produce {1} consume {2} total", producers.size, consumers.size, all.size);
    }

    public void remove(Tile tile){
        for(Tile other : all){
            other.entity.power.graph = null;
        }

        all.remove(tile);
        producers.remove(tile);
        consumers.remove(tile);

        for(Tile other : tile.entity.proximity()){
            if(other.entity.power == null || (other.entity.power != null && other.entity.power.graph != null)) continue;
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
