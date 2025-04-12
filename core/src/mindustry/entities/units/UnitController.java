package mindustry.entities.units;

import arc.util.*;
import mindustry.gen.*;

public interface UnitController{
    void unit(Unit unit);
    @Nullable Unit unit();

    default void hit(Bullet bullet){

    }

    default boolean isValidController(){
        return true;
    }

    /** @return whether logic AI can take over */
    default boolean isLogicControllable(){
        return false;
    }

    default void updateUnit(){

    }

    default void removed(Unit unit){

    }

    default void afterRead(Unit unit){

    }
}
