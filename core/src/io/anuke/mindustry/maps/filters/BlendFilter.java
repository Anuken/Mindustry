package io.anuke.mindustry.maps.filters;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.maps.filters.FilterOption.BlockOption;
import io.anuke.mindustry.maps.filters.FilterOption.SliderOption;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.maps.filters.FilterOption.floorsOnly;

public class BlendFilter extends GenerateFilter{
    float radius = 2f;
    Block flooronto = Blocks.stone, floor = Blocks.ice;

    {
        options(
        new SliderOption("radius", () -> radius, f -> radius = f, 1f, 10f),
        new BlockOption("flooronto", () -> flooronto, b -> flooronto = b, floorsOnly),
        new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly)
        );
    }

    @Override
    public void apply(){
        if(in.floor == flooronto) return;

        int rad = (int)radius;
        boolean found = false;

        outer:
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;

                if(in.tile(in.x + x, in.y + y).floor == flooronto.id){
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
