package io.anuke.mindustry.entities.type;

import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.Entity;

public abstract class BaseEntity implements Entity{
    private static int lastid;
    /** Do not modify. Used for network operations and mapping. */
    public int id;
    public float x, y;
    protected transient EntityGroup group;

    public BaseEntity(){
        id = lastid++;
    }

    public int tileX(){
        return Vars.world.toTile(x);
    }

    public int tileY(){
        return Vars.world.toTile(y);
    }

    @Override
    public int getID(){
        return id;
    }

    @Override
    public void resetID(int id){
        this.id = id;
    }

    @Override
    public EntityGroup getGroup(){
        return group;
    }

    @Override
    public void setGroup(EntityGroup group){
        this.group = group;
    }

    @Override
    public float getX(){
        return x;
    }

    @Override
    public void setX(float x){
        this.x = x;
    }

    @Override
    public float getY(){
        return y;
    }

    @Override
    public void setY(float y){
        this.y = y;
    }

    @Override
    public String toString(){
        return getClass() + " " + id;
    }

    /** Increments this entity's ID. Used for pooled entities.*/
    public void incrementID(){
        id = lastid++;
    }
}
