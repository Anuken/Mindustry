package mindustry.type;

import arc.graphics.*;
import mindustry.gen.*;

/** Type of ammo that a unit uses. */
public interface AmmoType{
    String icon();
    Color color();
    Color barColor();
    void resupply(Unit unit);
}
