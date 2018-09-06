package io.anuke.mindustry.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.type.ContentType;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.content;

public class ColorMapper implements ContentList{
    private static IntMap<Block> blockMap = new IntMap<>();
    private static ObjectIntMap<Block> colorMap = new ObjectIntMap<>();
    private static ThreadLocal<Color> tmpColors = new ThreadLocal<>();

    public static Block getByColor(int color){
        return blockMap.get(color);
    }

    public static int getBlockColor(Block block){
        return colorMap.get(block, 0);
    }

    public static int colorFor(Block floor, Block wall, Team team, int elevation, byte cliffs){
        if(wall.synthetic()){
            return team.intColor;
        }
        int color = getBlockColor(wall);
        if(color == 0) color = ColorMapper.getBlockColor(floor);
        if(elevation > 0){
            if(tmpColors.get() == null) tmpColors.set(new Color());
            Color tmpColor = tmpColors.get();
            tmpColor.set(color);
            float maxMult = 1f/Math.max(Math.max(tmpColor.r, tmpColor.g), tmpColor.b) ;
            float mul = Math.min(1.1f + elevation / 4f, maxMult);
            if((cliffs & Mathf.pow2(6)) != 0){
                mul -= 0.5f;
            }
            tmpColor.mul(mul, mul, mul, 1f);
            color = Color.rgba8888(tmpColor);
        }
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

    @Override
    public ContentType type(){
        return ContentType.mech;
    }
}
