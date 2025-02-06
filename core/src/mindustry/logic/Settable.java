package mindustry.logic;

import mindustry.ctype.*;

public interface Settable{
    void setProp(LAccess prop, double value);
    void setProp(LAccess prop, Object value);
    void setProp(UnlockableContent content, double value);
}
