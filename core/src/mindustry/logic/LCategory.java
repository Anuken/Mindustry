package mindustry.logic;

import arc.graphics.*;
import mindustry.graphics.*;

public enum LCategory{
    blocks(Pal.accentBack),
    control(Color.cyan.cpy().shiftSaturation(-0.6f).mul(0.7f)),
    operations(Pal.place.cpy().shiftSaturation(-0.5f).mul(0.7f)),
    io(Pal.remove.cpy().shiftSaturation(-0.5f).mul(0.7f)),
    units(Pal.bulletYellowBack.cpy().shiftSaturation(-0.3f).mul(0.8f)),
    modded(Color.pink.cpy().shiftSaturation(-0.6f).mul(0.7f)),
    custom(Pal.accent.cpy().shiftSaturation(-0.2f).mul(0.3f));

    public final Color color;

    LCategory(Color color){
        this.color = color;
    }
}
