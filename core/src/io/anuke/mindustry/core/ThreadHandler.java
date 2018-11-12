package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Threads.ThreadInfoProvider;

public class ThreadHandler implements ThreadInfoProvider{
    private long lastFrameTime;

    public ThreadHandler(){
        Timers.setDeltaProvider(() -> {
            float result = Gdx.graphics.getDeltaTime() * 60f;
            return Math.min(Float.isNaN(result) || Float.isInfinite(result) ? 1f : result, 15f);
        });
    }

    public void run(Runnable r){
        r.run();
    }

    public void runGraphics(Runnable r){
        r.run();
    }

    public void runDelay(Runnable r){
        Gdx.app.postRunnable(r);
    }

    public long getFrameID(){
        return Gdx.graphics.getFrameId();
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
    }

    @Override
    public boolean isOnLogicThread() {
        return true;
    }

    @Override
    public boolean isOnGraphicsThread() {
        return true;
    }

}
