package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.control;
import static io.anuke.mindustry.Vars.logic;

public class ThreadHandler{
    private final Queue<Runnable> toRun = new Queue<>();
    private final ThreadProvider impl;
    private final Object updateLock = new Object();
    private float delta = 1f;
    private float smoothDelta = 1f;
    private long frame = 0, lastDeltaUpdate;
    private float framesSinceUpdate;
    private boolean enabled;
    private boolean rendered = true;

    public ThreadHandler(ThreadProvider impl){
        this.impl = impl;

        Timers.setDeltaProvider(() -> {
            float result = impl.isOnThread() ? delta : Gdx.graphics.getDeltaTime() * 60f;
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
        return (int) (60 / smoothDelta);
    }

    public long getFrameID(){
        return enabled ? frame : Gdx.graphics.getFrameId();
    }

    public float getFramesSinceUpdate(){
        return framesSinceUpdate;
    }

    public void handleRender(){

        if(!enabled) return;

        framesSinceUpdate += Timers.delta();

        synchronized(updateLock){
            rendered = true;
            impl.notify(updateLock);
        }
    }

    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        if(enabled){
            logic.doUpdate = false;
            Timers.runTask(2f, () -> {
                impl.start(this::runLogic);
                this.enabled = true;
            });
        }else{
            this.enabled = false;
            impl.stop();
            Timers.runTask(2f, () -> {
                logic.doUpdate = true;
            });
        }
    }

    public boolean doInterpolate(){
        return enabled && Gdx.graphics.getFramesPerSecond() - getTPS() > 20 && getTPS() < 30;
    }

    public boolean isOnThread(){
        return impl.isOnThread();
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
                    impl.sleep(target - elapsed);
                }

                synchronized(updateLock){
                    while(!rendered){
                        impl.wait(updateLock);
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

    public interface ThreadProvider{
        boolean isOnThread();

        void sleep(long ms) throws InterruptedException;

        void start(Runnable run);

        void stop();

        void wait(Object object) throws InterruptedException;

        void notify(Object object);
    }
}
