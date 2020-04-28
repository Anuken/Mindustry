package mindustry.world;

import arc.graphics.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;

public class ColorMapper{
    private static final IntMap<Block> color2block = new IntMap<>();

    public static Block get(int color){
        return color2block.get(color, Blocks.air);
    }

    public static void load(){
        color2block.clear();

        for(Block block : Vars.content.blocks()){
            color2block.put(block.mapColor.rgba(), block);
        }

        color2block.put(Color.rgba8888(0, 0, 0, 1), Blocks.air);
    }
}
