package mindustry.world;

import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.world.blocks.*;

import java.util.concurrent.*;

public class LiquidManager{
    private static final int targetFps = 60;
    private static final long targetNanos = 1000 * Time.nanosPerMilli / targetFps;
    private static volatile int globalTick;

    //one thread per team
    //note: buildings across teams don't interact in terms of liquids
    private UpdaterThread[] threads = new UpdaterThread[256];

    public void add(Team team, LiquidUpdater building){
        if(team == Team.derelict) return;
        UpdaterThread thread = threads[team.id];

        if(thread == null){
            thread = threads[team.id] = new UpdaterThread(team);
            thread.start();
        }

        thread.addQueue.add(building);
    }

    public void stop(){
        for(var thread : threads){
            if(thread != null){
                thread.running = false;
                thread.interrupt();
            }
        }
    }

    static class UpdaterThread extends Thread{
        final Team team;
        Seq<LiquidUpdater> updaters = new Seq<>(false, 20, LiquidUpdater.class);
        ConcurrentLinkedQueue<LiquidUpdater> addQueue = new ConcurrentLinkedQueue<>();
        volatile boolean running;

        public UpdaterThread(Team team){
            this.team = team;
        }

        @Override
        public void run(){
            try{
                int innerTick = 0;
                while(running){
                    long nanos = Time.nanos();

                    LiquidUpdater toAdd;
                    while((toAdd = addQueue.poll()) != null){
                        updaters.add(toAdd);
                    }

                    LiquidUpdater[] items = updaters.items;
                    int size = updaters.size;
                    for(int i = 0; i < size; i++){
                        var item = items[i];
                        if(!item.isValid() || item.team() != team){
                            updaters.remove(i);
                            i --;
                            size --;
                        }else{
                            item.updateLiquids();
                        }
                    }

                    innerTick ++;
                    globalTick = innerTick;

                    long elapsed = Time.timeSinceNanos(nanos);
                    if(elapsed < targetNanos){
                        Thread.sleep(elapsed / Time.nanosPerMilli, (int)(elapsed % Time.nanosPerMilli));
                    }
                }

            }catch(InterruptedException e){
                //stop the thread
            }
        }
    }
}
