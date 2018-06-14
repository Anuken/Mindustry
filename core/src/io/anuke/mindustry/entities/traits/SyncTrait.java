package io.anuke.mindustry.entities.traits;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.ucore.entities.trait.Entity;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.threads;

public interface SyncTrait extends Entity, TypeTrait {

    /**Whether smoothing of entities is enabled when using multithreading; not yet implemented.*/
    static boolean isSmoothing(){
        return threads.isEnabled() && threads.getFPS() <= Gdx.graphics.getFramesPerSecond() / 2f;
    }

    /**Sets the position of this entity and updated the interpolator.*/
    default void setNet(float x, float y){
        set(x, y);

        if(getInterpolator() != null) {
            getInterpolator().target.set(x, y);
            getInterpolator().last.set(x, y);
            getInterpolator().spacing = 1f;
            getInterpolator().time = 0f;
        }
    }

    /**Interpolate entity position only. Override if you need to interpolate rotations or other values.*/
    default void interpolate(){
        if(getInterpolator() == null) throw new RuntimeException("This entity must have an interpolator to interpolate()!");

        getInterpolator().update();

        setX(getInterpolator().pos.x);
        setY(getInterpolator().pos.y);
    }

    /**Return the interpolator used for smoothing the position. Optional.*/
    default Interpolator getInterpolator(){
        return null;
    }

    /**Whether syncing is enabled for this entity; true by default.*/
    default boolean isSyncing(){
        return true;
    }

    //Read and write sync data, usually position
    void write(DataOutput data) throws IOException;
    void read(DataInput data, long time) throws IOException;
}
