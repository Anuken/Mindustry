package mindustry.entities.units;

import mindustry.gen.*;

//TODO rename
public interface UnitController{
    void unit(Unitc unit);
    Unitc unit();

    default void command(UnitCommand command){

    }

    default void update(){

    }

    default boolean isFollowing(Playerc player){
        return false;
    }
}
