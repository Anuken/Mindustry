package io.anuke.mindustry.maps.filters;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.editor.MapGenerateDialog.*;
import io.anuke.mindustry.maps.filters.FilterOption.BlockOption;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.maps.filters.FilterOption.*;

public class BlendFilter extends GenerateFilter{
    float radius = 2f;
    Block block = Blocks.stone, floor = Blocks.ice;

    {
        buffered = true;
        options(
        new SliderOption("radius", () -> radius, f -> radius = f, 1f, 10f),
        new BlockOption("block", () -> block, b -> block = b, anyOptional),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly)
        );
    }

    @Override
    public void apply(){
        if(in.floor == block || block == Blocks.air) return;

        int rad = (int)radius;
        boolean found = false;

        outer:
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;
                GenTile tile = in.tile(in.x + x, in.y + y);

                if(tile.floor == block.id || tile.block == block.id || tile.ore == block.id){
                    found = true;
                    break outer;
                }
            }
        }

        if(found){
            in.floor = floor;
        }
    }
}
