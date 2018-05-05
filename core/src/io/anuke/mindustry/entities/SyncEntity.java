package io.anuke.mindustry.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Mathf;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.threads;

public abstract class SyncEntity extends DestructibleEntity{
    private static ObjectIntMap<Class<? extends SyncEntity>> writeSizes = new ObjectIntMap<>();

    protected transient Interpolator interpolator = new Interpolator();

    //smoothed position/angle
    private Vector3 spos = new Vector3();

    public float angle;

    static{
        setWriteSize(Enemy.class, 4 + 4 + 2 + 2);
        setWriteSize(Player.class, 4 + 4 + 4 + 2 + 1);
    }

    public static boolean isSmoothing(){
        return threads.isEnabled() && threads.getFPS() <= Gdx.graphics.getFramesPerSecond() / 2f;
    }

    public abstract void writeSpawn(ByteBuffer data);
    public abstract void readSpawn(ByteBuffer data);

    public abstract void write(ByteBuffer data);
    public abstract void read(ByteBuffer data, long time);

    public void interpolate(){
        interpolator.update();

        x = interpolator.pos.x;
        y = interpolator.pos.y;
        angle = interpolator.angle;
    }

    @Override
    public final void draw(){
        final float x = this.x, y = this.y, angle = this.angle;

        //interpolates data at low tick speeds.
        if(isSmoothing()){
            if(Vector2.dst(spos.x, spos.y, x, y) > 128){
                spos.set(x, y, angle);
            }

            this.x = spos.x = Mathf.lerpDelta(spos.x, x, 0.2f);
            this.y = spos.y = Mathf.lerpDelta(spos.y, y, 0.2f);
            this.angle = spos.z = Mathf.slerpDelta(spos.z, angle, 0.3f);
        }

        drawSmooth();

        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public Vector3 getDrawPosition(){
        return isSmoothing() ? spos : spos.set(x, y, angle);
    }

    public void drawSmooth(){}

    public int getWriteSize(){
        return getWriteSize(getClass());
    }

    public static int getWriteSize(Class<? extends SyncEntity> type){
        int i = writeSizes.get(type, -1);
        if(i == -1) throw new RuntimeException("Write size for class \"" + type + "\" is not defined!");
        return i;
    }

    protected static void setWriteSize(Class<? extends SyncEntity> type, int size){
        writeSizes.put(type, size);
    }

    public <T extends SyncEntity> T setNet(float x, float y){
        set(x, y);
        interpolator.target.set(x, y);
        interpolator.last.set(x, y);
        interpolator.spacing = 1f;
        interpolator.time = 0f;
        return (T)this;
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
        public float angle;

        public void read(float cx, float cy, float x, float y, float angle, long sent){
            targetrot = angle;
            time = 0f;
            last.set(cx, cy);
            target.set(x, y);
            spacing = Math.min(Math.max(((TimeUtils.timeSinceMillis(sent) / 1000f) * 60f), 4f), 10);
        }

        public void update(){

            time += 1f / spacing * Math.min(Timers.delta(), 1f);

            time = Mathf.clamp(time, 0, 2f);

            Mathf.lerp2(pos.set(last), target, time);

            angle = Mathf.slerpDelta(angle, targetrot, 0.6f);

            if(target.dst(pos) > 128){
                pos.set(target);
                last.set(target);
                time = 0f;
            }

        }
    }
}
