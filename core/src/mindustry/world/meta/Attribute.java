package mindustry.world.meta;

import mindustry.*;

public enum Attribute{
    /** Heat of this block. Used for calculating output of thermal generators. */
    heat,
    /** Spore content of this block. Used for increasing cultivator yield. */
    spores,
    /** Water content of this block. Used for increasing water extractor yield. */
    water,
    /** Oil content of this block. Used for increasing oil extractor yield. */
    oil,
    /** Light coverage. Negative values decrease solar panel efficiency. */
    light;

    public static final Attribute[] all = values();

    /** @return the envrionmental value for this attribute. */
    public float env(){
        if(Vars.state == null) return 0;
        return Vars.state.envAttrs.get(this);
    }
}
