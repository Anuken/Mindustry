package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Texture;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.maps.goals.Goal;
import io.anuke.mindustry.maps.goals.WaveGoal;
import io.anuke.ucore.util.Bits;

import static io.anuke.mindustry.Vars.control;

public class Sector{
    /**Position on the map, can be positive or negative.*/
    public short x, y;
    /**Whether this sector has already been completed.*/
    public boolean complete;
    /**Slot ID of this sector's save. -1 means no save has been created.*/
    public int saveID = -1;
    /**Display texture. Needs to be disposed.*/
    public transient Texture texture;
    /**Goal of this sector-- what needs to be accomplished to unlock it.*/
    public transient Goal goal = new WaveGoal(30);

    public SaveSlot getSave(){
        return control.getSaves().getByID(saveID);
    }

    public boolean hasSave(){
        return control.getSaves().getByID(saveID) != null;
    }

    public int packedPosition(){
        return Bits.packInt(x, y);
    }
}
