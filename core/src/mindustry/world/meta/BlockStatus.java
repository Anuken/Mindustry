package mindustry.world.meta;

import arc.graphics.*;
import mindustry.graphics.*;

public enum BlockStatus{
    active(Color.valueOf("5ce677")),
    noOutput(Color.orange),
    noInput(Pal.remove);

    public final Color color;

    BlockStatus(Color color){
        this.color = color;
    }
}
