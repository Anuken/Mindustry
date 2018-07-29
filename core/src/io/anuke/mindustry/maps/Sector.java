package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.maps.missions.Mission;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.util.Bits;

import static io.anuke.mindustry.Vars.control;

public class Sector{
    /**Position on the map, can be positive or negative.*/
    public short x, y;
    /**Whether this sector has already been completed.*/
    public boolean complete;
    /**Slot ID of this sector's save. -1 means no save has been created.*/
    public int saveID = -1;
    /**Sector size; if more than 1, the coordinates are the bottom left corner.*/
    public int size = 1;
    /**Num of missions in this sector that have been completed so far.*/
    public int completedMissions;
    /**Display texture. Needs to be disposed.*/
    public transient Texture texture;
    /**Missions of this sector-- what needs to be accomplished to unlock it.*/
    public transient Array<Mission> missions = new Array<>();
    /**Enemies spawned at this sector.*/
    public transient Array<SpawnGroup> spawns = new Array<>();
    /**Ores that appear in this sector.*/
    public transient Array<Item> ores = new Array<>();
    /**Difficulty of the sector, measured by calculating distance from origin.*/
    public transient int difficulty;

    public int getSeed(){
        return Bits.packInt(x, y);
    }

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
