package mindustry.world.meta;

import arc.graphics.*;
import mindustry.graphics.*;

public enum BlockStatus{
    active(Color.valueOf("5ce677")),
    noOutput(Color.orange),
    noInput(Pal.remove),
    logicDisable(Color.valueOf("8a73c6")),
    inactiveUnitFactory(Color.lightGray), //identical to inactive, but makes blocks draw a special timer over them when hovered
    inactive(Color.lightGray);

    public final Color color;

    BlockStatus(Color color){
        this.color = color;
    }
}
