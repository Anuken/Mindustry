package mindustry.entities.units;

import mindustry.gen.*;

public interface UnitController{
    void unit(Unitc unit);
    Unitc unit();

    default void command(UnitCommand command){

    }

    default void updateUnit(){

    }

    default void removed(Unitc unit){

    }

    default boolean isBeingControlled(Unitc player){
        return false;
    }
}
