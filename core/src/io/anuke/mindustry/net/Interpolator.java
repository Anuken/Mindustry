package io.anuke.mindustry.net;

import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;

public class Interpolator{
    //used for movement
    public Vector2 target = new Vector2();
    public Vector2 last = new Vector2();
    public float[] targets = {};
    public float[] lasts = {};
    public long lastUpdated, updateSpacing;

    //current state
    public Vector2 pos = new Vector2();
    public float[] values = {};

    public void read(float cx, float cy, float x, float y, float... target1ds){
        if(lastUpdated != 0) updateSpacing = Time.timeSinceMillis(lastUpdated);

        lastUpdated = Time.millis();

        targets = target1ds;
        if(lasts.length != values.length){
            lasts = new float[values.length];
        }
        for(int i = 0; i < values.length; i++){
            lasts[i] = values[i];
        }
        last.set(cx, cy);
        target.set(x, y);
    }

    public void reset(){
        values = new float[0];
        targets = new float[0];
        target.setZero();
        last.setZero();
        lastUpdated = 0;
        updateSpacing = 16; //1 frame
        pos.setZero();
    }

    public void update(){
        if(lastUpdated != 0 && updateSpacing != 0){
            float timeSinceUpdate = Time.timeSinceMillis(lastUpdated);
            float alpha = Math.min(timeSinceUpdate / updateSpacing, 2f);

            pos.set(last).lerpPast(target, alpha);

            if(values.length != targets.length){
                values = new float[targets.length];
            }

            if(lasts.length != targets.length){
                lasts = new float[targets.length];
            }

            for(int i = 0; i < values.length; i++){
                values[i] = Mathf.slerp(lasts[i], targets[i], alpha);
            }
        }else{
            pos.set(target);
        }

    }
}