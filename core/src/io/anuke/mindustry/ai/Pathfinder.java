package io.anuke.mindustry.ai;

import com.badlogic.gdx.ai.pfa.PathSmoother;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.util.Log;

public class Pathfinder {
    private OptimizedPathFinder find;
    private AsyncExecutor executor = new AsyncExecutor(8);
    private PathSmoother<Tile, Vector2> smoother = new PathSmoother<>(new Raycaster());
    //private HierarchicalPathFinder<Tile> hfinder = new HierarchicalPathFinder<>();

    public Pathfinder(){
        Events.on(WorldLoadEvent.class, this::clear);
    }

    public void findPath(Tile start, Tile end, SmoothGraphPath path,
                         OptimizedPathFinder finder, Listenable completed){
        executor.submit(() -> {
            finder.searchNodePath(start, end, path);
            smoother.smoothPath(path);
            completed.listen();
            return path;
        });
    }

    public void test(Tile start, Tile end){
        SmoothGraphPath p2 = new SmoothGraphPath();

        Timers.markNs();
        find.searchNodePath(start, end, p2);
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
        find = new OptimizedPathFinder();
    }
}
