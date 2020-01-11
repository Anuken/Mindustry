package mindustry.entities.traits;

import mindustry.entities.EntityGroup;

public interface Entity extends MoveTrait{

    int getID();

    void resetID(int id);

    default void update(){}

    default void removed(){}

    default void added(){}

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
