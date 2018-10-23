package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();
    private final static Array<Tile> outArray1 = new Array<>();
    private final static Array<Tile> outArray2 = new Array<>();
    private final static IntSet closedSet = new IntSet();

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

    public synchronized void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 || producers.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        float totalInput = 0f;
        for(Tile producer : producers){
            totalInput += producer.entity.power.amount;
        }

        float maxOutput = 0f;
        for(Tile consumer : consumers){
            maxOutput += consumer.block().powerCapacity - consumer.entity.power.amount;
        }

        if (totalInput <= 0.0001f || maxOutput <= 0.0001f) {
            return;
        }

        float inputUsed = Math.min(maxOutput / totalInput, 1f);
        for(Tile producer : producers){
            producer.entity.power.amount -= producer.entity.power.amount * inputUsed;
        }

        float outputSatisfied = Math.min(totalInput / maxOutput, 1f);
        for(Tile consumer : consumers){
            consumer.entity.power.amount += (consumer.block().powerCapacity - consumer.entity.power.amount) * outputSatisfied;
        }
    }

    public synchronized void add(PowerGraph graph){
        for(Tile tile : graph.all){
            add(tile);
        }
    }

    public synchronized void add(Tile tile){
        tile.entity.power.graph = this;
        all.add(tile);

        if(tile.block().outputsPower){
            producers.add(tile);
        }

        if(tile.block().consumesPower){
            consumers.add(tile);
        }
    }

    public synchronized void clear(){
        for(Tile other : all){
            other.entity.power.graph = null;
        }
        all.clear();
        producers.clear();
        consumers.clear();
    }

    public synchronized void reflow(Tile tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        while(queue.size > 0){
            Tile child = queue.removeFirst();
            child.entity.power.graph = this;
            add(child);
            for(Tile next : child.block().getPowerConnections(child, outArray2)){
                if(next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.packedPosition())){
                    queue.addLast(next);
                    closedSet.add(next.packedPosition());
                }
            }
        }
    }

    public synchronized void remove(Tile tile){
        clear();
        closedSet.clear();

        for(Tile other : tile.block().getPowerConnections(tile, outArray1)){
            if(other.entity.power == null || other.entity.power.graph != null) continue;
            PowerGraph graph = new PowerGraph();
            queue.clear();
            queue.addLast(other);
            while(queue.size > 0){
                Tile child = queue.removeFirst();
                child.entity.power.graph = graph;
                graph.add(child);
                for(Tile next : child.block().getPowerConnections(child, outArray2)){
                    if(next != tile && next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.packedPosition())){
                        queue.addLast(next);
                        closedSet.add(next.packedPosition());
                    }
                }
            }
        }
    }

    @Override
    public String toString(){
        return "PowerGraph{" +
        "producers=" + producers +
        ", consumers=" + consumers +
        ", all=" + all +
        ", lastFrameUpdated=" + lastFrameUpdated +
        ", graphID=" + graphID +
        '}';
    }
}
