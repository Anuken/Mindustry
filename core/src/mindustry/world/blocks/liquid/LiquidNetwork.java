package mindustry.world.blocks.liquid;

import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;

import java.util.Arrays;

/** Represents a network of connected liquid buildings (conduits, routers, tanks) that share liquid as a merged pool. */
public class LiquidNetwork{
    private static final arc.struct.Queue<Building> queue = new arc.struct.Queue<>();
    private static final Seq<Building> outArray1 = new Seq<>();
    private static final Seq<Building> outArray2 = new Seq<>();
    private static final IntSet closedSet = new IntSet();

    /** All buildings in this network. */
    public final Seq<Building> all = new Seq<>(false, 16, Building.class);

    private final int graphID;
    private static int lastGraphID;

    public LiquidNetwork(){
        graphID = lastGraphID++;
    }

    public int getID(){
        return graphID;
    }

    /** Adds a building to this network. */
    public void add(Building build){
        if(build == null || build.liquids == null) return;

        // Remove from old network if exists
        if(build.liquidGraph != null && build.liquidGraph != this){
            build.liquidGraph.all.remove(build);
        }

        build.liquidGraph = this;

        if(!all.contains(build)){
            all.add(build);
        }
    }

    /** Merges another network into this one. */
    public void addGraph(LiquidNetwork other){
        if(other == this) return;

        // Merge into larger graph
        if(other.all.size > all.size){
            other.addGraph(this);
            return;
        }

        // Add all buildings from other network
        for(Building build : other.all){
            add(build);
        }
    }

    /** Reflows the network starting from a tile using BFS. */
    public void reflow(Building tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();

        while(queue.size > 0){
            Building child = queue.removeFirst();
            add(child);

            for(Building next : child.getLiquidConnections(outArray2)){
                if(closedSet.add(next.pos())){
                    queue.addLast(next);
                }
            }
        }
    }

    /** Removes a tile and splits the network into separate graphs. */
    public void remove(Building tile){
        // Go through all liquid connections of this tile
        for(Building other : tile.getLiquidConnections(outArray1)){
            // Skip if already reassigned
            if(other.liquidGraph != this) continue;

            // Create new graph for this branch
            LiquidNetwork graph = new LiquidNetwork();
            graph.add(other);

            // BFS to build new graph
            queue.clear();
            queue.addLast(other);

            while(queue.size > 0){
                Building child = queue.removeFirst();
                graph.add(child);

                for(Building next : child.getLiquidConnections(outArray2)){
                    if(next != tile && next.liquidGraph != graph){
                        graph.add(next);
                        queue.addLast(next);
                    }
                }
            }
        }
    }

    /** Clears all buildings from this network. */
    public void clear(){
        all.clear();
    }

    @Override
    public String toString(){
        return "LiquidNetwork{" +
        "graphID=" + graphID +
        ", buildings=" + all.size +
        '}';
    }
}
