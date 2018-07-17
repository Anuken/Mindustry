package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Texture;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.ucore.util.Bits;

import static io.anuke.mindustry.Vars.control;

public class Sector{
    /**Position on the map, can be positive or negative.*/
    public short x, y;
    /**Whether this sector has already been captured. TODO statistics?*/
    public boolean unlocked;
    /**Slot ID of this sector's save. -1 means no save has been created.*/
    public int saveID = -1;
    /**Display texture. Needs to be disposed.*/
    public transient Texture texture;

    public boolean hasSave(){
        return saveID != -1 && SaveIO.isSaveValid(saveID) && control.getSaves().getByID(saveID) != null;
    }

    public int packedPosition(){
        return Bits.packInt(x, y);
    }
}
