package io.anuke.mindustry.entities;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.entities.traits.*;

/** Simple container for managing entity groups.*/
public class Entities{
    private final Array<EntityGroup<?>> groupArray = new Array<>();

    public void clear(){
        for(EntityGroup group : groupArray){
            group.clear();
        }
    }

    public EntityGroup<?> get(int id){
        return groupArray.get(id);
    }

    public Array<EntityGroup<?>> all(){
        return groupArray;
    }

    public <T extends Entity> EntityGroup<T> add(Class<T> type){
        return add(type, true);
    }

    public <T extends Entity> EntityGroup<T> add(Class<T> type, boolean useTree){
        EntityGroup<T> group = new EntityGroup<>(groupArray.size, type, useTree);
        groupArray.add(group);
        return group;
    }
}
