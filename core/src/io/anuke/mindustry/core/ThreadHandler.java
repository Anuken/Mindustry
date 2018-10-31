package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Threads;
import io.anuke.ucore.util.Threads.ThreadInfoProvider;

import static io.anuke.mindustry.Vars.control;
import static io.anuke.mindustry.Vars.logic;

public class ThreadHandler implements ThreadInfoProvider{
    private final Queue<Runnable> toRun = new Queue<>();
    private Thread thread, graphicsThread;
    private final Object updateLock = new Object();
    private float delta = 1f;
    private float smoothDelta = 1f;
    private long frame = 0, lastDeltaUpdate;
    private float framesSinceUpdate;
    private boolean enabled;
    private boolean rendered = true;
    private long lastFrameTime;

    public ThreadHandler(){
        Threads.setThreadInfoProvider(this);
        graphicsThread = Thread.currentThread();

        Timers.setDeltaProvider(() -> {
            float result = isOnThread() ? delta : Gdx.graphics.getDeltaTime() * 60f;
            return Math.min(Float.isNaN(result) ? 1f : result, 15f);
        });
    }

    public void run(Runnable r){
        if(enabled){
            synchronized(toRun){
                toRun.addLast(r);
            }
        }else{
            r.run();
        }
    }

    public void runGraphics(Runnable r){
        if(enabled){
            Gdx.app.postRunnable(r);
        }else{
            r.run();
        }
    }

    public void runDelay(Runnable r){
        if(enabled){
            synchronized(toRun){
                toRun.addLast(r);
            }
        }else{
            Gdx.app.postRunnable(r);
        }
    }

    public int getTPS(){
        if(smoothDelta == 0f){
            return 60;
        }
        return (int) (60 / smoothDelta);
    }

    public long getFrameID(){
        return enabled ? frame : Gdx.graphics.getFrameId();
    }

    public float getFramesSinceUpdate(){
        return framesSinceUpdate;
    }

    public void handleBeginRender(){
        lastFrameTime = TimeUtils.millis();
    }

    public void handleEndRender(){
        int fpsCap = Settings.getInt("fpscap", 125);

        if(fpsCap <= 120){
            long target = 1000/fpsCap;
            long elapsed = TimeUtils.timeSinceMillis(lastFrameTime);
            if(elapsed < target){
                try{
                    Thread.sleep(target - elapsed);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        if(!enabled) return;

        framesSinceUpdate += Timers.delta();

        synchronized(updateLock){
            rendered = true;
            updateLock.notify();
        }
    }

    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        if(enabled){
            logic.doUpdate = false;
            Timers.runTask(2f, () -> {
                if(thread != null){
                    thread.interrupt();
                    thread = null;
                }

                thread = new Thread(this::runLogic);
                thread.setDaemon(true);
                thread.setName("Update Thread");
                thread.start();
                Log.info("Starting logic thread.");

                this.enabled = true;
            });
        }else{
            this.enabled = false;
            if(thread != null){
                thread.interrupt();
                thread = null;
            }
            Timers.runTask(2f, () -> {
                logic.doUpdate = true;
            });
        }
    }

    public boolean doInterpolate(){
        return enabled && Gdx.graphics.getFramesPerSecond() - getTPS() > 20 && getTPS() < 30;
    }

    public boolean isOnThread(){
        return Thread.currentThread() == thread;
    }

    @Override
    public boolean isOnLogicThread() {
        return !enabled || Thread.currentThread() == thread;
    }

    @Override
    public boolean isOnGraphicsThread() {
        return !enabled || Thread.currentThread() == graphicsThread;
    }

    private void runLogic(){
        try{
            while(true){
                long time = TimeUtils.nanoTime();

                while(true){
                    Runnable r;
                    synchronized(toRun){
                        if(toRun.size > 0){
                            r = toRun.removeFirst();
                        }else{
                            break;
                        }
                    }

                    r.run();
                }

                logic.doUpdate = true;
                logic.update();
                logic.doUpdate = false;

                long elapsed = TimeUtils.nanosToMillis(TimeUtils.timeSinceNanos(time));
                long target = (long) ((1000) / 60f);

                if(elapsed < target){
                    Thread.sleep(target - elapsed);
                }

                synchronized(updateLock){
                    while(!rendered){
                        updateLock.wait();
                    }
                    rendered = false;
                }

                long actuallyElapsed = TimeUtils.nanosToMillis(TimeUtils.timeSinceNanos(time));
                delta = Math.max(actuallyElapsed, target) / 1000f * 60f;

                if(TimeUtils.timeSinceMillis(lastDeltaUpdate) > 1000){
                    lastDeltaUpdate = TimeUtils.millis();
                    smoothDelta = delta;
                }

                frame++;
                framesSinceUpdate = 0;
            }
        }catch(InterruptedException ex){
            Log.info("Stopping logic thread.");
        }catch(Throwable ex){
            control.setError(ex);
        }
    }
}
