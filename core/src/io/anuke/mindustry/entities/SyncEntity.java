package io.anuke.mindustry.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Mathf;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.threads;

/**Base class for any entity that needs to be synced across clients.*/
public abstract class SyncEntity extends DestructibleEntity{
    protected transient Interpolator interpolator = new Interpolator();
    /**smoothed position and rotation*/
    private Vector3 spos = new Vector3();
    /**the general rotation.*/
    public float rotation;

    public abstract void writeSpawn(ByteBuffer data);
    public abstract void readSpawn(ByteBuffer data);

    public abstract void write(ByteBuffer data);
    public abstract void read(ByteBuffer data, long time);

    /**Interpolate everything needed. Should be called in update() for non-local entities.*/
    public void interpolate(){
        interpolator.update();

        x = interpolator.pos.x;
        y = interpolator.pos.y;
        rotation = interpolator.rotation;
    }

    /**Same as draw, but for interpolated drawing at low tick speeds.*/
    public abstract void drawSmooth();

    /**Do not override, use drawSmooth instead.*/
    @Override
    public final void draw(){
        final float x = this.x, y = this.y, rotation = this.rotation;

        //interpolates data at low tick speeds.
        if(isSmoothing()){
            if(Vector2.dst(spos.x, spos.y, x, y) > 128){
                spos.set(x, y, rotation);
            }

            this.x = spos.x = Mathf.lerpDelta(spos.x, x, 0.2f);
            this.y = spos.y = Mathf.lerpDelta(spos.y, y, 0.2f);
            this.rotation = spos.z = Mathf.slerpDelta(spos.z, rotation, 0.3f);
        }

        drawSmooth();

        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    /**Returns smoothed position. x = x, y = y, z = rotation.*/
    public Vector3 getDrawPosition(){
        return isSmoothing() ? spos : spos.set(x, y, rotation);
    }

    /**Set position and interpolator position.*/
    public <T extends SyncEntity> T setNet(float x, float y){
        set(x, y);
        interpolator.target.set(x, y);
        interpolator.last.set(x, y);
        interpolator.spacing = 1f;
        interpolator.time = 0f;
        return (T)this;
    }

    public static boolean isSmoothing(){
        return threads.isEnabled() && threads.getFPS() <= Gdx.graphics.getFramesPerSecond() / 2f;
    }

    public static class Interpolator {
        //used for movement
        public Vector2 target = new Vector2();
        public Vector2 last = new Vector2();
        public float targetrot;
        public float spacing = 1f;
        public float time;

        //current state
        public Vector2 pos = new Vector2();
        public float rotation;

        public void read(float cx, float cy, float x, float y, float angle, long sent){
            targetrot = angle;
            time = 0f;
            last.set(cx, cy);
            target.set(x, y);
            spacing = Math.min(Math.max(((TimeUtils.timeSinceMillis(sent) / 1000f) * 60f), 4f), 10);
        }

        public void update(){

            time += 1f / spacing * Math.min(Timers.delta(), 1f);

            Mathf.lerp2(pos.set(last), target, time);

            rotation = Mathf.slerpDelta(rotation, targetrot, 0.6f);

            if(target.dst(pos) > 128){
                pos.set(target);
                last.set(target);
                time = 0f;
            }

        }
    }
}
