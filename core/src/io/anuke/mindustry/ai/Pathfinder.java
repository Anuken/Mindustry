package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;

public class Pathfinder {
    OptimizedPathFinder find = new OptimizedPathFinder();
    OptimizedPathFinder find2 = new OptimizedPathFinder();
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

        Log.info("JSFSAF elapsed: {0}", Timers.elapsedNs());

        for(Tile tile : p){
            Effects.effect(Fx.breakBlock, tile.worldx(), tile.worldy());
        }

        SmoothGraphPath p2 = new SmoothGraphPath();

        OptimizedPathFinder.unop = true;
        Timers.markNs();
        find2.searchNodePath(start, end, p2);
        new PathSmoother<Tile, Vector2>(new Raycaster()).smoothPath(p2);

        Log.info("UNOP elapsed: {0}", Timers.elapsedNs());

        for(Tile tile : p2){
            Effects.effect(Fx.place, tile.worldx(), tile.worldy());
        }

    }

    public void step(){
        //find.runStep(start, end);
    }

    private void clear(){

    }
}
