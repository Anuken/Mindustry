package io.anuke.mindustry.net;


import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.util.Mathf;

public class Interpolator {
    //used for movement
    public Vector2 target = new Vector2();
    public Vector2 last = new Vector2();
    public float[] targets = {};
    public long lastUpdated, updateSpacing;

    //current state
    public Vector2 pos = new Vector2();
    public float[] values = {};

    public void read(float cx, float cy, float x, float y, long sent, float... target1ds){
        if(lastUpdated != 0) updateSpacing = TimeUtils.timeSinceMillis(lastUpdated);

        lastUpdated = sent;

        targets = target1ds;
        last.set(cx, cy);
        target.set(x, y);
    }

    public void reset(){
        values = new float[0];
        targets = new float[0];
        target.setZero();
        last.setZero();
        lastUpdated = updateSpacing = 0;
        pos.setZero();
    }

    public void update(){

        if(lastUpdated != 0){
            float timeSinceUpdate = TimeUtils.timeSinceMillis(lastUpdated);
            float alpha = Math.min(timeSinceUpdate / updateSpacing, 1f);

            Mathf.lerp2(last, target, alpha);

            pos.set(last).lerp(target, alpha);

            if(values.length != targets.length){
                values = new float[targets.length];
            }

            for (int i = 0; i < values.length; i++) {
                values[i] = Mathf.slerp(values[i], targets[i], alpha);
            }

            if(target.dst(pos) > 128){
                pos.set(target);
                last.set(target);
            }
        }else{
            pos.set(target);
        }
        /*
        time += 1f / spacing * Math.min(Timers.delta(), 1f);

        time = Mathf.clamp(time, 0, 2f);

        Mathf.lerp2(pos.set(last), target, time);

        if(values.length != targets.length){
            values = new float[targets.length];
        }

        for(int i = 0; i < values.length; i ++){
            values[i] = Mathf.slerpDelta(values[i], targets[i], 0.6f);
        }

        if(target.dst(pos) > 128){
            pos.set(target);
            last.set(target);
            time = 0f;
        }*/

    }
}