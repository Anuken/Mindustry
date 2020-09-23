package mindustry.entities.units;

import mindustry.gen.*;

public interface UnitController{
    void unit(Unit unit);
    Unit unit();

    default boolean isValidController(){
        return true;
    }

    default void command(UnitCommand command){

    }

    default void updateUnit(){

    }

    default void removed(Unit unit){

    }

    default boolean isBeingControlled(Unit player){
        return false;
    }
}
