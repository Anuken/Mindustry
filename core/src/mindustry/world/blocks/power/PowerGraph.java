package mindustry.world.blocks.power;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.consumers.*;

public class PowerGraph{
    private static final Queue<Building> queue = new Queue<>();
    private static final Seq<Building> outArray1 = new Seq<>();
    private static final Seq<Building> outArray2 = new Seq<>();
    private static final IntSet closedSet = new IntSet();

    private final Seq<Building> producers = new Seq<>(false);
    private final Seq<Building> consumers = new Seq<>(false);
    private final Seq<Building> batteries = new Seq<>(false);
    private final Seq<Building> all = new Seq<>(false);

    private final WindowedMean powerBalance = new WindowedMean(60);
    private float lastPowerProduced, lastPowerNeeded, lastPowerStored;
    private float lastScaledPowerIn, lastScaledPowerOut, lastCapacity;
    //diodes workaround for correct energy production info
    private float energyDelta = 0f;

    private long lastFrameUpdated = -1;
    private final int graphID;
    private static int lastGraphID;

    {
        graphID = lastGraphID++;
    }

    public int getID(){
        return graphID;
    }

    public float getLastScaledPowerIn(){
        return lastScaledPowerIn;
    }

    public float getLastScaledPowerOut(){
        return lastScaledPowerOut;
    }

    public float getLastCapacity(){
        return lastCapacity;
    }

    public float getPowerBalance(){
        return powerBalance.rawMean();
    }

    public float getLastPowerNeeded(){
        return lastPowerNeeded;
    }

    public float getLastPowerProduced(){
        return lastPowerProduced;
    }

    public float getLastPowerStored(){
        return lastPowerStored;
    }

    public void transferPower(float amount){
        if(amount > 0){
            chargeBatteries(amount);
        }else{
            useBatteries(-amount);
        }
        energyDelta += amount;
    }

    public float getSatisfaction(){
        if(Mathf.zero(lastPowerProduced)){
            return 0f;
        }else if(Mathf.zero(lastPowerNeeded)){
            return 1f;
        }
        return Mathf.clamp(lastPowerProduced / lastPowerNeeded);
    }

    /** @return multiplier of speed at which resources should be consumed for power generation. */
    public float getUsageFraction(){
        return 1f;
    }

    public float getPowerProduced(){
        float powerProduced = 0f;
        for(Building producer : producers){
            powerProduced += producer.getPowerProduction() * producer.delta();
        }
        return powerProduced;
    }

    public float getPowerNeeded(){
        float powerNeeded = 0f;
        for(Building consumer : consumers){
            Consumers consumes = consumer.block.consumes;
            if(consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(otherConsumersAreValid(consumer, consumePower)){
                    powerNeeded += consumePower.requestedPower(consumer) * consumer.delta();
                }
            }
        }
        return powerNeeded;
    }

    public float getBatteryStored(){
        float totalAccumulator = 0f;
        for(Building battery : batteries){
            Consumers consumes = battery.block.consumes;
            if(battery.enabled && consumes.hasPower()){
                totalAccumulator += battery.power.status * consumes.getPower().capacity;
            }
        }
        return totalAccumulator;
    }

    public float getBatteryCapacity(){
        float totalCapacity = 0f;
        for(Building battery : batteries){
            if(battery.enabled && battery.block.consumes.hasPower()){
                ConsumePower power = battery.block.consumes.getPower();
                totalCapacity += (1f - battery.power.status) * power.capacity;
            }
        }
        return totalCapacity;
    }

    public float getTotalBatteryCapacity(){
        float totalCapacity = 0f;
        for(Building battery : batteries){
            if(battery.enabled && battery.block.consumes.hasPower()){
                totalCapacity += battery.block.consumes.getPower().capacity;
            }
        }
        return totalCapacity;
    }

    public float useBatteries(float needed){
        float stored = getBatteryStored();
        if(Mathf.equal(stored, 0f)) return 0f;

        float used = Math.min(stored, needed);
        float consumedPowerPercentage = Math.min(1.0f, needed / stored);
        for(Building battery : batteries){
            Consumers consumes = battery.block.consumes;
            if(battery.enabled && consumes.hasPower()){
                battery.power.status *= (1f-consumedPowerPercentage);
            }
        }
        return used;
    }

    public float chargeBatteries(float excess){
        float capacity = getBatteryCapacity();
        //how much of the missing in each battery % is charged
        float chargedPercent = Math.min(excess/capacity, 1f);
        if(Mathf.equal(capacity, 0f)) return 0f;

        for(Building battery : batteries){
            Consumers consumes = battery.block.consumes;
            if(battery.enabled && consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(consumePower.capacity > 0f){
                    battery.power.status += (1f- battery.power.status) * chargedPercent;
                }
            }
        }
        return Math.min(excess, capacity);
    }

