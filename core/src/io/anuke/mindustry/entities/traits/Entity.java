package io.anuke.mindustry.entities.traits;

import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.*;

public interface Entity extends MoveTrait{

    int getID();

    void resetID(int id);

    default void update(){}

    default void removed(){}

    default void added(){}

    default int tileX(){
        return Vars.world.toTile(getX());
    }

    default int tileY(){
        return Vars.world.toTile(getY());
    }

    EntityGroup targetGroup();

    @SuppressWarnings("unchecked")
    default void add(){
        if(targetGroup() != null){
            targetGroup().add(this);
        }
    }

    @SuppressWarnings("unchecked")
    default void remove(){
        if(getGroup() != null){
            getGroup().remove(this);
        }

        setGroup(null);
    }

    EntityGroup getGroup();

    void setGroup(EntityGroup group);

    default boolean isAdded(){
        return getGroup() != null;
    }
}
