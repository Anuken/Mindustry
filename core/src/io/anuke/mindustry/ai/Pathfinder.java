package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;

public class Pathfinder {
    private OptimizedPathFinder finder = new OptimizedPathFinder();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, this::clear);
    }

    public void test(Tile start, Tile end){
        DefaultGraphPath<Tile> p = new DefaultGraphPath<>();
/*
        OptimizedPathFinder.unop = true;
        Timers.markNs();
        finder.searchNodePath(start, end, p);
        for(Tile tile : p.nodes){
            Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
        }
        Log.info("Normal elapsed: {0}", Timers.elapsedNs());*/

        OptimizedPathFinder.unop = false;
        Timers.markNs();
        finder.searchNodePath(start, end, p);
        for(Tile tile : p.nodes){
            Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
        }
        Log.info("JSFSAF elapsed: {0}", Timers.elapsedNs());
    }

    private void clear(){

    }
}
