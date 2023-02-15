package mindustry.logic;

import mindustry.ctype.*;

//TODO
public interface Settable{
    void setProperty(LAccess prop, double value);
    void setProperty(LAccess prop, Object value);
    void setProperty(UnlockableContent content, double value);
}
