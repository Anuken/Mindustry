package io.anuke.mindustry.net;


import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Interpolator {
    //used for movement
    public Vector2 target = new Vector2();
    public Vector2 last = new Vector2();
    public float[] targets = {};
    public float spacing = 1f;
    public float time;

    //current state
    public Vector2 pos = new Vector2();
    public float[] values = {};

    public void read(float cx, float cy, float x, float y, long sent, float... target1ds){
        targets = target1ds;
        time = 0f;
        last.set(cx, cy);
        target.set(x, y);
        spacing = Math.min(Math.max(((TimeUtils.timeSinceMillis(sent) / 1000f) * 60f), 4f), 10);
    }

    public void update(){

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
        }

    }
}