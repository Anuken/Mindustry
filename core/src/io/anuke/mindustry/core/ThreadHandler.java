package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.logic;

public class ThreadHandler {
    private final Array<Runnable> toRun = new Array<>();
    private final ThreadProvider impl;
    private float delta = 1f;
    private long frame = 0;
    private float framesSinceUpdate;
    private boolean enabled;

    private final Object updateLock = new Object();
    private boolean rendered = true;

    public ThreadHandler(ThreadProvider impl){
        this.impl = impl;

        Timers.setDeltaProvider(() -> impl.isOnThread() ? delta : Gdx.graphics.getDeltaTime()*60f);
    }

    public void run(Runnable r){
        synchronized (toRun) {
            toRun.add(r);
        }
    }

    public int getFPS(){
        return (int)(60/delta);
    }

    public long getFrameID(){
        return frame;
    }

    public float getFramesSinceUpdate(){
        return framesSinceUpdate;
    }

    public void handleRender(){
        if(!enabled) return;

        framesSinceUpdate += Timers.delta();

        synchronized (updateLock) {
            rendered = true;
            impl.notify(updateLock);
        }
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

    public boolean isEnabled(){
        return enabled;
    }

    private void runLogic(){
        try {
            while (true) {
                long time = TimeUtils.millis();

                synchronized (toRun) {
                    for(Runnable r : toRun){
                        r.run();
                    }
                    toRun.clear();
                }

                logic.update();

                long elapsed = TimeUtils.timeSinceMillis(time);
                long target = (long) (1000 / 60f);

                delta = Math.max(elapsed, target) / 1000f * 60f;

                if (elapsed < target) {
                    impl.sleep(target - elapsed);
                }

                synchronized(updateLock) {
                    while(!rendered) {
                        impl.wait(updateLock);
                    }
                    rendered = false;
                }

                frame ++;
                framesSinceUpdate = 0;
            }
        } catch (InterruptedException ex) {
            Log.info("Stopping logic thread.");
        } catch (Exception ex) {
            Gdx.app.postRunnable(() -> {
                throw new RuntimeException(ex);
            });
        }
    }

    public interface ThreadProvider {
        boolean isOnThread();
        void sleep(long ms) throws InterruptedException;
        void start(Runnable run);
        void stop();
        void wait(Object object) throws InterruptedException;
        void notify(Object object);
    }
}
