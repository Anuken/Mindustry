package mindustry.logic2;

import arc.graphics.*;
import mindustry.graphics.*;

public enum LCategory{
    control(Pal.accentBack),
    operations(Pal.place.cpy().shiftSaturation(-0.4f).mul(0.7f));

    public final Color color;

    LCategory(Color color){
        this.color = color;
    }
}
