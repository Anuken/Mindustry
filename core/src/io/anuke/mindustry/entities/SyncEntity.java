package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.ucore.entities.DestructibleEntity;

import java.nio.ByteBuffer;

public abstract class SyncEntity extends DestructibleEntity{
    private static ObjectIntMap<Class<? extends SyncEntity>> writeSizes = new ObjectIntMap<>();

    public transient Interpolator interpolator = new Interpolator();

    static{
        setWriteSize(Enemy.class, 4 + 4 + 2 + 2);
        setWriteSize(Player.class, 4 + 4 + 4 + 2 + 1);
    }

    public abstract void writeSpawn(ByteBuffer data);
    public abstract void readSpawn(ByteBuffer data);

    public abstract void write(ByteBuffer data);
    public abstract void read(ByteBuffer data);
    public abstract void interpolate();

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

    public class Interpolator {
        public Vector2 target = new Vector2();
        public Vector2 delta = new Vector2();
        public Vector2 last = new Vector2();
        public float targetrot;
    }
}
