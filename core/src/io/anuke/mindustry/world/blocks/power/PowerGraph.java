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

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 || producers.size == 0){
            return;
        }

        lastFrameUpdated = threads.getFrameID();

        float powerProduced = 0f;
        for(Tile producer : producers){
            totalInput += producer.block().getPowerProduction(producer);
        }

        float powerNeeded = 0f;
        for(Tile consumer : consumers){
            powerNeeded += consumer.block().basePowerUse + consumer.entity.power.extraUse;
        }

        float totalAccumulator = 0f;
        float totalCapacity = 0f;
        for(Tile battery : batteries){
            totalAccumulator += battery.entity.power.satisfaction * battery.block().basePowerUse;
            totalCapacity += (1f - battery.entity.power.satisfaction) * battery.block().basePowerUse;
        }

        if(powerNeeded > powerProduced){
            float accumulatorUsed = Math.min(totalAccumulator, powerNeeded - powerProduced);
            float thing = 1f - (accumulatorUsed / totalAccumulator);
            for(Tile battery : batteries){
                battery.entity.power.satisfaction *= thing;
            }
            powerProduced += accumulatorUsed;
        }

        float powerSatisfaction = Math.max(1, powerProduced / powerNeeded);
        for(Tile consumer : producers){
            consumer.power.satisfaction = powerSatisfaction;
        }

        if(powerProduced > powerNeeded){
            powerProduced -= powerNeeded;
            float thing = Math.min(1, powerProduced / totalCapacity);
            for(tile battery : batteries){
                battery.power.satisfaction += (1 - battery.power.satisfaction) * thing;
            }
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
            if(other.entity != null && other.entity.power != null) other.entity.power.graph = null;
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
        ", batteries=" + batteries +
        ", all=" + all +
        ", lastFrameUpdated=" + lastFrameUpdated +
        ", graphID=" + graphID +
        '}';
    }
}
