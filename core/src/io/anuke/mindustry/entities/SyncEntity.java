package io.anuke.mindustry.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Mathf;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.threads;

public abstract class SyncEntity extends DestructibleEntity{
    private static ObjectIntMap<Class<? extends SyncEntity>> writeSizes = new ObjectIntMap<>();

    protected transient Interpolator interpolator = new Interpolator();

    //for interpolating at low tick speeds.
    private transient Vector2 tpos = new Vector2(-999, -999);
    private transient float tang = 0f;

    public float angle;

    static{
        setWriteSize(Enemy.class, 4 + 4 + 2 + 2);
        setWriteSize(Player.class, 4 + 4 + 4 + 2 + 1);
    }

    public abstract void writeSpawn(ByteBuffer data);
    public abstract void readSpawn(ByteBuffer data);

    public abstract void write(ByteBuffer data);
    public abstract void read(ByteBuffer data, long time);
    public abstract void interpolate();

    @Override
    public final void draw(){
        float x = this.x, y = this.y, angle = this.angle;

        //interpolates data at low tick speeds.
        if(isSmoothing()){
            if(tpos.dst(x, y) > 100){
                tpos.set(x, y);
            }
            tpos.x = Mathf.lerpDelta(tpos.x, x, 0.3f);
            tpos.y = Mathf.lerpDelta(tpos.y, y, 0.3f);
            tang = Mathf.lerpAngDelta(tang, angle, 0.3f);
            this.x = tpos.x;
            this.y = tpos.y;
            this.angle = tang;
        }

        drawSmooth();

        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    private boolean isSmoothing(){
        return threads.isEnabled() && threads.getFPS() <= Gdx.graphics.getFramesPerSecond() / 2f;
    }

    public Vector2 getDrawPosition(){
        return isSmoothing() ? tpos : tpos.set(x, y);
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

    public class Interpolator {
        public Vector2 target = new Vector2();
        public Vector2 last = new Vector2();
        public Vector2 vec = new Vector2();
        public float targetrot;
        public float spacing = 1f;
        public float time;
    }
}
