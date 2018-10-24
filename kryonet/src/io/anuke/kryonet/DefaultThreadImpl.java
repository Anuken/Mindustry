package io.anuke.kryonet;

import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.ucore.util.Threads;
import io.anuke.ucore.util.Threads.ThreadInfoProvider;
import io.anuke.ucore.util.Log;

public class DefaultThreadImpl implements ThreadProvider, ThreadInfoProvider{
    private Thread thread;

    public DefaultThreadImpl(){
        Threads.setThreadInfoProvider(this);
    }

    @Override
    public boolean isOnLogicThread(){
        return thread == null || isOnThread();
    }

    @Override
    public boolean isOnGraphicsThread(){
        return thread == null || !isOnThread();
    }

    @Override
    public boolean isOnThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void sleep(long ms) throws InterruptedException{
        Thread.sleep(ms);
    }

    @Override
    public void start(Runnable run) {
        if(thread != null){
            thread.interrupt();
            thread = null;
        }

        thread = new Thread(run);
        thread.setDaemon(true);
        thread.setName("Update Thread");
        thread.start();
        Log.info("Starting logic thread.");
    }

    @Override
    public void stop() {
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void wait(Object object) throws InterruptedException{
        object.wait();
    }

    @Override
    public void notify(Object object) {
        object.notify();
    }

}
