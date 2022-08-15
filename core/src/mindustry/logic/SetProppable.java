package mindustry.logic;

import mindustry.ctype.*;

public interface SetProppable{
    default void setProp(LProperty property, double value){
    }

    default void setProp(Content content, double value){
    }

    default void setPropObject(LProperty property, Object value){
    }
}
