package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.WindowedMean;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.*;

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
    private float lastPowerProduced, lastPowerNeeded;

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

    public float getLastPowerNeeded(){
        return lastPowerNeeded;
    }

    public float getLastPowerProduced(){
        return lastPowerProduced;
    }

    public float getSatisfaction(){
        if(Mathf.isZero(lastPowerProduced)){
            return 0f;
        }else if(Mathf.isZero(lastPowerNeeded)){
            return 1f;
        }
        return Mathf.clamp(lastPowerProduced / lastPowerNeeded);
    }

    public float getPowerProduced(){
        float powerProduced = 0f;
        for(Tile producer : producers){
            if(producer.entity == null) continue;
            powerProduced += producer.block().getPowerProduction(producer) * producer.entity.delta();
        }
        return powerProduced;
    }

    public float getPowerNeeded(){
        float powerNeeded = 0f;
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(otherConsumersAreValid(consumer, consumePower)){
                    powerNeeded += consumePower.requestedPower(consumer.entity) * consumer.entity.delta();
                }
            }
        }
        return powerNeeded;
    }

    public float getBatteryStored(){
        float totalAccumulator = 0f;
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasPower()){
                totalAccumulator += battery.entity.power.satisfaction * consumes.getPower().capacity;
            }
        }
        return totalAccumulator;
    }

    public float getBatteryCapacity(){
        float totalCapacity = 0f;
        for(Tile battery : batteries){
            if(battery.block().consumes.hasPower()){
                ConsumePower power = battery.block().consumes.getPower();
                totalCapacity += (1f - battery.entity.power.satisfaction) * power.capacity;
            }
        }
        return totalCapacity;
    }

    public float getTotalBatteryCapacity(){
        float totalCapacity = 0f;
        for(Tile battery : batteries){
            if(battery.block().consumes.hasPower()){
                totalCapacity += battery.block().consumes.getPower().capacity;
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
            if(consumes.hasPower()){
                battery.entity.power.satisfaction *= (1f-consumedPowerPercentage);
            }
        }
        return used;
    }

    public float chargeBatteries(float excess){
        float capacity = getBatteryCapacity();
        //how much of the missing in each battery % is charged
        float chargedPercent = Math.min(excess/capacity, 1f);
        if(Mathf.isEqual(capacity, 0f)) return 0f;

        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(consumePower.capacity > 0f){
                    battery.entity.power.satisfaction += (1f-battery.entity.power.satisfaction) * chargedPercent;
                }
            }
        }
        return Math.min(excess, capacity);
    }

    public void distributePower(float needed, float produced){
        //distribute even if not needed. this is because some might be requiring power but not using it; it updates consumers
        float coverage = Mathf.isZero(needed) && Mathf.isZero(produced) ? 0f : Mathf.isZero(needed) ? 1f : Math.min(1, produced / needed);
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(consumePower.buffered){
                    if(!Mathf.isZero(consumePower.capacity)){
                        // Add an equal percentage of power to all buffers, based on the global power coverage in this graph
                        float maximumRate = consumePower.requestedPower(consumer.entity) * coverage * consumer.entity.delta();
                        consumer.entity.power.satisfaction = Mathf.clamp(consumer.entity.power.satisfaction + maximumRate / consumePower.capacity);
                    }
                }else{
                    //valid consumers get power as usual
                    if(otherConsumersAreValid(consumer, consumePower)){
                        consumer.entity.power.satisfaction = coverage;
                    }else{ //invalid consumers get an estimate, if they were to activate
                        consumer.entity.power.satisfaction = Math.min(1, produced / (needed + consumePower.usage * consumer.entity.delta()));
                        //just in case
                        if(Float.isNaN(consumer.entity.power.satisfaction)){
                            consumer.entity.power.satisfaction = 0f;
                        }
                    }
                }
            }
        }
    }

    public void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated){
            return;
        }else if(!consumers.isEmpty() && consumers.first().isEnemyCheat()){
            //when cheating, just set satisfaction to 1
            for(Tile tile : consumers){
                tile.entity.power.satisfaction = 1f;
            }

            return;
        }

        lastFrameUpdated = Core.graphics.getFrameId();

        float powerNeeded = getPowerNeeded();
        float powerProduced = getPowerProduced();

        lastPowerNeeded = powerNeeded;
        lastPowerProduced = powerProduced;

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
        if(tile.entity == null || tile.entity.power == null) return;
        tile.entity.power.graph = this;
        all.add(tile);

        if(tile.block().outputsPower && tile.block().consumesPower && !tile.block().consumes.getPower().buffered){
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

    public void reflow(Tile tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        while(queue.size > 0){
            Tile child = queue.removeFirst();
            add(child);
            for(Tile next : child.block().getPowerConnections(child, outArray2)){
                if(!closedSet.contains(next.pos())){
                    queue.addLast(next);
                    closedSet.add(next.pos());
                }
            }
        }
    }

    private void removeSingle(Tile tile){
        all.remove(tile);
        producers.remove(tile);
        consumers.remove(tile);
        batteries.remove(tile);
    }

    public void remove(Tile tile){
        removeSingle(tile);
        //begin by clearing the closed set
        closedSet.clear();

        //go through all the connections of this tile
        for(Tile other : tile.block().getPowerConnections(tile, outArray1)){
            //a graph has already been assigned to this tile from a previous call, skip it
            if(other.entity.power.graph != this) continue;

            //create graph for this branch
            PowerGraph graph = new PowerGraph();
            graph.add(other);
            //add to queue for BFS
            queue.clear();
            queue.addLast(other);
            while(queue.size > 0){
                //get child from queue
                Tile child = queue.removeFirst();
                //remove it from this graph
                removeSingle(child);
                //add it to the new branch graph
                graph.add(child);
                //go through connections
                for(Tile next : child.block().getPowerConnections(child, outArray2)){
                    //make sure it hasn't looped back, and that the new graph being assigned hasn't already been assigned
                    //also skip closed tiles
                    if(next != tile && next.entity.power.graph != graph && !closedSet.contains(next.pos())){
                        queue.addLast(next);
                        closedSet.add(next.pos());
                    }
                }
            }
            //update the graph once so direct consumers without any connected producer lose their power
            graph.update();
        }
    }

    private boolean otherConsumersAreValid(Tile tile, Consume consumePower){
        for(Consume cons : tile.block().consumes.all()){
            if(cons != consumePower && !cons.isOptional() && !cons.valid(tile.entity())){
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
