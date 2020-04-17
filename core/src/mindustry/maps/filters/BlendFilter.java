package mindustry.maps.filters;

import arc.math.*;
import mindustry.content.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;

import static mindustry.maps.filters.FilterOption.*;

public class BlendFilter extends GenerateFilter{
    float radius = 2f;
    Block block = Blocks.stone, floor = Blocks.ice, ignore = Blocks.air;

    {
        buffered = true;
        options(
            new SliderOption("radius", () -> radius, f -> radius = f, 1f, 10f),
            new BlockOption("block", () -> block, b -> block = b, anyOptional),
            new BlockOption("floor", () -> floor, b -> floor = b, floorsOnly),
            new BlockOption("ignore", () -> ignore, b -> ignore = b, floorsOptional)
        );
    }

    @Override
    public void apply(){
        if(in.floor == block || block == Blocks.air || in.floor == ignore) return;

        int rad = (int)radius;
        boolean found = false;

        outer:
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;
                Tile tile = in.tile(in.x + x, in.y + y);

                if(tile.floor() == block || tile.block() == block || tile.overlay() == block){
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
