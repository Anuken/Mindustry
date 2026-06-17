package mindustry.world;

import arc.struct.*;
import arc.util.*;
import mindustry.world.blocks.*;

import java.util.concurrent.*;

//TODO: consumers read AND write from the liquids array... on the main thread. as do various other blocks. see remove() usage
//TODO: payload loaders/unloaders don't use this system
public class LiquidManager{
    private static final int targetFps = 60;
    private static final long targetNanos = 1000 * Time.nanosPerMilli / targetFps;

    private UpdaterThread thread;
    private int curTick;

    public void checkUpdate(){
        //ensures volatile read
        if(thread != null){
            curTick = thread.tick;
        }
    }

    public void add(LiquidUpdater building){
        if(!building.shouldLiquidUpdate()) return;

        if(thread == null){
            thread = new UpdaterThread();
            thread.start();
        }

        thread.addQueue.add(building);
    }

    public void stop(){
        if(thread != null){
            thread.running = false;
            thread.interrupt();
            thread = null;
        }
    }

    static class UpdaterThread extends Thread{
        Seq<LiquidUpdater> updaters = new Seq<>(false, 20, LiquidUpdater.class);
        ConcurrentLinkedQueue<LiquidUpdater> addQueue = new ConcurrentLinkedQueue<>();
        volatile boolean running = true;
        volatile int tick;

        long lastNanos = Time.nanos();

        @Override
        public void run(){
            try{
                while(running){
                    long nanos = Time.nanos();
                    long deltaNanos = Time.timeSinceNanos(lastNanos);
                    float deltaTicks = (float)deltaNanos / Time.nanosPerMilli / 1000f * 60f;
                    lastNanos = nanos;

                    LiquidUpdater toAdd;
                    while((toAdd = addQueue.poll()) != null){
                        updaters.add(toAdd);
                    }

                    LiquidUpdater[] items = updaters.items;
                    int size = updaters.size;
                    for(int i = 0; i < size; i++){
                        var item = items[i];
                        if(!item.isValid()){
                            //note: this is an un-ordered seq, so this is just a O(1) swap
                            updaters.remove(i);
                            i --;
                            size --;
                        }else{
                            item.updateLiquids(deltaTicks);
                        }
                    }

                    //update 'global tick' as a volatile memory barrier
                    tick ++;

                    long elapsed = Time.timeSinceNanos(nanos);
                    if(elapsed < targetNanos){
                        long remaining = targetNanos - elapsed;
                        Thread.sleep(remaining / Time.nanosPerMilli, (int)(remaining % Time.nanosPerMilli));
                    }
                }

            }catch(InterruptedException e){
                //stop the thread
            }
        }
    }
}
