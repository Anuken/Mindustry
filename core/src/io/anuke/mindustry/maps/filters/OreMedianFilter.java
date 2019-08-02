package io.anuke.mindustry.maps.filters;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.maps.filters.FilterOption.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.content;

public class OreMedianFilter extends GenerateFilter{
    float radius = 2;
    float percentile = 0.5f;
    IntArray blocks = new IntArray();

    {
        buffered = true;
        options(
            new SliderOption("radius", () -> radius, f -> radius = f, 1f, 12f),
            new SliderOption("percentile", () -> percentile, f -> percentile = f, 0f, 1f)
        );
    }

    @Override
    public void apply(){
        int rad = (int)radius;
        blocks.clear();
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;

                Tile tile = in.tile(in.x + x, in.y + y);
                blocks.add(tile.overlay().id);
            }
        }

        blocks.sort();

        int index = Math.min((int)(blocks.size * percentile), blocks.size - 1);
        int overlay = blocks.get(index), block = blocks.get(index);

        in.ore = content.block(overlay);
    }
}
