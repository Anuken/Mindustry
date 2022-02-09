package mindustry.ai;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import mindustry.game.EventType.*;

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
    IntMap<PathRequest> requests = new IntMap<>();


    /** @return the next target ID to use as a unique path identifier. */
    public int nextTargetId(){
        return lastTargetId ++;
    }

    public ControlPathfinder(){
        Events.on(WorldLoadEvent.class, event -> {
            stop();


            start();
        });

        Events.on(ResetEvent.class, event -> stop());
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
        public int lastRequestFrame;

        public void update(long maxUpdateNs){

        }
    }
}
