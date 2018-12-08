package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;

import java.util.Comparator;
import java.util.Iterator;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();
    private final static Array<Tile> outArray1 = new Array<>();
    private final static Array<Tile> outArray2 = new Array<>();
    private final static IntSet closedSet = new IntSet();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final Array<Tile> consumers = new Array<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();

    private float produced = 0f;
    private float stored = 0f;
    private float used = 0f;
    private float change = 0f;
    private float charged = 0f;

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
        if(threads.getFrameID() == lastFrameUpdated || (consumers.isEmpty() && producers.isEmpty())){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        // produced = 0f;
        // stored = 0f;
        used = 0f;
        // change = 0f;
        charged = 0f;

        float newStored = 0f;
        // Gather power.
        ObjectSet<Tile> storage = new ObjectSet<>();
        ObjectSet<Tile> realProducers = new ObjectSet<>();
        for(Tile producer : producers){
            if(producer.block().consumesPower){
                storage.add(producer);
            }else{
                realProducers.add(producer);
            }
            newStored += producer.entity.power.amount;
        }

        // Distribute power.
        boolean hasPower = !producers.isEmpty();
        byte priority = 9;
        float localStored = newStored;
        float currentUsed = 0f;
        ObjectSet<Tile> currentConsumers = new ObjectSet<>();
        Array<Tile> modifiedConsumers = new Array<>(consumers);
        modifiedConsumers.add(null);
        for(Tile consumer : modifiedConsumers){
            if(!hasPower) break;
            if(!currentConsumers.isEmpty() || consumer == null || consumer.entity.power.priority != priority){
                if(hasPower = localStored > currentUsed){
                    for(Tile currentConsumer : currentConsumers){
                        if(currentConsumer.block().outputsPower) charged += currentConsumer.block().powerCapacity;
                        currentConsumer.entity.power.amount = currentConsumer.block().powerCapacity;
                    }
                    used += currentUsed;
                    localStored -= currentUsed;
                }else{
                    float fill = localStored / currentUsed;
                    for(Tile currentConsumer : currentConsumers){
                        float amount = (currentConsumer.block().powerCapacity - currentConsumer.entity.power.amount) * fill;
                        if(currentConsumer.block().outputsPower) charged += amount;
                        currentConsumer.entity.power.amount += amount;
                    }
                    used += currentUsed * fill;
                    // localStored = 0f;
                }
                currentConsumers.clear();
                currentUsed = 0f;
            }
            if(consumer == null) break;
            priority = consumer.entity.power.priority;
            currentConsumers.add(consumer);
            currentUsed += consumer.block().powerCapacity - consumer.entity.power.amount;
        }

        // Remove power.
        float localUsed = used;
        for(Tile producer : realProducers){
            if(localUsed <= 0f) break;
            float amount = Math.min(producer.entity.power.amount, localUsed);
            producer.entity.power.amount -= amount;
            localUsed -= amount;
        }
        for(Tile producer : storage){
            if(localUsed <= 0f) break;
            float amount = Math.min(producer.entity.power.amount, localUsed);
            producer.entity.power.amount -= amount;
            localUsed -= amount;
        }

        // Calculate variables.
        used -= charged;
        change = newStored - stored;
        produced = change + used;
        stored = newStored;
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
            sort();
        }
    }

    private static Comparator<Tile> comparator = (o1, o2) -> {
        int result = o2.entity.power.priority - o1.entity.power.priority;
        return result == 0 ? 0 : result / Math.abs(result);
    };

    public void sort(){
        consumers.sort(comparator);
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

    public float getProduced(){
        return produced;
    }

    public float getStored(){
        return stored;
    }

    public float getUsed(){
        return used;
    }

    public float getChange(){
        return change;
    }

    public float getCharged(){
        return charged;
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
