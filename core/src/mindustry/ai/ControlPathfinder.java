package mindustry.ai;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ControlPathfinder implements Runnable{
    private static final long maxUpdate = Time.millisToNanos(7);
    private static final int updateFPS = 60;
    private static final int updateInterval = 1000 / updateFPS;

    //MAIN THREAD DATA
    /** Current pathfinding thread */
    @Nullable Thread thread;
    /** for unique target IDs */
    int lastTargetId;
    /** handles task scheduling on the update thread. */
    TaskQueue queue = new TaskQueue();

    //PATHFINDING THREAD DATA
    ObjectMap<Unit, PathRequest> requests = new ObjectMap<>();

    public ControlPathfinder(){
        Events.on(WorldLoadEvent.class, event -> {
            stop();


            start();
        });

        Events.on(ResetEvent.class, event -> stop());
    }


    /** @return the next target ID to use as a unique path identifier. */
    public int nextTargetId(){
        return lastTargetId ++;
    }

    public void getPathPosition(Unit unit, int pathId, Vec2 destination){

    }

    /** Starts or restarts the pathfinding thread. */
    private void start(){
        stop();
        thread = Threads.daemon("ControlPathfinder", this);
    }

    /** Stops the pathfinding thread. */
    private void stop(){
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
        requests.clear();
    }

    //distance heuristic: manhattan
    private static float dstCost(float x, float y, float x2, float y2){
        return Math.abs(x - x2) + Math.abs(x2 - y2);
    }

    private static float tileCost(Tile a, Tile b){
        return 1f;
    }

    @Override
    public void run(){
        while(true){
            if(net.client()) return;
            try{
                if(state.isPlaying()){
                    queue.run();

                    //total update time no longer than maxUpdate
                    //for(Flowfield data : threadList){
                    //    updateFrontier(data, maxUpdate / threadList.size);
                    //}

                    for(var entry : requests){
                        entry.value.update(maxUpdate / requests.size);
                    }
                }

                try{
                    Thread.sleep(updateInterval);
                }catch(InterruptedException e){
                    //stop looping when interrupted externally
                    return;
                }
            }catch(Throwable e){
                //do not crash the pathfinding thread
                Log.err(e);
            }
        }
    }

    class PathRequest{
        GridBits closed;
        PQueue<Tile> queue;

        //TODO how will costs be computed? where will they be stored...?

        int lastId;
        int curId;
        int lastFrame;

        PathRequest(){
            clear();

            lastId = curId;
        }

        void update(long maxUpdateNs){
            if(curId != lastId){
                clear();
            }


        }

        void clear(){
            //TODO

            closed = new GridBits(world.width(), world.height());
            queue = new PQueue<>(16, (a, b) -> 0);
        }
    }
}
