package mindustry.logic;

import mindustry.game.*;

/** An object that can be controlled with logic. */
public interface Controllable{
    void control(LAccess type, double p1, double p2, double p3, double p4);
    void control(LAccess type, Object p1, double p2, double p3, double p4);
    Team team();
}
