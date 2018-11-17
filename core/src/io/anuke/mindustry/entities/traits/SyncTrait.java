package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.core.NetClient;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.util.Tmp;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface SyncTrait extends Entity, TypeTrait{

    /**Sets the position of this entity and updated the interpolator.*/
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

    /**Interpolate entity position only. Override if you need to interpolate rotations or other values.*/
    default void interpolate(){
        if(getInterpolator() == null){
            throw new RuntimeException("This entity must have an interpolator to interpolate()!");
        }

        if(isClipped()){
            //move off screen when no longer in bounds
            Tmp.r1.setSize(Core.camera.viewportWidth * Core.camera.zoom * NetClient.viewScale,
            Core.camera.viewportHeight * Core.camera.zoom * NetClient.viewScale)
            .setCenter(Core.camera.position.x, Core.camera.position.y);

            if(!Tmp.r1.contains(getX(), getY()) && !Tmp.r1.contains(getInterpolator().last.x, getInterpolator().last.y)){
                set(-99999f, -99999f);
                return;
            }
        }

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

    /**Whether this entity is clipped and not synced when out of viewport.*/
    default boolean isClipped(){
        return true;
    }

    //Read and write sync data, usually position
    void write(DataOutput data) throws IOException;

    void read(DataInput data, long time) throws IOException;
}
