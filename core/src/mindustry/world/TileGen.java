package mindustry.world;

import mindustry.content.*;

public class TileGen{
    public Block floor;
    public Block block ;
    public Block overlay;

    {
        reset();
    }

    public void reset(){
        floor = Blocks.stone;
        block = Blocks.air;
        overlay = Blocks.air;
    }
}
