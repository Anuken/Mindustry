package mindustry.logic;

import arc.graphics.*;
import mindustry.graphics.*;

/** The types of data a node field can be. */
public enum DataType{
    /** A double. Used for integer calculations as well. */
    number(Pal.place),
    /** Any type of content, e.g. item. */
    content(Color.cyan),
    /** A building of a tile. */
    building(Pal.items),
    /** A unit on the map. */
    unit(Pal.health),
    /** Java string */
    string(Color.royal);

    public final Color color;

    DataType(Color color){
        this.color = color;
    }
}