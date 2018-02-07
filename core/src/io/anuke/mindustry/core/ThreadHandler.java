package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.logic;

public class ThreadHandler {
    private final ThreadProvider impl;
    private final Object lock = new Object();
    private float delta = 1f;
    private boolean finished;
    private boolean enabled;

    public ThreadHandler(ThreadProvider impl){
        this.impl = impl;

        Timers.setDeltaProvider(() -> impl.isOnThread() ? delta : Gdx.graphics.getDeltaTime()*60f);
    }

    public void handleRender(){
        synchronized(lock) {
            finished = true;
            lock.notify();
        }
    }

    public void setEnabled(boolean enabled){
        if(enabled){
            logic.doUpdate = false;
            Timers.runTask(2f, () -> impl.start(this::runLogic));
        }else{
            impl.stop();
            Timers.runTask(2f, () -> logic.doUpdate = true);
        }
        this.enabled = enabled;
    }

    public boolean isEnabled(){
        return enabled;
    }

    private void runLogic(){
        try {
            while (true) {
                long time = TimeUtils.millis();
                logic.update();

                long elapsed = TimeUtils.timeSinceMillis(time);
                long target = (long) (1000 / 60f);

                delta = Math.max(elapsed, target) / 1000f * 60f;

                if (elapsed < target) {
                    impl.sleep(target - elapsed);
                }

                synchronized(lock) {
                    while(!finished) {
                        lock.wait();
                    }
                    finished = false;
                }
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
    }
}
