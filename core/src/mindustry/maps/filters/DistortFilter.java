package mindustry.maps.filters;

import arc.util.*;
import mindustry.maps.filters.FilterOption.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

public class DistortFilter extends GenerateFilter{
    float scl = 40, mag = 5;

    @Override
    public FilterOption[] options(){
        return Structs.arr(
        new SliderOption("scale", () -> scl, f -> scl = f, 1f, 200f),
        new SliderOption("mag", () -> mag, f -> mag = f, 0.5f, 100f)
        );
    }

    @Override
    public boolean isBuffered(){
        return true;
    }

    @Override
    public void apply(){
        Tile tile = in.tile(in.x + noise(in.x, in.y, scl, mag) - mag / 2f, in.y + noise(in.x, in.y + o, scl, mag) - mag / 2f);

        in.floor = tile.floor();
        if(!tile.block().synthetic() && !in.block.synthetic()) in.block = tile.block();
        if(!((Floor)in.floor).isLiquid) in.ore = tile.overlay();
    }
}
