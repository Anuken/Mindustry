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
    OptimizedPathFinder find = new OptimizedPathFinder();
    Tile start, end;

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, this::clear);
    }

    public void test(Tile start, Tile end){
        this.start = start;
        this.end = end;
        DefaultGraphPath<Tile> p = new DefaultGraphPath<>();

        OptimizedPathFinder.unop = false;
        Timers.markNs();
        find.searchNodePath(start, end, p);

        for(Tile tile : p){
            Effects.effect(Fx.node1, tile.worldx(), tile.worldy());
        }

        Log.info("JSFSAF elapsed: {0}", Timers.elapsedNs());
    }

    public void step(){
        find.runStep(start, end);
    }

    private void clear(){

    }
}
