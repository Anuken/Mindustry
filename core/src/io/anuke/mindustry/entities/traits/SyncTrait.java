package io.anuke.mindustry.entities.traits;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.function.Supplier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.threads;

public interface SyncTrait extends Entity {
    int[] lastRegisteredID = {0};
    Array<Supplier<? extends SyncTrait>> registeredTypes = new Array<>();

    /**Register and return a type ID. The supplier should return a fresh instace of that type.*/
    static int registerType(Supplier<? extends SyncTrait> supplier){
        registeredTypes.add(supplier);
        int result = lastRegisteredID[0];
        lastRegisteredID[0] ++;
        return result;
    }

    /**Registers a syncable type by ID.*/
    static Supplier<? extends SyncTrait> getTypeByID(int id){
        if(id == -1){
            throw new IllegalArgumentException("Attempt to retrieve invalid entity type ID! Did you forget to set it in ContentLoader.registerTypes()?");
        }
        return registeredTypes.get(id);
    }

    /**Whether smoothing of entities is enabled; not yet implemented.*/
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

    /**Returns the type ID of this entity used for intstantiation. Should be < BYTE_MAX.*/
    int getTypeID();

    //Read and write sync data, usually position
    void write(DataOutput data) throws IOException;
    void read(DataInput data, long time) throws IOException;
}
