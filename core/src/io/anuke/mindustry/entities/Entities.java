package io.anuke.mindustry.entities;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntMap;
import io.anuke.mindustry.entities.traits.Entity;

public class Entities{
    public static final int maxLeafObjects = 5;
    private static final EntityGroup<Entity> defaultGroup;
    private static final Array<EntityGroup<?>> groupArray = new Array<>();
    private static final IntMap<EntityGroup<?>> groups = new IntMap<>();

    static{
        defaultGroup = addGroup(Entity.class);
    }

    public static void clear(){
        for(EntityGroup group : groupArray){
            group.clear();
        }
    }

    public static Iterable<Entity> all(){
        return defaultGroup.all();
    }

    public static EntityGroup<?> getGroup(int id){
        return groups.get(id);
    }

    public static Iterable<EntityGroup<?>> getAllGroups(){
        return groups.values();
    }

    public static EntityGroup<Entity> defaultGroup(){
        return defaultGroup;
    }

    public static <T extends Entity> EntityGroup<T> addGroup(Class<T> type){
        return addGroup(type, true);
    }

    public static <T extends Entity> EntityGroup<T> addGroup(Class<T> type, boolean useTree){
        EntityGroup<T> group = new EntityGroup<>(type, useTree);
        groups.put(group.getID(), group);
        groupArray.add(group);
        return group;
    }

    public static void update(){
        update(defaultGroup());
        EntityQuery.collideGroups(defaultGroup(), defaultGroup());
    }

    public static void update(EntityGroup<?> group){
        group.updateEvents();

        if(group.useTree()){
            EntityQuery.collisions().updatePhysics(group);
        }

        for(Entity e : group.all()){
            e.update();
        }
    }
}
