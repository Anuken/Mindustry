package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.collection.Queue;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.WindowedMean;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.Consume;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.consumers.Consumers;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();
    private final static Array<Tile> outArray1 = new Array<>();
    private final static Array<Tile> outArray2 = new Array<>();
    private final static IntSet closedSet = new IntSet();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> batteries = new ObjectSet<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();

    private final WindowedMean powerBalance = new WindowedMean(60);

    private long lastFrameUpdated = -1;
    private final int graphID;
    private static int lastGraphID;

    {
        graphID = lastGraphID++;
    }

    public int getID(){
        return graphID;
    }

    public float getPowerBalance(){
        return powerBalance.getMean();
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
                ConsumePower consumePower = consumes.get(ConsumePower.class);
                if(otherConsumersAreValid(consumer, consumePower)){
                    powerNeeded += consumePower.requestedPower(consumer.block(), consumer.entity) * consumer.entity.delta();
                }
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
        if(Mathf.isEqual(stored, 0f)) return 0f;

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
        if(Mathf.isEqual(capacity, 0f)) return 0f;

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
        //distribute even if not needed. this is because some might be requiring power but not requesting it; it updates consumers
        float coverage = Mathf.isZero(needed) ? 1f : Math.min(1, produced / needed);
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.has(ConsumePower.class)){
                ConsumePower consumePower = consumes.get(ConsumePower.class);
                //currently satisfies power even if it's not required yet
                if(consumePower.isBuffered){
                    if(!Mathf.isZero(consumePower.powerCapacity)){
                        // Add an equal percentage of power to all buffers, based on the global power coverage in this graph
                        float maximumRate = consumePower.requestedPower(consumer.block(), consumer.entity()) * coverage * consumer.entity.delta();
                        consumer.entity.power.satisfaction = Mathf.clamp(consumer.entity.power.satisfaction + maximumRate / consumePower.powerCapacity);
                    }
                }else{
                    consumer.entity.power.satisfaction = coverage;
                }
            }
        }
    }

    public void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated){
            return;
        }

        lastFrameUpdated = Core.graphics.getFrameId();

        float powerNeeded = getPowerNeeded();
        float powerProduced = getPowerProduced();

        powerBalance.addValue((powerProduced - powerNeeded) / Time.delta());

        if(consumers.size == 0 && producers.size == 0 && batteries.size == 0){
            return;
        }

        if(!Mathf.isEqual(powerNeeded, powerProduced)){
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

        if(tile.block().outputsPower && tile.block().consumesPower && !tile.block().consumes.get(ConsumePower.class).isBuffered){
            producers.add(tile);
            consumers.add(tile);
        }else if(tile.block().outputsPower && tile.block().consumesPower){
            batteries.add(tile);
        }else if(tile.block().outputsPower){
            producers.add(tile);
        }else if(tile.block().consumesPower){
            consumers.add(tile);
        }
    }

    public void clear(){
        for(Tile other : all){
            if(other.entity != null && other.entity.power != null){
                if(other.block().consumes.has(ConsumePower.class) && !other.block().consumes.get(ConsumePower.class).isBuffered){
                    // Reset satisfaction to zero in case of direct consumer. There is no reason to clear power from buffered consumers.
                    other.entity.power.satisfaction = 0.0f;
                }
                other.entity.power.graph = null;
            }
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
            if(other.entity.power == null || other.entity.power.graph != null){ continue; }
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
            // Update the graph once so direct consumers without any connected producer lose their power
            graph.update();
        }
    }

    private boolean otherConsumersAreValid(Tile tile, Consume consumePower){
        for(Consume cons : tile.block().consumes.all()){
            if(cons != consumePower && !cons.isOptional() && !cons.valid(tile.block(), tile.entity())){
                return false;
            }
        }
        return true;
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
