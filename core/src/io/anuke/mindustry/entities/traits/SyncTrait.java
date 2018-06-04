package io.anuke.mindustry.entities.traits;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.ucore.entities.trait.Entity;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.threads;

public interface SyncTrait extends Entity {

    static boolean isSmoothing(){
        return threads.isEnabled() && threads.getFPS() <= Gdx.graphics.getFramesPerSecond() / 2f;
    }

    default void setNet(float x, float y){
        set(x, y);
        getInterpolator().target.set(x, y);
        getInterpolator().last.set(x, y);
        getInterpolator().spacing = 1f;
        getInterpolator().time = 0f;
    }

    default void interpolate(){
        getInterpolator().update();

        setX(getInterpolator().pos.x);
        setY(getInterpolator().pos.y);
    }

    Interpolator getInterpolator();

    //Read and write sync data, usually position
    void write(ByteBuffer data);
    void read(ByteBuffer data, long time);
}
