package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.net.Interpolator;

import java.io.*;

public interface SyncTrait extends Entity, TypeTrait{

    /** Sets the position of this entity and updated the interpolator. */
    default void setNet(float x, float y){
        set(x, y);

        if(getInterpolator() != null){
            getInterpolator().target.set(x, y);
            getInterpolator().last.set(x, y);
            getInterpolator().pos.set(0, 0);
            getInterpolator().updateSpacing = 16;
            getInterpolator().lastUpdated = 0;
        }
    }

    /** Interpolate entity position only. Override if you need to interpolate rotations or other values. */
    default void interpolate(){
        if(getInterpolator() == null){
            throw new RuntimeException("This entity must have an interpolator to interpolate()!");
        }

        getInterpolator().update();

        setX(getInterpolator().pos.x);
        setY(getInterpolator().pos.y);
    }

    /** Return the interpolator used for smoothing the position. Optional. */
    default Interpolator getInterpolator(){
        return null;
    }

    /** Whether syncing is enabled for this entity; true by default. */
    default boolean isSyncing(){
        return true;
    }

    //Read and write sync data, usually position
    void write(DataOutput data) throws IOException;

    void read(DataInput data) throws IOException;
}
