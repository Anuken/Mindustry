package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.consumers.Consumers;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();
    private final static Array<Tile> outArray1 = new Array<>();
    private final static Array<Tile> outArray2 = new Array<>();
    private final static IntSet closedSet = new IntSet();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> batteries = new ObjectSet<>();
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

    public float getPowerProduced(){
        float powerProduced = 0f;
        for(Tile producer : producers){
            powerProduced += producer.block().getPowerProduction(producer) * producer.entity.delta();
        }
        return powerProduced;
    }

    public float getPowerNeeded(){
        float powerNeeded = 0f;
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.has(ConsumePower.class)){
                powerNeeded += consumes.get(ConsumePower.class).requestedPower(consumer.block(), consumer.entity) * consumer.entity.delta();
            }
        }
        return powerNeeded;
    }

    public float getBatteryStored(){
        float totalAccumulator = 0f;
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.has(ConsumePower.class)){
                totalAccumulator += battery.entity.power.satisfaction * consumes.get(ConsumePower.class).powerCapacity;
            }
        }
        return totalAccumulator;
    }

    public float getBatteryCapacity(){
        float totalCapacity = 0f;
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.has(ConsumePower.class)){
                totalCapacity += consumes.get(ConsumePower.class).requestedPower(battery.block(), battery.entity) * battery.entity.delta();
            }
        }
        return totalCapacity;
    }

    public float useBatteries(float needed){
        float stored = getBatteryStored();
        if(MathUtils.isEqual(stored, 0f)){ return 0f; }

        float used = Math.min(stored, needed);
        float consumedPowerPercentage = Math.min(1.0f, needed / stored);
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.has(ConsumePower.class)){
                ConsumePower consumePower = consumes.get(ConsumePower.class);
                if(consumePower.powerCapacity > 0f){
                    battery.entity.power.satisfaction = Math.max(0.0f, battery.entity.power.satisfaction - consumedPowerPercentage);
                }
            }
        }
        return used;
    }

    public float chargeBatteries(float excess){
        float capacity = getBatteryCapacity();
        if(MathUtils.isEqual(capacity, 0f)){ return 0f; }

        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.has(ConsumePower.class)){
                ConsumePower consumePower = consumes.get(ConsumePower.class);
                if(consumePower.powerCapacity > 0f){
                    float additionalPowerPercentage = Math.min(1.0f, excess / consumePower.powerCapacity);
                    battery.entity.power.satisfaction = Math.min(1.0f, battery.entity.power.satisfaction + additionalPowerPercentage);
                }
            }
        }
        return Math.min(excess, capacity);
    }

    public void distributePower(float needed, float produced){
        if(MathUtils.isEqual(needed, 0f)){ return; }

        float coverage = Math.min(1, produced / needed);
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.has(ConsumePower.class)){
                ConsumePower consumePower = consumes.get(ConsumePower.class);
                if(consumePower.isBuffered){
                    // Add a percentage of the requested amount, but limit it to the mission amount.
                    // TODO This can maybe be calculated without converting to absolute values first
                    float maximumRate = consumePower.requestedPower(consumer.block(), consumer.entity()) * coverage * consumer.entity.delta();
                    float missingAmount = consumePower.powerCapacity * (1 - consumer.entity.power.satisfaction);
                    consumer.entity.power.satisfaction += Math.min(missingAmount, maximumRate) / consumePower.powerCapacity;
                }else{
                    consumer.entity.power.satisfaction = coverage;
                }
            }
        }
    }

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 && producers.size == 0 && batteries.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        float powerNeeded = getPowerNeeded();
        float powerProduced = getPowerProduced();

        if(!MathUtils.isEqual(powerNeeded, powerProduced)){
            if(powerNeeded > powerProduced){
                powerProduced += useBatteries(powerNeeded - powerProduced);
            }else if(powerProduced > powerNeeded){
                powerProduced -= chargeBatteries(powerProduced - powerNeeded);
            }
        }

        distributePower(powerNeeded, powerProduced);
    }

    public void add(PowerGraph graph){
        for(Tile tile : graph.all){
            add(tile);
        }
    }

    public void add(Tile tile){
        tile.entity.power.graph = this;
        all.add(tile);

        if(tile.block().outputsPower && tile.block().consumesPower){
            batteries.add(tile);
        }else if(tile.block().outputsPower){
            producers.add(tile);
        }else if(tile.block().consumesPower){
            consumers.add(tile);
        }
    }

    public void clear(){
        for(Tile other : all){
            if(other.entity != null && other.entity.power != null){ other.entity.power.graph = null; }
        }
        all.clear();
        producers.clear();
        consumers.clear();
        batteries.clear();
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
                if(next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.packedPosition())){
                    queue.addLast(next);
                    closedSet.add(next.packedPosition());
                }
            }
        }
    }

    public void remove(Tile tile){
        clear();
        closedSet.clear();

        for(Tile other : tile.block().getPowerConnections(tile, outArray1)){
            if(other.entity.power == null || other.entity.power.graph != null){ continue; }
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
            // Update the graph once so direct consumers without any connected producer lose their power
            graph.update();
        }
    }

    @Override
    public String toString(){
        return "PowerGraph{" +
            "producers=" + producers +
            ", consumers=" + consumers +
            ", batteries=" + batteries +
            ", all=" + all +
            ", lastFrameUpdated=" + lastFrameUpdated +
            ", graphID=" + graphID +
            '}';
    }
}
