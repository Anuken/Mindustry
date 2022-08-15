package mindustry.logic;

import mindustry.ctype.*;

public interface SetProppable{
    void setProp(LAccess sensor, double value);

    default void setProp(Content content, double value){
    }

    default void setPropObject(LAccess sensor, Object value){
    }
}
