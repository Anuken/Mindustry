package io.anuke.mindustry.world;

import io.anuke.arc.collection.IntMap;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Team;

import static io.anuke.mindustry.Vars.content;

public class ColorMapper implements ContentList{
    private static IntMap<Block> blockMap = new IntMap<>();
    private static ObjectIntMap<Block> colorMap = new ObjectIntMap<>();
    private static ThreadLocal<Color> tmpColors = new ThreadLocal<>();

    private static int getBlockColor(Block block){
        return colorMap.get(block, 0);
    }

    public static int colorFor(Block floor, Block wall, Team team){
        if(wall.synthetic()){
            return team.intColor;
        }
        int color = getBlockColor(wall);
        if(color == 0) color = ColorMapper.getBlockColor(floor);
        return color;
    }

    @Override
    public void load(){
        for(Block block : content.blocks()){
            int color = Color.rgba8888(block.minimapColor);
            if(color == 0) continue; //skip blocks that are not mapped

            blockMap.put(color, block);
            colorMap.put(block, color);
        }
    }
}
