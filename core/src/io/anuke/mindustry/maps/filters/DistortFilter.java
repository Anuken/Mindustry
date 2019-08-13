package io.anuke.mindustry.maps.filters;

import io.anuke.mindustry.maps.filters.FilterOption.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    {
        buffered = true;
        options(
            new SliderOption("scale", () -> scl, f -> scl = f, 1f, 200f),
            new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 100f)
        );
    }

    @Override
    public void apply(){
        Tile tile = in.tile(in.x + noise(in.x, in.y, scl, mag) - mag / 2f, in.y + noise(in.x, in.y + o, scl, mag) - mag / 2f);

        in.floor = tile.floor();
        if(!tile.block().synthetic() && !in.block.synthetic()) in.block = tile.block();
        if(!((Floor)in.floor).isLiquid) in.ore = tile.overlay();
    }
}
