package mindustry.logic;

import mindustry.ctype.*;

public interface SetStatable {
    void setStat(LAccess sensor, double value);

    default void setStat(Content content, double value) {
    }

    default void setObject(LAccess sensor, Object value) {
    }
}
