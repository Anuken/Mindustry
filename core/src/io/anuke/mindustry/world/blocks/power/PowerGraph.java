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

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 || producers.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        boolean charge = false;

        float totalInput = 0f;
        float bufferInput = 0f;
        for(Tile producer : producers){
            if(producer.block().consumesPower){
                bufferInput += producer.entity.power.amount;
            }else{
                totalInput += producer.entity.power.amount;
            }
        }

        float maxOutput = 0f;
        float bufferOutput = 0f;
        for(Tile consumer : consumers){
            if(consumer.block().outputsPower){
                bufferOutput += consumer.block().powerCapacity - consumer.entity.power.amount;
            }else{
                maxOutput += consumer.block().powerCapacity - consumer.entity.power.amount;
            }
        }

        if(maxOutput < totalInput){
            charge = true;
        }

        if(totalInput + bufferInput <= 0.0001f || maxOutput + bufferOutput <= 0.0001f){
            return;
        }

        float bufferUsed;
        if(charge){
            bufferUsed = Math.min((totalInput - maxOutput) / bufferOutput, 1f);
        }else{
            bufferUsed = Math.min((maxOutput - totalInput) / bufferInput, 1f);
        }

        float inputUsed = charge ? Math.min((maxOutput + bufferOutput) / totalInput, 1f) : 1f;
        for(Tile producer : producers){
            if(producer.block().consumesPower){
                if(!charge){
                    producer.entity.power.amount -= producer.entity.power.amount * bufferUsed;
                }
                continue;
            }
            producer.entity.power.amount -= producer.entity.power.amount * inputUsed;
        }

        float outputSatisfied = charge ? 1f : Math.min((totalInput + bufferInput) / maxOutput, 1f);
        for(Tile consumer : consumers){
            if(consumer.block().outputsPower){
                if(charge){
                    consumer.entity.power.amount += (consumer.block().powerCapacity - consumer.entity.power.amount) * bufferUsed;
                }
                continue;
            }
            consumer.entity.power.amount += (consumer.block().powerCapacity - consumer.entity.power.amount) * outputSatisfied;
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
        }

        if(tile.block().consumesPower){
            consumers.add(tile);
        }
    }

    public void clear(){
        for(Tile other : all){
            if(other.entity != null && other.entity.power != null) other.entity.power.graph = null;
        }
        all.clear();
        producers.clear();
        consumers.clear();
    }

    public void reflow(Tile tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        while(queue.size > 0){
            Tile child = queue.removeFirst();
            child.entity.power.graph = this;
            add(child);
            for(Tile next : child.block().getPowerConnections(child, outArray2)){
                if(next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.pos())){
                    queue.addLast(next);
                    closedSet.add(next.pos());
                }
            }
        }
    }

    public void remove(Tile tile){
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
                    if(next != tile && next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.pos())){
                        queue.addLast(next);
                        closedSet.add(next.pos());
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
