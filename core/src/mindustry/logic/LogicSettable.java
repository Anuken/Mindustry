package mindustry.logic;

import mindustry.type.*;

public interface LogicSettable{
    void setProp(LogicProp prop, double value);
    void setProp(LogicProp prop, Object value);
    void setProp(UnlockableContent content, double value);
}
