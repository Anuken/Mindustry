package io.anuke.mindustry.maps.filters;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.maps.filters.FilterOption.*;
import io.anuke.mindustry.world.*;

public class OreMedianFilter extends GenerateFilter{
    public float radius = 2;
    public float percentile = 0.5f;

    private IntArray blocks = new IntArray();

    {
        buffered = true;
        options(
            new SliderOption("radius", () -> radius, f -> radius = f, 1f, 12f),
            new SliderOption("percentile", () -> percentile, f -> percentile = f, 0f, 1f)
        );
    }

    @Override
    public void apply(){
        if(in.ore == Blocks.spawn) return;

        int cx = (in.x / 2) * 2;
        int cy = (in.y / 2) * 2;
        if(in.ore != Blocks.air){
            if(!(in.tile(cx + 1, cy).overlay() == in.ore && in.tile(cx, cy).overlay() == in.ore && in.tile(cx + 1, cy + 1).overlay() == in.ore && in.tile(cx, cy + 1).overlay() == in.ore &&
            !in.tile(cx + 1, cy).block().isStatic() && !in.tile(cx, cy).block().isStatic() && !in.tile(cx + 1, cy + 1).block().isStatic() && !in.tile(cx, cy + 1).block().isStatic())){
                in.ore = Blocks.air;
            }
        }

        int rad = (int)radius;

        blocks.clear();
        for(int x = -rad; x <= rad; x++){
            for(int y = -rad; y <= rad; y++){
                if(Mathf.dst2(x, y) > rad*rad) continue;

                Tile tile = in.tile(in.x + x, in.y + y);
                if(tile.overlay() != Blocks.spawn)
                blocks.add(tile.overlay().id);
            }
        }

        blocks.sort();

        int index = Math.min((int)(blocks.size * percentile), blocks.size - 1);
        int overlay = blocks.get(index);

        in.ore = Vars.content.block(overlay);
    }
}