    public void distributePower(float needed, float produced){
        //distribute even if not needed. this is because some might be requiring power but not using it; it updates consumers
        float coverage = Mathf.zero(needed) && Mathf.zero(produced) ? 0f : Mathf.zero(needed) ? 1f : Math.min(1, produced / needed);
        for(Building consumer : consumers){
            Consumers consumes = consumer.block.consumes;
            if(consumes.hasPower()){
                ConsumePower consumePower = consumes.getPower();
                if(consumePower.buffered){
                    if(!Mathf.zero(consumePower.capacity)){
                        // Add an equal percentage of power to all buffers, based on the global power coverage in this graph
                        float maximumRate = consumePower.requestedPower(consumer) * coverage * consumer.delta();
                        consumer.power.status = Mathf.clamp(consumer.power.status + maximumRate / consumePower.capacity);
                    }
                }else{
                    //valid consumers get power as usual
                    if(otherConsumersAreValid(consumer, consumePower)){
                        consumer.power.status = coverage;
                    }else{ //invalid consumers get an estimate, if they were to activate
                        consumer.power.status = Math.min(1, produced / (needed + consumePower.usage * consumer.delta()));
                        //just in case
                        if(Float.isNaN(consumer.power.status)){
                            consumer.power.status = 0f;
                        }
                    }
                }
            }
        }
    }

    public void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated){
            return;
        }else if(!consumers.isEmpty() && consumers.first().cheating()){
            //when cheating, just set status to 1
            for(Building tile : consumers){
                tile.power.status = 1f;
            }

            lastPowerNeeded = lastPowerProduced = 1f;
            return;
        }

        lastFrameUpdated = Core.graphics.getFrameId();

        float powerNeeded = getPowerNeeded();
        float powerProduced = getPowerProduced();

        lastPowerNeeded = powerNeeded;
        lastPowerProduced = powerProduced;

        lastScaledPowerIn = (powerProduced + energyDelta) / Time.delta;
        lastScaledPowerOut = powerNeeded / Time.delta;
        lastCapacity = getTotalBatteryCapacity();
        lastPowerStored = getBatteryStored();

        powerBalance.add((lastPowerProduced - lastPowerNeeded + energyDelta) / Time.delta);
        energyDelta = 0f;

        if(!(consumers.size == 0 && producers.size == 0 && batteries.size == 0)){

            if(!Mathf.equal(powerNeeded, powerProduced)){
                if(powerNeeded > powerProduced){
                    float powerBatteryUsed = useBatteries(powerNeeded - powerProduced);
                    powerProduced += powerBatteryUsed;
                    lastPowerProduced += powerBatteryUsed;
                }else if(powerProduced > powerNeeded){
                    powerProduced -= chargeBatteries(powerProduced - powerNeeded);
                }
            }

            distributePower(powerNeeded, powerProduced);
        }
    }

    public void addGraph(PowerGraph graph){
        if(graph == this) return;

        for(Building tile : graph.all){
            add(tile);
        }
    }

    public void add(Building build){
        if(build == null || build.power == null) return;

        if(build.power.graph != this || !build.power.init){
            build.power.graph = this;
            build.power.init = true;
            all.add(build);

            if(build.block.outputsPower && build.block.consumesPower && !build.block.consumes.getPower().buffered){
                producers.add(build);
                consumers.add(build);
            }else if(build.block.outputsPower && build.block.consumesPower){
                batteries.add(build);
            }else if(build.block.outputsPower){
                producers.add(build);
            }else if(build.block.consumesPower){
                consumers.add(build);
            }
        }
    }

    public void reflow(Building tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        while(queue.size > 0){
            Building child = queue.removeFirst();
            add(child);
            for(Building next : child.getPowerConnections(outArray2)){
                if(closedSet.add(next.pos())){
                    queue.addLast(next);
                }
            }
        }
    }

    /** Used for unit tests only. */
    public void removeList(Building build){
        all.remove(build);
        producers.remove(build);
        consumers.remove(build);
        batteries.remove(build);
    }

    /** Note that this does not actually remove the building from the graph;
     * it creates *new* graphs that contain the correct buildings. */
    public void remove(Building tile){

        //go through all the connections of this tile
        for(Building other : tile.getPowerConnections(outArray1)){
            //a graph has already been assigned to this tile from a previous call, skip it
            if(other.power.graph != this) continue;

            //create graph for this branch
            PowerGraph graph = new PowerGraph();
            graph.add(other);
            //add to queue for BFS
            queue.clear();
            queue.addLast(other);
            while(queue.size > 0){
                //get child from queue
                Building child = queue.removeFirst();
                //add it to the new branch graph
                graph.add(child);
                //go through connections
                for(Building next : child.getPowerConnections(outArray2)){
                    //make sure it hasn't looped back, and that the new graph being assigned hasn't already been assigned
                    //also skip closed tiles
                    if(next != tile && next.power.graph != graph){
                        graph.add(next);
                        queue.addLast(next);
                    }
                }
            }
            //update the graph once so direct consumers without any connected producer lose their power
            graph.update();
        }
    }

    private boolean otherConsumersAreValid(Building tile, Consume consumePower){
        for(Consume cons : tile.block.consumes.all()){
            if(cons != consumePower && !cons.isOptional() && !cons.valid(tile)){
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
